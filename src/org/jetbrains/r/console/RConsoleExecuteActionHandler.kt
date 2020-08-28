/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.codeInsight.hint.HintManager
import com.intellij.execution.console.BaseConsoleExecuteActionHandler
import com.intellij.execution.console.ConsoleHistoryController
import com.intellij.execution.console.LanguageConsoleView
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.*
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.xdebugger.XSourcePosition
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.debugger.RDebuggerUtil
import org.jetbrains.r.documentation.RDocumentationProvider
import org.jetbrains.r.intentions.DependencyManagementFix
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.*
import org.jetbrains.r.rinterop.rstudioapi.*
import org.jetbrains.r.util.PromiseUtil
import java.util.*
import kotlin.collections.HashSet

class RConsoleExecuteActionHandler(private val consoleView: RConsoleView)
  : BaseConsoleExecuteActionHandler(false), Condition<LanguageConsoleView> {
  private val rInterop = consoleView.rInterop
  private val listeners = HashSet<Listener>()
  private val consolePromptDecorator
    get() = consoleView.consolePromptDecorator
  private var showStackTraceHandler: HyperlinkInfo? = null

  enum class State {
    PROMPT, DEBUG_PROMPT, READ_LN, BUSY, TERMINATED, SUBPROCESS_INPUT
  }

  @Volatile
  var state = State.BUSY
    internal set(newState) {
      runInEdt {
        when (newState) {
          State.PROMPT -> {
            consolePromptDecorator.mainPrompt = R_CONSOLE_PROMPT
            consolePromptDecorator.indentPrompt = R_CONSOLE_CONTINUE
          }
          State.DEBUG_PROMPT -> {
            consolePromptDecorator.mainPrompt = R_CONSOLE_DEBUG_PROMPT
            consolePromptDecorator.indentPrompt = R_CONSOLE_CONTINUE
          }
          State.READ_LN, State.SUBPROCESS_INPUT -> {
            consolePromptDecorator.mainPrompt = R_CONSOLE_READ_LN_PROMPT
            consolePromptDecorator.indentPrompt = ""
          }
          else -> {
            consolePromptDecorator.mainPrompt = ""
            consolePromptDecorator.indentPrompt = ""
          }
        }
      }
      field = newState
    }
  // should be accessed only from RInterop Thread Pool
  private val executeLaterQueue: Queue<() -> Unit> = ArrayDeque<() -> Unit>()
  val isRunningCommand: Boolean
    get() = state.let { it != State.PROMPT && it != State.DEBUG_PROMPT }

  @Volatile
  var chunkState: ChunkExecutionState? = null

  internal inner class AsyncEventsListener : RInterop.AsyncEventsListener {
    private val ansiEscapeDecoder = AnsiEscapeDecoder()

    override fun onText(text: String, type: ProcessOutputType) {
      runInEdt {
        ansiEscapeDecoder.escapeText(text, type) { s, attributes ->
          consoleView.print(s, ConsoleViewContentType.getConsoleViewType(attributes))
        }
      }
    }

    override fun onBusy() {
      fireBusy()
    }

    override fun onRequestReadLn(prompt: String) {
      state = State.READ_LN
      if (prompt.isNotBlank()) {
        runInEdt {
          val lines = prompt.lines()
          lines.dropLast(1).forEach {
            consoleView.print(it + "\n", ConsoleViewContentType.USER_INPUT)
          }
          consolePromptDecorator.mainPrompt = lines.last()
        }
      }
    }

    override fun onSubprocessInput() {
      state = State.SUBPROCESS_INPUT
    }

    override fun onPrompt(isDebug: Boolean, isDebugStep: Boolean, isBreakpoint: Boolean) {
      if (isDebug) {
        val suspend = isBreakpoint && RDebuggerUtil.processBreakpoint(consoleView)
        if (isBreakpoint && !isDebugStep && !suspend) {
          rInterop.debugCommandKeepPrevious()
          return
        }
      }
      state = if (isDebug) State.DEBUG_PROMPT else State.PROMPT
      runAsync { RLibraryWatcher.getInstance(consoleView.project).refresh() }
      pollExecuteLaterQueue()
      if (!isRunningCommand) fireCommandExecuted()
    }

    override fun onTermination() {
      state = State.TERMINATED
      runInEdt {
        consoleView.print(RBundle.message("console.process.terminated") + "\n", ConsoleViewContentType.SYSTEM_OUTPUT)
      }
    }

    override fun onViewRequest(ref: RReference, title: String, value: RValue): Promise<Unit> {
      return RPomTarget.createPomTarget(RVar(title, ref, value)).navigateAsync(true)
    }

    override fun onException(exception: RExceptionInfo) {
      executeLaterQueue.clear()
      if (exception.details is RInterrupted) return
      runInEdt {
        onText("\n", ProcessOutputType.STDERR)
        if (exception.call == null) {
          onText(RBundle.message("console.exception.message"), ProcessOutputType.STDERR)
        } else {
          onText(RBundle.message("console.exception.message.with.call", exception.call.lines().first()), ProcessOutputType.STDERR)
        }
        rInterop.lastErrorStack
          .lastOrNull { it.position?.file?.let { file -> RSourceFileManager.isTemporary(file) } == false }
          ?.position
          ?.let {
            onText(" (", ProcessOutputType.STDERR)
            consoleView.printHyperlink("${it.file.name}#${it.line + 1}", SourcePositionHyperlink(it.xSourcePosition))
            onText(")", ProcessOutputType.STDERR)
          }
        onText(": ${exception.message}\n", ProcessOutputType.STDERR)
        if (rInterop.lastErrorStack.isEmpty()) {
          showStackTraceHandler = null
        } else {
          val handler = object : HyperlinkInfo {
            override fun navigate(project: Project?) {
              if (this != showStackTraceHandler) {
                RNotificationUtil.notifyConsoleError(consoleView.project, RBundle.message("console.show.stack.trace.error"))
                return
              }
              consoleView.debuggerPanel?.showLastErrorStack()
            }

            override fun includeInOccurenceNavigation() = false
          }
          showStackTraceHandler = handler
          consoleView.printHyperlink(RBundle.message("console.show.stack.trace"), handler)
          onText("\n", ProcessOutputType.STDERR)
        }
        when (val details = exception.details) {
          is RNoSuchPackageError -> {
            consoleView.printHyperlink(RBundle.message("console.install.package.message", details.packageName), object : HyperlinkInfo {
              override fun navigate(project: Project?) {
                RequiredPackageInstaller.getInstance(consoleView.project).installPackagesWithUserPermission(
                  RBundle.message("console.utility.name"), listOf(RequiredPackage(details.packageName)), false)
                  .onError { DependencyManagementFix.showErrorNotification(consoleView.project, it) }
              }

              override fun includeInOccurenceNavigation() = false
            })
            onText("\n", ProcessOutputType.STDERR)
          }
        }
      }
    }

    override fun onShowHelpRequest(httpdResponse: RInterop.HttpdResponse) {
      invokeLater {
        RToolWindowFactory.showDocumentation(RDocumentationProvider.makeElementForText(rInterop, httpdResponse))
      }
    }

    override fun onShowFileRequest(filePath: String, title: String): Promise<Unit> {
      return consoleView.interpreter.showFileInViewer(consoleView.rInterop, filePath)
    }

    override fun onBrowseURLRequest(url: String) {
      consoleView.interpreter.showUrlInViewer(consoleView.rInterop, url)
    }

    override fun onRStudioApiRequest(functionId: RStudioApiFunctionId, args: RObject): Promise<RObject> {
      val promise = AsyncPromise<RObject>()
      invokeLater {
        try {
          when (functionId) {
            RStudioApiFunctionId.GET_SOURCE_EDITOR_CONTEXT_ID -> {
              promise.setResult(getSourceEditorContext(rInterop))
            }
            RStudioApiFunctionId.INSERT_TEXT_ID -> {
              promise.setResult(insertText(rInterop, args))
            }
            RStudioApiFunctionId.SEND_TO_CONSOLE_ID -> {
              promise.setResult(RObject.getDefaultInstance())
              sendToConsole(rInterop, args)
            }
            RStudioApiFunctionId.GET_CONSOLE_EDITOR_CONTEXT_ID -> {
              promise.setResult(getConsoleEditorContext(rInterop))
            }
            RStudioApiFunctionId.NAVIGATE_TO_FILE_ID -> {
              navigateToFile(rInterop, args).then {
                promise.setResult(it)
              }
            }
            RStudioApiFunctionId.GET_ACTIVE_PROJECT_ID -> {
              promise.setResult(getActiveProject(rInterop))
            }
            RStudioApiFunctionId.GET_ACTIVE_DOCUMENT_CONTEXT_ID -> {
              promise.setResult(getActiveDocumentContext(rInterop))
            }
            RStudioApiFunctionId.SET_SELECTION_RANGES_ID -> {
              promise.setResult(setSelectionRanges(rInterop, args))
            }
            RStudioApiFunctionId.ASK_FOR_PASSWORD_ID -> {
              askForPassword(args).then { promise.setResult(it) }
                .onError { promise.setError(it) }
            }
            RStudioApiFunctionId.SHOW_QUESTION_ID -> {
              showQuestion(args).then { promise.setResult(it) }
                .onError { promise.setError(it) }
            }
            RStudioApiFunctionId.SHOW_PROMPT_ID -> {
              showPrompt(args).then { promise.setResult(it) }
                .onError { promise.setError(it) }
            }
            RStudioApiFunctionId.ASK_FOR_SECRET_ID -> {
              askForSecret(args).then { promise.setResult(it) }
                .onError { promise.setError(it) }
            }
            RStudioApiFunctionId.SELECT_FILE_ID -> {
              selectFile(rInterop, args).then { promise.setResult(it) }
                .onError { promise.setError(it) }
            }
            RStudioApiFunctionId.SELECT_DIRECTORY_ID -> {
              selectDirectory(rInterop, args).then { promise.setResult(it) }
                .onError { promise.setError(it) }
            }
            RStudioApiFunctionId.SHOW_DIALOG_ID -> {
              showDialog(args).then { promise.setResult(it) }
                .onError { promise.setError(it) }
            }
            RStudioApiFunctionId.UPDATE_DIALOG_ID -> {
              updateDialog(args).then { promise.setResult(it) }
                .onError { promise.setError(it) }
            }
            RStudioApiFunctionId.GET_THEME_INFO -> {
              promise.setResult(getThemeInfo())
            }
            RStudioApiFunctionId.JOB_RUN_SCRIPT_ID -> {
              jobRunScript(rInterop, args).then {
                promise.setResult(it)
              }.onError {
                  promise.setError(it)
              }
            }
            RStudioApiFunctionId.JOB_REMOVE_ID -> {
              promise.setResult(jobRemove(rInterop, args))
            }
            RStudioApiFunctionId.JOB_SET_STATE_ID -> TODO()
            RStudioApiFunctionId.RESTART_SESSION_ID -> {
              promise.setResult(RObject.getDefaultInstance())
              restartSession(rInterop, args)
            }
            RStudioApiFunctionId.DOCUMENT_NEW_ID -> {
              documentNew(rInterop, args).then {
                promise.setResult(it)
              }
            }
            RStudioApiFunctionId.TERMINAL_ACTIVATE_ID -> TODO()
            RStudioApiFunctionId.TERMINAL_BUFFER_ID -> {
              promise.setResult(terminalBuffer(rInterop, args))
            }
            RStudioApiFunctionId.TERMINAL_BUSY_ID -> {
              promise.setResult(terminalBusy(rInterop, args))
            }
            RStudioApiFunctionId.TERMINAL_CLEAR_ID -> {
              promise.setResult(terminalClear(rInterop, args))
            }
            RStudioApiFunctionId.TERMINAL_CONTEXT_ID -> {
              promise.setResult(terminalContext(rInterop, args))
            }
            RStudioApiFunctionId.TERMINAL_CREATE_ID -> {
              promise.setResult(terminalCreate(rInterop, args))
            }
            RStudioApiFunctionId.TERMINAL_EXECUTE_ID -> {
              promise.setResult(terminalExecute(rInterop, args))
            }
            RStudioApiFunctionId.TERMINAL_EXITCODE_ID -> TODO()
            RStudioApiFunctionId.TERMINAL_KILL_ID -> {
              terminalKill(rInterop, args)
              promise.setResult(getRNull())
            }
            RStudioApiFunctionId.TERMINAL_LIST_ID -> {
              promise.setResult(terminalList(rInterop))
            }
            RStudioApiFunctionId.TERMINAL_RUNNING_ID -> {
              promise.setResult(terminalRunning(rInterop, args))
            }
            RStudioApiFunctionId.TERMINAL_SEND_ID -> {
              promise.setResult(terminalSend(rInterop, args))
            }
            RStudioApiFunctionId.TERMINAL_VISIBLE_ID -> {
              promise.setResult(terminalVisible(rInterop))
            }
            RStudioApiFunctionId.VIEWER_ID -> {
              viewer(rInterop, args).then {
                promise.setResult(RObject.getDefaultInstance())
              }.onError { promise.setError(it) }
            }
            RStudioApiFunctionId.VERSION_INFO_MODE_ID -> {
              promise.setResult(versionInfoMode(rInterop))
            }
            RStudioApiFunctionId.DOCUMENT_CLOSE_ID -> {
              promise.setResult(documentClose(rInterop, args))
            }
            RStudioApiFunctionId.SOURCE_MARKERS_ID -> {
              sourceMarkers(rInterop, args)
              promise.setResult(RObject.getDefaultInstance())
            }
          }
        } catch (e: Throwable) {
          promise.setError(e)
          throw e
        }
      }
      return promise
    }

    private fun pollExecuteLaterQueue() {
      assert(Thread.currentThread().name == RINTEROP_THREAD_NAME)
      while (!isRunningCommand && executeLaterQueue.isNotEmpty()) executeLaterQueue.poll().invoke()
    }
  }

  private val asyncEventsListener = AsyncEventsListener()

  init {
    consolePromptDecorator.mainPrompt = ""
    consolePromptDecorator.indentPrompt = ""
    rInterop.addAsyncEventsListener(asyncEventsListener)
    rInterop.asyncEventsStartProcessing()
  }

  /**
   * Schedule [f] execution on RInterop Thread Pool
   */
  fun <R> executeLater(f: () -> R): Promise<R> {
    val promise = AsyncPromise<R>()
    val task: () -> Unit = {
      try {
        promise.setResult(f())
      }
      catch (t: Throwable) {
        promise.setError(t)
      }
    }
    rInterop.executeTask {
      while (!isRunningCommand) {
        if (executeLaterQueue.isEmpty()) {
          task()
          return@executeTask
        }
        executeLaterQueue.poll().invoke()
      }
      executeLaterQueue.add(task)
    }
    return promise
  }

  fun interruptTextExecution() {
    if (state == State.BUSY || state == State.READ_LN || state == State.SUBPROCESS_INPUT) {
      rInterop.replInterrupt()
    }
  }

  override fun value(t: LanguageConsoleView?) = state != State.TERMINATED

  override fun runExecuteAction(console: LanguageConsoleView) {
    if (console != this.consoleView) return
    runExecuteActionImpl()
  }

  fun runExecuteActionImpl(): Promise<Unit> {
    when (state) {
      State.PROMPT, State.DEBUG_PROMPT -> {
        if (RConsoleEnterHandler.handleEnterPressed(consoleView.consoleEditor)) {
          val document = consoleView.consoleEditor.document
          RConsoleEnterHandler.analyzePrompt(consoleView)

          ConsoleHistoryController.addToHistory(consoleView, document.text)
          return splitAndExecute(document.text)
        }
      }
      State.READ_LN -> {
        val document = consoleView.consoleEditor.document
        runWriteAction {
          document.setText(document.text.lineSequence().first())
        }
        val text = consoleView.prepareExecuteAction(true, false, true)
        (UndoManager.getInstance(consoleView.project) as UndoManagerImpl).invalidateActionsFor(
          DocumentReferenceManager.getInstance().create(consoleView.currentEditor.document))
        consoleView.interpreter.prepareForExecution().onProcessed { rInterop.replSendReadLn(text) }
        fireBusy()
      }
      State.SUBPROCESS_INPUT -> {
        val text = consoleView.prepareExecuteAction(true, false, true)
        (UndoManager.getInstance(consoleView.project) as UndoManagerImpl).invalidateActionsFor(
          DocumentReferenceManager.getInstance().create(consoleView.currentEditor.document))
        consoleView.interpreter.prepareForExecution().onProcessed {
          rInterop.replSendReadLn(text + System.lineSeparator())
        }
      }
      State.BUSY -> {
        throwExceptionInTests()
        HintManager.getInstance().showErrorHint(consoleView.consoleEditor, RBundle.message("console.previous.command.still.running"))
      }
      State.TERMINATED -> {
        throwExceptionInTests()
        HintManager.getInstance().showErrorHint(consoleView.consoleEditor, RBundle.message("console.process.terminated"))
      }
    }
    return resolvedPromise()
  }

  fun splitAndExecute(code: String, isDebug: Boolean = false,
                      sourceFile: VirtualFile? = null, sourceStartOffset: Int? = null,
                      firstDebugCommand: ExecuteCodeRequest.DebugCommand = ExecuteCodeRequest.DebugCommand.CONTINUE
  ): Promise<Unit> = runReadAction {
    splitCodeForExecution(consoleView.project, code)
      .mapIndexed { index, (text, range) ->
        val doExecute = if (sourceFile == null || sourceStartOffset == null) {
          ({ consoleView.rInterop.replExecute(text, setLastValue = true, isDebug = isDebug) })
        } else {
          val newRange = TextRange(range.startOffset + sourceStartOffset, range.endOffset + sourceStartOffset)
          val debugCommand = if (index == 0) firstDebugCommand else ExecuteCodeRequest.DebugCommand.KEEP_PREVIOUS
          val request = consoleView.rInterop.prepareReplSourceFileRequest(sourceFile, newRange, isDebug, debugCommand)
          ({ consoleView.rInterop.replSourceFile(request) })
        }
        {
          executeLater {
            if (isDebug && state == State.DEBUG_PROMPT) {
              return@executeLater resolvedPromise(false)
            }
            fireBeforeExecution()
            consoleView.appendCommandText(text.trim { it <= ' ' })
            fireBusy()
            val prepare = if (index == 0) consoleView.interpreter.prepareForExecution() else resolvedPromise()
            prepare.thenAsync {
              doExecute().then { it.exception == null }
            }
          }.thenAsync { it }
        }
      }
      .let {
        PromiseUtil.runChain(it).then {
          refreshLocalFileSystem()
          Unit
        }
      }
  }

  private fun throwExceptionInTests() {
    check(!ApplicationManager.getApplication().isUnitTestMode) { "Console is busy or terminated" }
  }

  override fun execute(text: String, console: LanguageConsoleView) {
    if (console != consoleView) return
    fireBeforeExecution()
    state = State.BUSY
    rInterop.executeCodeAsync(text, isRepl = true, setLastValue = true)
  }

  @Synchronized
  fun addListener(listener: Listener) {
    listeners.add(listener)
  }

  @Synchronized
  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }

  @Synchronized
  fun resetListeners() {
    for (listener in listeners) {
      listener.onReset()
    }
  }

  @Synchronized
  fun fireCommandExecuted() {
    listeners.forEach { it.onCommandExecuted() }
  }

  @Synchronized
  fun fireBeforeExecution() {
    listeners.forEach { it.beforeExecution() }
  }

  @Synchronized
  fun fireBusy() {
    state = State.BUSY
    listeners.forEach { it.onBusy() }
  }

  interface Listener {
    fun beforeExecution() {}
    fun onCommandExecuted() {}
    fun onBusy() {}
    fun onReset() {}
  }

  companion object {
    fun splitCodeForExecution(project: Project, text: String): List<Pair<String, TextRange>> {
      val psiFile = RElementFactory.buildRFileFromText(project, text)
      return psiFile.children.asSequence()
        .filter { it is PsiWhiteSpace }
        .flatMap {
          it.text.asSequence().mapIndexedNotNull { i, c ->
            if (c == '\n') {
              TextRange(it.textRange.startOffset + i, it.textRange.startOffset + i + 1)
            } else {
              null
            }
          }
        }
        .let { sequenceOf(TextRange(0, 0)).plus(it) }
        .plus(TextRange(text.length, text.length))
        .zipWithNext { first, second -> TextRange(first.endOffset, second.startOffset) }
        .map { text.substring(it.startOffset, it.endOffset) to it }
        .toList()
    }

    private class SourcePositionHyperlink(private val position: XSourcePosition) : HyperlinkInfo {
      override fun navigate(project: Project?) {
        if (project == null) return
        position.createNavigatable(project).navigate(true)
      }

      override fun includeInOccurenceNavigation() = false
    }
  }

  private fun refreshLocalFileSystem() {
    VirtualFileManager.getInstance().asyncRefresh(null)
  }
}

