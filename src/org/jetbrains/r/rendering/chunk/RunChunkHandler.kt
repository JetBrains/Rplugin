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
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.plugins.notebooks.visualization.r.inlays.InlayDimensions
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.GraphicsPanel
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.ProcessOutput
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.progress.InlayProgressStatus
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.progress.ProgressStatus
import org.jetbrains.plugins.notebooks.visualization.r.inlays.getEditorManager
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleExecuteActionHandler
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.editor.ui.rMarkdownNotebook
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.rinterop.RIExecutionResult
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rmarkdown.RMarkdownUtil
import org.jetbrains.r.rmarkdown.R_FENCE_ELEMENT_TYPE
import org.jetbrains.r.run.graphics.RGraphicsDevice
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.settings.RMarkdownGraphicsSettings
import java.awt.Dimension
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private val LOGGER = Logger.getInstance(RunChunkHandler::class.java)

private val UNKNOWN_ERROR_MESSAGE = RBundle.message("notification.unknown.error.message")
private val CHUNK_EXECUTOR_NAME = RBundle.message("run.chunk.executor.name")

object RunChunkHandler {

  fun runAllChunks(psiFile: PsiFile,
                   editor: Editor,
                   start: Int = 0,
                   end: Int = Int.MAX_VALUE,
                   runSelectedCode: Boolean = false,
                   isDebug: Boolean = false) {
    val state = ChunkExecutionState(editor)
    editor.chunkExecutionState = state
    runAllChunks(psiFile, editor, state.currentPsiElement, state.terminationRequired, start, end, runSelectedCode, isDebug).onProcessed {
      editor.chunkExecutionState = null
    }
  }

  private fun runAllChunks(psiFile: PsiFile,
                           editor: Editor,
                           currentElement: AtomicReference<PsiElement>,
                           terminationRequired: AtomicBoolean,
                           start: Int,
                           end: Int,
                           runSelectedCode: Boolean,
                           isDebug: Boolean): Promise<Unit> {
    val result = AsyncPromise<Unit>()
    RConsoleManager.getInstance(psiFile.project).currentConsoleAsync.onSuccess { console ->
      runAsync {
        try {
          val document = editor.document
          val ranges = ArrayList<IntRange>()
          val chunks = runReadAction {
            PsiTreeUtil.collectElements(psiFile) {
              isChunkFenceLang(it) && if (runSelectedCode) {
                it.parent?.textRange?.intersects(TextRange(start, end)) == true
              } else {
                (it.textRange.endOffset - 1) in start..end
              }
            }
              .also { chunks ->
                chunks.map { it.parent }.forEach { chunk ->
                  ranges.add(
                    IntRange(document.getLineNumber(chunk.textRange.startOffset), document.getLineNumber(chunk.textRange.endOffset)))
                }
              }
          }
          for ((index, element) in chunks.withIndex()) {
            if (terminationRequired.get()) break

            currentElement.set(element)
            val proceed = invokeAndWaitIfNeeded { execute(element, isDebug = isDebug, isBatchMode = true,
                                                          isFirstChunk = index == 0,
                                                          textRange = if (runSelectedCode) TextRange(start, end) else null) }
              .onError { LOGGER.error("Cannot execute chunk: $it") }
              .blockingGet(Int.MAX_VALUE) ?: false
            currentElement.set(null)
            if (!proceed) break
          }
        } finally {
          result.setResult(Unit)
          console.resetHandler()
        }
      }
    }
    return result
  }

  fun runSelectedRange(file: PsiFile, editor: Editor, range: TextRange, isDebug: Boolean = false) {
    runAllChunks(file, editor, range.startOffset, range.endOffset, runSelectedCode = true, isDebug = isDebug)
  }

  fun execute(element: PsiElement, isDebug: Boolean = false, isBatchMode: Boolean = false, isFirstChunk: Boolean = true,
              textRange: TextRange? = null) : Promise<Boolean> {
    return AsyncPromise<Boolean>().also { promise ->
      RMarkdownUtil.checkOrInstallPackages(element.project, CHUNK_EXECUTOR_NAME)
        .onSuccess {
          runInEdt {
            executeAsync(element, isDebug, isBatchMode, isFirstChunk, textRange).processed(promise)
          }
        }
        .onError {
          promise.setError(it.message ?: UNKNOWN_ERROR_MESSAGE)
        }
    }
  }

  private fun executeAsync(element: PsiElement, isDebug: Boolean, isBatchMode: Boolean, isFirstChunk: Boolean,
                           textRange: TextRange? = null): Promise<Boolean> {
    return AsyncPromise<Boolean>().also { promise ->
      if (element.containingFile == null || element.context == null) return promise.apply { setError("parent not found") }
      val fileEditor = FileEditorManager.getInstance(element.project).getSelectedEditor(element.containingFile.originalFile.virtualFile)
      val editor = EditorUtil.getEditorEx(fileEditor) ?: return promise.apply { setError("editor not found") }
      RConsoleManager.getInstance(element.project).currentConsoleAsync.onSuccess {
        runAsync {
          runReadAction {
            runHandlersAndExecuteChunk(it, element, editor, isDebug, isBatchMode, isFirstChunk, textRange)
          }.processed(promise)
        }
      }
    }
  }

  @VisibleForTesting
  internal fun runHandlersAndExecuteChunk(console: RConsoleView, element: PsiElement, editor: EditorEx,
                                          isDebug: Boolean = false, isBatchMode: Boolean = false,
                                          isFirstChunk: Boolean = true, textRange: TextRange? = null): Promise<Boolean> {
    val promise = AsyncPromise<Boolean>()
    val rInterop = console.rInterop
    val parent = element.parent
    val chunkText = parent?.text ?: return promise.apply { setError("parent is null") }
    val codeElement = findCodeElement(parent) ?: return promise.apply { setError("cannot find code fence") }
    val project = element.project
    val file = element.containingFile
    val inlayElement = findInlayElementByFenceElement(element) ?: return promise.apply { setError("cannot find code fence") }
    val chunkPath = ChunkPath.create(inlayElement) ?: return promise.apply { setError("cannot create cache dir") }
    val rMarkdownParameters = createRMarkdownParameters(file)
    val cacheDirectory = createCacheDirectory(chunkPath)
    val screenParameters = createScreenParameters(editor, project)
    val imagesDirectory = chunkPath.getImagesDirectory()
    val graphicsDeviceRef = AtomicReference<RGraphicsDevice>()

    if (!ensureConsoleIsReady(console, project, promise)) return promise

    // run before chunk handler without read action
    val beforeChunkPromise = runAsync {
      beforeRunChunk(rInterop, rMarkdownParameters, chunkText)
      val device = RGraphicsDevice(rInterop, File(imagesDirectory), screenParameters, inMemory = false)
      graphicsDeviceRef.set(device)
    }

    val range = if (textRange == null) {
      codeElement.textRange
    } else {
      textRange.intersection(codeElement.textRange) ?: TextRange.EMPTY_RANGE
    }
    val request = rInterop.prepareReplSourceFileRequest(file.virtualFile, range, isDebug)

    updateProgressBar(editor, inlayElement)
    val prepare = if (isFirstChunk) rInterop.interpreter.prepareForExecution() else resolvedPromise()
    editor.rMarkdownNotebook?.get(inlayElement)?.let { e ->
      e.clearOutputs(removeFiles = false)
      e.updateProgressStatus(InlayProgressStatus(ProgressStatus.RUNNING))
    }
    prepare.onProcessed {
      executeCode(request, console, codeElement, beforeChunkPromise) {
        getEditorManager(editor)?.addTextToInlay(inlayElement, it.text, it.kind)
        editor.rMarkdownNotebook?.let { nb -> nb[inlayElement]?.addText(it.text, it.kind) }
      }.onProcessed { result ->
        dumpAndShutdownAsync(graphicsDeviceRef.get()).onProcessed {
          pullOutputsWithLogAsync(rInterop, cacheDirectory).onProcessed {
            afterRunChunk(element, rInterop, result, promise, console, editor, inlayElement, isBatchMode)
          }
        }
      }
    }

    return promise
  }

  private fun dumpAndShutdownAsync(device: RGraphicsDevice?): Promise<Unit> {
    return device?.dumpAndShutdownAsync() ?: resolvedPromise()
  }

  private fun pullOutputsWithLogAsync(rInterop: RInterop, cacheDirectory: String): Promise<Unit> {
    return pullOutputsAsync(rInterop, cacheDirectory).onError { e ->
      LOGGER.error("Run Chunk: cannot pull outputs", e)
    }
  }

  private fun pullOutputsAsync(rInterop: RInterop, cacheDirectory: String) = runAsync {
    val response = rInterop.pullChunkOutputPaths()
    for (relativePath in response.relativePaths) {
      val remotePath = "${response.directory}/$relativePath"
      val localPath = "$cacheDirectory/$relativePath"
      File(localPath).parentFile.mkdirs()
      rInterop.interpreter.downloadFileFromHost(remotePath, localPath)
    }
  }

  private fun createRMarkdownParameters(file: PsiFile) =
    "---${System.lineSeparator()}${RMarkdownUtil.findMarkdownParagraph(file)?.text ?: ""}${System.lineSeparator()}---"

  private fun createCacheDirectory(chunkPath: ChunkPath): String {
    return chunkPath.getCacheDirectory().also { cacheDirectoryPath ->
      createCleanDirectory(File(cacheDirectoryPath))
      chunkPath.getNestedDirectories().forEach { File(it).mkdir() }
    }
  }

  private fun createCleanDirectory(directory: File) {
    FileUtil.delete(directory)
    directory.mkdirs()
  }

  private fun findCodeElement(parent: PsiElement?) =
    SyntaxTraverser.psiTraverser(parent).firstOrNull { it.elementType == R_FENCE_ELEMENT_TYPE }

  private fun beforeRunChunk(rInterop: RInterop, rmarkdownParameters: String, chunkText: String) {
    logNonEmptyError(rInterop.runBeforeChunk(rmarkdownParameters, chunkText))
  }

  private fun createScreenParameters(editor: EditorEx, project: Project): RGraphicsUtils.ScreenParameters {
    return if (ApplicationManager.getApplication().isHeadlessEnvironment) {
      RGraphicsUtils.ScreenParameters(Dimension(800, 600), null)
    } else {
      val inlayContentSize = InlayDimensions.calculateInlayContentSize(editor)
      if (inlayContentSize.width == 0) {
        // if editor is hidden
        inlayContentSize.width = 800
      }
      val imageSize = GraphicsPanel.calculateImageSizeForRegion(inlayContentSize)
      val resolution = RMarkdownGraphicsSettings.getInstance(project).globalResolution
      RGraphicsUtils.ScreenParameters(imageSize, resolution)
    }
  }

  private fun ensureConsoleIsReady(console: RConsoleView,
                                   project: Project,
                                   promise: AsyncPromise<Boolean>): Boolean {
    if (console.executeActionHandler.state != RConsoleExecuteActionHandler.State.PROMPT) {
      Notification("RMarkdownRunChunkStatus", RBundle.message("run.chunk.notification.title"), RBundle.message("console.previous.command.still.running"), NotificationType.WARNING)
        .notify(project)
      promise.setError(RBundle.message("console.previous.command.still.running"))
      return false
    }
    return true
  }

  private fun updateProgressBar(editor: EditorEx, inlayElement: PsiElement) {
    val inlaysManager = getEditorManager(editor)
    inlaysManager?.updateCell(inlayElement, listOf(), createTextOutput = true)
    inlaysManager?.updateInlayProgressStatus(inlayElement, InlayProgressStatus(ProgressStatus.RUNNING))
    editor.rMarkdownNotebook?.get(inlayElement)?.let { e ->
      e.updateOutputs()
      e.updateProgressStatus(InlayProgressStatus(ProgressStatus.RUNNING))
    }
  }

  private fun afterRunChunk(element: PsiElement,
                            rInterop: RInterop,
                            result: ExecutionResult?,
                            promise: AsyncPromise<Boolean>,
                            console: RConsoleView,
                            editor: EditorEx,
                            inlayElement: PsiElement,
                            isBatchMode: Boolean) {
    logNonEmptyError(rInterop.runAfterChunk())
    val outputs = result?.output
    if (outputs != null) {
      saveOutputs(outputs, element)
    }
    else {
      Notification("RMarkdownRunChunkStatus", RBundle.message("run.chunk.notification.title"), RBundle.message("run.chunk.notification.stopped"), NotificationType.INFORMATION)
        .notify(element.project)
    }
    val success = result != null && result.exception == null
    promise.setResult(success)
    if (!isBatchMode) {
      console.resetHandler()
    }
    if (ApplicationManager.getApplication().isUnitTestMode) return
    @Suppress("HardCodedStringLiteral") val status = when {
      result == null -> InlayProgressStatus(ProgressStatus.STOPPED_ERROR)
      result.exception != null -> InlayProgressStatus(ProgressStatus.STOPPED_ERROR, result.exception)
      else -> InlayProgressStatus(ProgressStatus.STOPPED_OK)
    }
    val inlaysManager = getEditorManager(editor)
    inlaysManager?.updateCell(inlayElement, createTextOutput = !success)
    inlaysManager?.updateInlayProgressStatus(inlayElement, status)
    editor.rMarkdownNotebook?.get(inlayElement)?.let { e ->
      e.updateOutputs()
      e.updateProgressStatus(status)
    }
    cleanupOutdatedOutputs(element)

    ApplicationManager.getApplication().invokeLater { editor.gutterComponentEx.revalidateMarkup() }
  }

  private data class ExecutionResult(val output: List<ProcessOutput>, val exception: String? = null)

  private fun executeCode(request: RInterop.ReplSourceFileRequest,
                          console: RConsoleView,
                          codeElement: PsiElement,
                          beforeChunkPromise: Promise<Unit>,
                          onOutput: (ProcessOutput) -> Unit = {}): Promise<ExecutionResult> {
    val result = mutableListOf<ProcessOutput>()
    val promise = AsyncPromise<ExecutionResult>()
    beforeChunkPromise.onProcessed {
      val executePromise = console.rInterop.replSourceFile(request) { s, type ->
        val output = ProcessOutput(s, type)
        result.add(output)
        onOutput(output)
      }
      executePromise.onProcessed { promise.setResult(ExecutionResult(result, if (it == null) "Interrupted" else it.exception)) }
      codeElement.project.chunkExecutionState?.interrupt?.set { executePromise.cancel() }
    }
    return promise
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
    File(ChunkPath.getDirectoryForPath(path)).takeIf { it.exists() && it.isDirectory }?.listFiles()?.filter {
      !it.isDirectory || !presentChunks.contains(it.name)
    }?.forEach { FileUtil.delete(it) }
  }

  private fun saveOutputs(outputs: List<ProcessOutput>, element: PsiElement) {
    if (outputs.any { it.text.isNotEmpty() }) {
      runReadAction { ChunkPath.create(element)?.getOutputFile() }?.let {
        val text = Gson().toJson(outputs.filter { output -> output.text.isNotEmpty() })
        val file = File(it)
        check(FileUtil.createParentDirs(file)) { "cannot create parent directories" }
        file.writeText(text)
      }
    }
  }

  fun interruptChunkExecution(project: Project) {
    val chunkExecutionState = project.chunkExecutionState ?: return
    chunkExecutionState.interrupt.get()?.invoke()
  }
}
