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
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.*
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.PathUtil
import com.intellij.xdebugger.XSourcePosition
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
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

    override fun onPrompt(isDebug: Boolean) {
      state = if (isDebug) State.DEBUG_PROMPT else State.PROMPT
      runAsync { RLibraryWatcher.getInstance(consoleView.project).refresh() }
      fireCommandExecuted()
      pollExecuteLaterQueue()
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

    override fun onShowFileRequest(filePath: String, title: String, content: ByteArray): Promise<Unit> {
      val promise = AsyncPromise<Unit>()
      val fileName = PathUtil.getFileName(filePath)
      val tmpFile = FileUtil.createTempFile(
        fileName.substringBeforeLast('.'),
        fileName.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" },
        true)
        .also { it.writeBytes(content) }
      invokeLater {
        RToolWindowFactory.showFile(consoleView.project, tmpFile.absolutePath).onProcessed {
          tmpFile.delete()
          promise.setResult(Unit)
        }
      }
      return promise
    }

    override fun onBrowseURLRequest(url: String) {
      BrowserUtil.browse(url)
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
                      firstDebugCommand: ExecuteCodeRequest.DebugCommand = ExecuteCodeRequest.DebugCommand.CONTINUE): Promise<Unit> {
    return splitCodeForExecution(consoleView.project, code)
      .mapIndexed { index, (text, range) ->
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
              if (sourceFile == null || sourceStartOffset == null) {
                consoleView.rInterop.replExecute(text, setLastValue = true, isDebug = isDebug).then { it.exception == null }
              } else {
                val newRange = TextRange(range.startOffset + sourceStartOffset, range.endOffset + sourceStartOffset)
                val debugCommand = if (index == 0) firstDebugCommand else ExecuteCodeRequest.DebugCommand.KEEP_PREVIOUS
                rInterop.replSourceFile(sourceFile, textRange = newRange, debug = isDebug,
                                        firstDebugCommand = debugCommand, setLastValue = true)
                  .then { it.exception == null }
              }
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
    if (state != State.BUSY) {
      state = State.BUSY
      listeners.forEach { it.onBusy() }
    }
  }

  interface Listener {
    fun beforeExecution() {}
    fun onCommandExecuted() {}
    fun onBusy() {}
    fun onReset() {}
  }

  companion object {
    fun splitCodeForExecution(project: Project, text: String): List<Pair<String, TextRange>> =
      runReadAction {
        val psiFile = RElementFactory.buildRFileFromText(project, text)
        psiFile.children.asSequence()
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

