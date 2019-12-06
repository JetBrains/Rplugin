/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk


import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.PsiElementProcessor.FindFilteredElement
import com.intellij.psi.util.PsiTreeUtil
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.rendering.chunk.ChunkPathManager
import org.intellij.datavis.inlays.InlaysManager
import org.intellij.datavis.inlays.components.ProcessOutput
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownParagraphImpl
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.rinterop.RIExecutionResult
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rmarkdown.RMarkdownUtil
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import org.jetbrains.r.run.graphics.RGraphicsDevice
import org.jetbrains.r.run.graphics.RGraphicsUtils
import java.awt.Dimension
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private val LOGGER = Logger.getInstance(RunChunkHandler::class.java)

private val UNKNOWN_ERROR_MESSAGE = RBundle.message("notification.unknown.error.message")
private val CHUNK_EXECUTOR_NAME = RBundle.message("run.chunk.executor.name")

private const val RUN_CHUNK_GROUP_ID = "org.jetbrains.r.rendering.chunk.RunChunkActions"

object RunChunkHandler {

  fun runAllChunks(psiFile: PsiFile,
                   editor: Editor,
                   currentElement: AtomicReference<PsiElement>,
                   terminationRequired: AtomicBoolean,
                   start: Int = 0,
                   end: Int = Int.MAX_VALUE): Promise<Unit> {
    val result = AsyncPromise<Unit>()
    RConsoleManager.getInstance(psiFile.project).currentConsoleAsync.onSuccess { console ->
      runAsync {
        console.debugger.isVariableRefreshEnabled = false
        try {
          val chunks = runReadAction { PsiTreeUtil.collectElements(psiFile) {
            isChunkFenceLang(it) && (it.textRange.endOffset - 1) in start..end
          } }
          try {
            for (element in chunks) {
              if (terminationRequired.get()) {
                continue
              }
              currentElement.set(element)
              invokeAndWaitIfNeeded { execute(element, false, true) }
                .onError { LOGGER.error("Cannot execute chunk: " + it) }
                .blockingGet(Int.MAX_VALUE)
              currentElement.set(null)
            }
          } finally {
            runReadAction { chunks.mapNotNull { findInlayElementByFenceElement(it) } }.forEach { inlayElement ->
              InlaysManager.getEditorManager(editor)?.updateCell(inlayElement)
            }
          }
        } finally {
          result.setResult(Unit)
          console.resetHandler()
          console.debugger.isVariableRefreshEnabled = true
        }
      }
    }
    return result
  }

  fun execute(element: PsiElement, isDebug: Boolean = false, isBatchMode: Boolean = false) : Promise<Unit> {
    return AsyncPromise<Unit>().also { promise ->
      RMarkdownUtil.checkOrInstallPackages(element.project, CHUNK_EXECUTOR_NAME)
        .onSuccess {
          runInEdt {
            executeWithKnitr(element, isDebug, isBatchMode).processed(promise)
          }
        }
        .onError {
          promise.setError(it.message ?: UNKNOWN_ERROR_MESSAGE)
        }
    }
  }

  private fun executeWithKnitr(element: PsiElement, isDebug: Boolean, isBatchMode: Boolean): Promise<Unit> {
    return AsyncPromise<Unit>().also { promise ->
      if (element.containingFile == null || element.context == null) return promise.apply { setError("parent not found") }
      val fileEditor = FileEditorManager.getInstance(element.project).getSelectedEditor(element.containingFile.originalFile.virtualFile)
      val editor = EditorUtil.getEditorEx(fileEditor) ?: return promise.apply { setError("editor not found") }
      RConsoleManager.getInstance(element.project).currentConsoleAsync.onSuccess {
        runAsync {
          runReadAction { runHandlersAndExecuteChunk(it, element, editor, isDebug, isBatchMode) }.processed(promise)
        }
      }
    }
  }

  @VisibleForTesting
  internal fun runHandlersAndExecuteChunk(console: RConsoleView, element: PsiElement, editor: EditorEx,
                                          isDebug: Boolean = false, isBatchMode: Boolean = false): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    val rInterop = console.rInterop
    val parent = element.parent
    val chunkText = parent?.text ?: return promise.apply { setError("parent is null") }
    val codeElement = FindFilteredElement<LeafPsiElement> {
      (it as? LeafPsiElement)?.elementType == R_FENCE_ELEMENT_TYPE
    }.apply { PsiTreeUtil.processElements(parent, this) }.foundElement
      ?: return promise.apply { setError("cannot find code fence") }
    val project = element.project
    val file = element.containingFile
    val inlayElement = findInlayElementByFenceElement(element) ?: return promise.apply { setError("cannot find code fence") }
    val paragraph = FindFilteredElement<MarkdownParagraphImpl> { it is MarkdownParagraphImpl }.also {
      PsiTreeUtil.processElements(file, it)
    }.foundElement
    val rmarkdownParameters = "---${System.lineSeparator()}${paragraph?.text ?: ""}${System.lineSeparator()}---"
    val cacheDirectory = ChunkPathManager.getCacheDirectory(inlayElement) ?: return promise.apply { setError("cannot create cache dir") }
    FileUtil.delete(File(cacheDirectory))
    val screenParameters = if (ApplicationManager.getApplication().isHeadlessEnvironment) {
      RGraphicsUtils.ScreenParameters(Dimension(800, 600), null)
    } else {
      RGraphicsUtils.getDefaultScreenParameters()
    }
    try {
      if (console.isRunningCommand) {
        throw RDebuggerException(RBundle.message("console.previous.command.still.running"))
      }
      if (console.debugger.isEnabled) {
        throw RDebuggerException(RBundle.message("debugger.still.running"))
      }
      var graphicsDevice: RGraphicsDevice? = null
      if (!isDebug) {
        if (!isBatchMode) {
          console.debugger.isVariableRefreshEnabled = false
        }
        logNonEmptyError(rInterop.runBeforeChunk(rmarkdownParameters, chunkText, cacheDirectory, screenParameters))
        ChunkPathManager.getImagesDirectory(inlayElement)?.let { imagesDirectory ->
          graphicsDevice = RGraphicsDevice(rInterop, File(imagesDirectory), screenParameters, false)
        }
      }
      executeCode(console, codeElement, element, isDebug).onProcessed { outputs ->
        runAsync {
          graphicsDevice?.shutdown()
          afterRunChunk(element, rInterop, outputs, promise, console, editor, inlayElement, isBatchMode, isDebug)
        }
      }
    } catch (e: RDebuggerException) {
      if (!isBatchMode && !isDebug) {
        console.debugger.isVariableRefreshEnabled = true
      }
      val notification = Notification(
        "RMarkdownRunChunkStatus", RBundle.message("run.chunk.notification.title"),
        e.message?.takeIf { it.isNotEmpty() } ?: RBundle.message("run.chunk.notification.failed"),
        NotificationType.WARNING, null)
      notification.notify(project)
      ApplicationManager.getApplication().invokeLater { editor.gutterComponentEx.revalidateMarkup() }
      promise.setError(e.message.orEmpty())
    }
    return promise
  }

  private fun afterRunChunk(element: PsiElement,
                            rInterop: RInterop,
                            outputs: List<ProcessOutput>?,
                            promise: AsyncPromise<Unit>,
                            console: RConsoleView,
                            editor: EditorEx,
                            inlayElement: PsiElement,
                            isBatchMode: Boolean,
                            isDebug: Boolean) {
    if (!isDebug) {
      logNonEmptyError(rInterop.runAfterChunk())
    }
    if (!isBatchMode && !isDebug) { console.debugger.isVariableRefreshEnabled = true }
    if (outputs != null) {
      if (!isDebug) {
        saveOutputs(outputs, element)
      }
    }
    else {
      val notification = Notification(
        "RMarkdownRunChunkStatus", RBundle.message("run.chunk.notification.title"),
        RBundle.message("run.chunk.notification.stopped"), NotificationType.INFORMATION, null)
      notification.notify(element.project)
    }
    promise.setResult(Unit)
    if (!isBatchMode) {
      console.resetHandler()
    }
    if (ApplicationManager.getApplication().isUnitTestMode || isDebug) return
    cleanupOutdatedOutputs(element)
    ApplicationManager.getApplication().invokeLater { editor.gutterComponentEx.revalidateMarkup() }
  }

  private fun executeCode(console: RConsoleView, codeElement: PsiElement, fenceElement: PsiElement, debug: Boolean = false):
    Promise<List<ProcessOutput>> {
    return if (debug) {
      console.debugger.executeChunk(codeElement.containingFile.virtualFile, codeElement.textRange)
    } else {
      val result = mutableListOf<ProcessOutput>()
      val executePromise = console.executeCodeAsyncWithBusy(codeElement.text) { s, type ->
        result.add(ProcessOutput(s, type))
      }
      val promise = AsyncPromise<List<ProcessOutput>>()
      executePromise.onProcessed { promise.setResult(result) }
      codeElement.project.chunkExecutionState?.cancellableExecutionPromise?.set(executePromise)
      promise
    }
  }

  private fun logNonEmptyError(result: RIExecutionResult) {
    if (result.stderr.isNotBlank()) {
      LOGGER.error("Run Chunk: the command returns non-empty output; stdout='${result.stdout}', stderr='${result.stderr}'")
    }
  }

  private fun cleanupOutdatedOutputs(element: PsiElement) {
    if (ApplicationManager.getApplication().isUnitTestMode) return
    val (path, presentChunks) = runReadAction {
      val file = element.containingFile
      val path = file?.virtualFile?.canonicalPath ?: return@runReadAction null
      val presentChunks = PsiTreeUtil.collectElements(file) { it is LeafPsiElement && it.elementType === R_FENCE_ELEMENT_TYPE }
                                     .map { Integer.toHexString(it.parent.text.hashCode()) }
      return@runReadAction Pair(path, presentChunks)
    } ?: return
    File(ChunkPathManager.getDirectoryForPath(path)).takeIf { it.exists() && it.isDirectory }?.listFiles()?.filter {
      !it.isDirectory || !presentChunks.contains(it.name)
    }?.forEach { FileUtil.delete(it) }
  }

  private fun saveOutputs(outputs: List<ProcessOutput>, element: PsiElement) {
    if (outputs.any { it.text.isNotEmpty() }) {
      runReadAction { ChunkPathManager.getOutputFile(element) }?.let {
        val text = Gson().toJson(outputs.filter { output -> output.text.isNotEmpty() })
        File(it).writeText(text)
      }
    }
  }

  fun interruptChunkExecution(project: Project) {
    val chunkExecutionState = project.chunkExecutionState ?: return
    if (chunkExecutionState.isDebug) {
      RConsoleManager.getInstance(project).currentConsoleOrNull?.debugger?.stop()
    }
    else {
      chunkExecutionState.cancellableExecutionPromise.get()?.cancel()
    }
  }

  private fun escape(text: String) = text.replace("""\""", """\\""").replace("""'""", """\'""")
}
