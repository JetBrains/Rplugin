/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk


import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.concurrency.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
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
import org.jetbrains.r.rmarkdown.RmdFenceProvider
import org.jetbrains.r.run.graphics.RGraphicsDevice
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.settings.RMarkdownGraphicsSettings
import org.jetbrains.r.visualization.inlays.RInlayDimensions
import org.jetbrains.r.visualization.inlays.components.GraphicsPanel
import org.jetbrains.r.visualization.inlays.components.InlayProgressStatus
import org.jetbrains.r.visualization.inlays.components.ProcessOutput
import org.jetbrains.r.visualization.inlays.components.RProgressStatus
import java.awt.Dimension
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


private val LOG = fileLogger()


@Service(Service.Level.PROJECT)
class RunChunkHandler(
  private val project: Project,
  private val coroutineScope: CoroutineScope,
) {

  fun interruptChunkExecution() {
    val chunkExecutionState = project.chunkExecutionState ?: return
    chunkExecutionState.interrupt.get()?.invoke()
  }

  fun runAllChunks(psiFile: PsiFile, editor: Editor) {
    runAllChunks(psiFile, editor, start = 0, end = Int.MAX_VALUE)
  }

  fun runAllChunks(
    psiFile: PsiFile,
    editor: Editor,
    start: Int,
    end: Int,
    runSelectedCode: Boolean = false,
    isDebug: Boolean = false,
  ) {
    coroutineScope.launch(Dispatchers.IO + ModalityState.defaultModalityState().asContextElement()) {
      val state = ChunkExecutionState(editor)

      state.useCurrent {
        runAllChunks(psiFile, state.currentPsiElement, state.terminationRequired, start, end, runSelectedCode, isDebug)
      }
    }
  }

  private suspend fun runAllChunks(
    psiFile: PsiFile,
    currentElement: AtomicReference<PsiElement>,
    terminationRequired: AtomicBoolean,
    start: Int,
    end: Int,
    runSelectedCode: Boolean,
    isDebug: Boolean,
  ) {
    val console = RConsoleManager.getInstance(psiFile.project).awaitCurrentConsole().getOrNull() ?: return

    try {
      val chunks = readAction {
        PsiTreeUtil.collectElements(psiFile) {
          isChunkFenceLang(it) && if (runSelectedCode) {
            it.parent?.textRange?.intersects(TextRange(start, end)) == true
          }
          else {
            (it.textRange.endOffset - 1) in start..end
          }
        }
      }
      for ((index, element) in chunks.withIndex()) {
        if (terminationRequired.get()) break

        currentElement.set(element)
        val proceed = execute(element, isDebug = isDebug, isBatchMode = true,
                              isFirstChunk = index == 0,
                              textRange = if (runSelectedCode) TextRange(start, end) else null)

        currentElement.set(null)
        if (!proceed) break
      }
    }
    catch (t: Throwable) {
      console.resetHandler()
      LOG.error("Cannot execute all chunks", t)
    }
  }

  @Throws(Throwable::class)
  suspend fun execute(
    element: PsiElement,
    isDebug: Boolean = false,
    isBatchMode: Boolean = false,
    isFirstChunk: Boolean = true,
    textRange: TextRange? = null,
  ): Boolean {
    return withContext(Dispatchers.EDT) {
      require(element.project == project)
      RMarkdownUtil.checkOrInstallPackages(project, RBundle.message("run.chunk.executor.name"))

      if (element.containingFile == null || element.context == null) throw RunChunkHandlerException("parent not found")
      val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(element.containingFile.originalFile.virtualFile)
      val editor = EditorUtil.getEditorEx(fileEditor) ?: throw RunChunkHandlerException("editor not found")
      val console = RConsoleManager.getInstance(project).awaitCurrentConsole().getOrNull()
      if (console == null) return@withContext false

      return@withContext withContext(Dispatchers.IO) {
        blockingContext {
          ReadAction.compute<Promise<Boolean>, Throwable> {
            runHandlersAndExecuteChunk(console, element, editor, isDebug, isBatchMode, isFirstChunk, textRange)
          }
        }.await()
      }
    }
  }

  private data class PsiElementWithContext(
    val element: PsiElement,
    val chunkText: String,
    val codeElement: CodeElement,
    val file: PsiFile,
    val inlayElement: PsiElement,
    val chunkPath: ChunkPath,
    val rMarkdownParameters: String,
    val screenParameters: RGraphicsUtils.ScreenParameters,
  ) {

    /** should be inside read action */
    @Throws(RunChunkHandlerException::class)
    fun makeRequest(textRange: TextRange?, console: RConsoleView, isDebug: Boolean): RInterop.ReplSourceFileRequest {
      val range = codeElement.getTextRange(textRange)
      val originalRequest = console.rInterop.prepareReplSourceFileRequest(file.virtualFile, range, isDebug)
      return modifyRequestIfPython(originalRequest, codeElement)
    }
  }

  /** should be inside read action */
  @Throws(RunChunkHandlerException::class)
  private fun readElementContext(element: PsiElement, editor: EditorEx): PsiElementWithContext {
    val parent = element.parent
    val chunkText = parent?.text ?: throw RunChunkHandlerException("parent is null")
    val codeElement = findCodeElement(parent) ?: throw RunChunkHandlerException("cannot find code fence")
    val file = element.containingFile ?: throw RunChunkHandlerException("containing file is null")
    val inlayElement = findInlayElementByFenceElement(element) ?: throw RunChunkHandlerException("cannot find code fence")
    val chunkPath = ChunkPath.create(inlayElement) ?: throw RunChunkHandlerException("cannot create cache dir")
    val rMarkdownParameters = createRMarkdownParameters(file)
    val screenParameters = createScreenParameters(editor, project)

    return PsiElementWithContext(element, chunkText, codeElement, file, inlayElement, chunkPath, rMarkdownParameters, screenParameters)
  }

  @VisibleForTesting
  internal fun runHandlersAndExecuteChunk(
    console: RConsoleView,
    element: PsiElement,
    editor: EditorEx,
    isDebug: Boolean = false,
    isBatchMode: Boolean = false,
    isFirstChunk: Boolean = true,
    textRange: TextRange? = null,
  ): Promise<Boolean> {
    require(element.project == project)

    val promise = AsyncPromise<Boolean>()

    val elementWithContext = try {
      readElementContext(element, editor)
    }
    catch (ex: RunChunkHandlerException) {
      return promise.apply { setError(ex) }
    }

    val cacheDirectory = createCacheDirectory(elementWithContext.chunkPath)
    val imagesDirectory = elementWithContext.chunkPath.getImagesDirectory()
    val graphicsDeviceRef = AtomicReference<RGraphicsDevice>()

    if (!ensureConsoleIsReady(console, project, promise)) return promise

    // run before chunk handler without read action
    val beforeChunkPromise = runAsync {
      beforeRunChunk(console.rInterop, elementWithContext.rMarkdownParameters, elementWithContext.chunkText)
      val device = RGraphicsDevice(console.rInterop, File(imagesDirectory), elementWithContext.screenParameters, inMemory = false)
      graphicsDeviceRef.set(device)
    }

    val request =
      try {
        elementWithContext.makeRequest(textRange, console, isDebug)
      }
      catch (ex: RunChunkHandlerException) {
        return promise.apply { setError(ex) }
      }

    val prepare = if (isFirstChunk) console.rInterop.interpreter.prepareForExecutionAsync() else resolvedPromise()
    editor.rMarkdownNotebook?.get(elementWithContext.inlayElement)?.let { e ->
      e.clearOutputs(removeFiles = false)
      e.updateProgressStatus(InlayProgressStatus(RProgressStatus.RUNNING))
    }
    prepare.onProcessed {
      beforeChunkPromise.onProcessed {
        executeCode(project, request, console) {
          editor.rMarkdownNotebook?.let { nb -> nb[elementWithContext.inlayElement]?.addText(it.text, it.kind) }
        }.onProcessed { result ->
          dumpAndShutdownAsync(graphicsDeviceRef.get()).onProcessed {
            pullOutputsWithLogAsync(console.rInterop, cacheDirectory).onProcessed {
              afterRunChunk(element, console.rInterop, result, promise, console, editor, elementWithContext.inlayElement, isBatchMode)
            }
          }
        }
      }
    }

    return promise
  }

  companion object {
    fun getInstance(project: Project): RunChunkHandler =
      project.service()

    @Throws(RunChunkHandlerException::class)
    private fun modifyRequestIfPython(request: RInterop.ReplSourceFileRequest, codeElement: CodeElement): RInterop.ReplSourceFileRequest =
      when {
        codeElement.fenceProvider.fenceLanguage === RLanguage.INSTANCE -> {
          request
        }
        codeElement.fenceProvider.fenceLanguage.id === "Python" -> {
          // TODO understand why output is not captured
          request.copy(code = "reticulate::py_run_string(${wrapIntoRString(request.code)})")
        }
        else -> {
          throw RunChunkHandlerException("unknown code fence")
        }
      }

    private fun dumpAndShutdownAsync(device: RGraphicsDevice?): Promise<Unit> {
      return device?.dumpAndShutdownAsync() ?: resolvedPromise()
    }

    private fun pullOutputsWithLogAsync(rInterop: RInterop, cacheDirectory: String): Promise<Unit> {
      return pullOutputsAsync(rInterop, cacheDirectory).onError { e ->
        LOG.error("Run Chunk: cannot pull outputs", e)
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

    private data class CodeElement(
      val psi: PsiElement,
      val fenceProvider: RmdFenceProvider,
    ) {
      fun getTextRange(textRange: TextRange?): TextRange =
        if (textRange == null) {
          psi.textRange
        }
        else {
          textRange.intersection(psi.textRange) ?: TextRange.EMPTY_RANGE
        }
    }

    private fun findCodeElement(parent: PsiElement?): CodeElement? {
      val providers = RmdFenceProvider.EP_NAME.extensionList.associate { it.fenceElementType to it }
      val psi = SyntaxTraverser.psiTraverser(parent).firstOrNull { it.elementType in providers } ?: return null
      return CodeElement(psi, providers[psi.elementType]!!)
    }

    private fun wrapIntoRString(s: String): String {
      return s.map {
        when (it) {
          '\n' -> "\\n"
          '\r' -> "\\r"
          '\t' -> "\\t"
          '\\' -> "\\\\"
          '"' -> "\\\""
          else -> "$it"
        }
      }.joinToString(prefix = "\"", postfix = "\"", separator = "")
    }

    private fun beforeRunChunk(rInterop: RInterop, rmarkdownParameters: String, chunkText: String) {
      logNonEmptyError(rInterop.runBeforeChunk(rmarkdownParameters, chunkText))
    }

    private fun createScreenParameters(editor: EditorEx, project: Project): RGraphicsUtils.ScreenParameters {
      return if (ApplicationManager.getApplication().isHeadlessEnvironment) {
        RGraphicsUtils.ScreenParameters(Dimension(800, 600), null)
      }
      else {
        val inlayContentSize = RInlayDimensions.calculateInlayContentSize(editor)
        if (inlayContentSize.width == 0) {
          // if editor is hidden
          inlayContentSize.width = 800
        }
        val imageSize = GraphicsPanel.calculateImageSizeForRegion(inlayContentSize)
        val resolution = RMarkdownGraphicsSettings.getInstance(project).globalResolution
        RGraphicsUtils.ScreenParameters(imageSize, resolution)
      }
    }

    private fun ensureConsoleIsReady(
      console: RConsoleView,
      project: Project,
      promise: AsyncPromise<Boolean>,
    ): Boolean {
      if (console.executeActionHandler.state != RConsoleExecuteActionHandler.State.PROMPT) {
        Notification("RMarkdownRunChunkStatus", RBundle.message("run.chunk.notification.title"), RBundle.message("console.previous.command.still.running"), NotificationType.WARNING)
          .notify(project)
        promise.setError(RBundle.message("console.previous.command.still.running"))
        return false
      }
      return true
    }

    private fun afterRunChunk(
      element: PsiElement,
      rInterop: RInterop,
      result: ExecutionResult?,
      promise: AsyncPromise<Boolean>,
      console: RConsoleView,
      editor: EditorEx,
      inlayElement: PsiElement,
      isBatchMode: Boolean,
    ) {
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
        result == null -> InlayProgressStatus(RProgressStatus.STOPPED_ERROR)
        result.exception != null -> InlayProgressStatus(RProgressStatus.STOPPED_ERROR, result.exception)
        else -> InlayProgressStatus(RProgressStatus.STOPPED_OK)
      }
      editor.rMarkdownNotebook?.get(inlayElement)?.let { e ->
        e.updateOutputs()
        e.updateProgressStatus(status)
      }
      cleanupOutdatedOutputs(element)

      ApplicationManager.getApplication().invokeLater { editor.gutterComponentEx.revalidateMarkup() }
    }

    private data class ExecutionResult(val output: List<ProcessOutput>, val exception: String? = null)

    private fun executeCode(
      project: Project,
      request: RInterop.ReplSourceFileRequest,
      console: RConsoleView,
      onOutput: (ProcessOutput) -> Unit = {},
    ): Promise<ExecutionResult> {
      val result = mutableListOf<ProcessOutput>()
      val promise = AsyncPromise<ExecutionResult>()
      val executePromise = console.rInterop.replSourceFile(request) { s, type ->
        val output = ProcessOutput(s, type)
        result.add(output)
        onOutput(output)
      }
      executePromise.onProcessed { promise.setResult(ExecutionResult(result, if (it == null) "Interrupted" else it.exception)) }
      project.chunkExecutionState?.interrupt?.set { executePromise.cancel() }
      return promise
    }

    private fun logNonEmptyError(result: RIExecutionResult) {
      if (result.stderr.isNotBlank()) {
        LOG.error("Run Chunk: the command returns non-empty output; stdout='${result.stdout}', stderr='${result.stderr}'")
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

    private fun findInlayElementByFenceElement(element: PsiElement) =
      TreeUtil.findChildBackward(element.parent.node, MarkdownTokenTypes.CODE_FENCE_END)?.psi
  }
}
