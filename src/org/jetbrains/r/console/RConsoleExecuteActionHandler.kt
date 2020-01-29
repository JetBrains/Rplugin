/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.codeInsight.hint.HintManager
import com.intellij.execution.console.BaseConsoleExecuteActionHandler
import com.intellij.execution.console.LanguageConsoleView
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.documentation.RDocumentationUtil
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.rendering.editor.ChunkExecutionState
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.rinterop.RValue
import org.jetbrains.r.rinterop.RVar

class RConsoleExecuteActionHandler(private val consoleView: RConsoleView)
  : BaseConsoleExecuteActionHandler(false), Condition<LanguageConsoleView> {
  private val rInterop = consoleView.rInterop
  private val listeners = HashSet<Listener>()
  private val consolePromptDecorator
    get() = consoleView.consolePromptDecorator
  private var showStackTraceHandler: HyperlinkInfo? = null

  enum class State {
    PROMPT, DEBUG_PROMPT, READ_LN, BUSY, TERMINATED
  }
  @Volatile var state = State.BUSY
    internal set(newState) {
      when (newState) {
        State.PROMPT -> {
          consolePromptDecorator.mainPrompt = R_CONSOLE_PROMPT
          consolePromptDecorator.indentPrompt = R_CONSOLE_CONTINUE
        }
        State.DEBUG_PROMPT -> {
          consolePromptDecorator.mainPrompt = R_CONSOLE_DEBUG_PROMPT
          consolePromptDecorator.indentPrompt = R_CONSOLE_CONTINUE
        }
        State.READ_LN -> {
          consolePromptDecorator.mainPrompt = R_CONSOLE_READ_LN_PROMPT
          consolePromptDecorator.indentPrompt = ""
        }
        else -> {
          consolePromptDecorator.mainPrompt = ""
          consolePromptDecorator.indentPrompt = ""
        }
      }
      field = newState
    }

  @Volatile
  var chunkState: ChunkExecutionState? = null

  internal inner class AsyncEventsListener : RInterop.AsyncEventsListener {
    override fun onText(text: String, type: ProcessOutputType) {
      when (type) {
        ProcessOutputType.STDOUT -> consoleView.print(text, ConsoleViewContentType.NORMAL_OUTPUT)
        ProcessOutputType.STDERR -> consoleView.print(text, ConsoleViewContentType.ERROR_OUTPUT)
      }
    }

    override fun onBusy() {
      fireBusy()
    }

    override fun onRequestReadLn(prompt: String) {
      state = State.READ_LN
      if (prompt.isNotBlank()) {
        val lines = prompt.lines()
        lines.dropLast(1).forEach { consoleView.print(it + "\n", ConsoleViewContentType.USER_INPUT) }
        consolePromptDecorator.mainPrompt = lines.last()
      }
    }

    override fun onPrompt(isDebug: Boolean) {
      state = if (isDebug) State.DEBUG_PROMPT else State.PROMPT
      runAsync { RLibraryWatcher.getInstance(consoleView.project).refresh() }
      fireCommandExecuted()
    }

    override fun onTermination() {
      state = State.TERMINATED
      consoleView.print(RBundle.message("console.process.terminated") + "\n", ConsoleViewContentType.SYSTEM_OUTPUT)
    }

    override fun onViewRequest(ref: RRef, title: String, value: RValue): Promise<Unit> {
      return RPomTarget.createPomTarget(RVar(title, ref, value)).navigateAsync(true)
    }

    override fun onException(text: String) {
      consoleView.print("\n" + RBundle.message("console.exception.message", text) + "\n", ConsoleViewContentType.ERROR_OUTPUT)
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
      consoleView.print("\n", ConsoleViewContentType.ERROR_OUTPUT)
    }

    override fun onShowHelpRequest(content: String, url: String) {
      RToolWindowFactory.showDocumentation(RDocumentationUtil.makeElementForText(rInterop, content, url))
    }

    override fun onShowFileRequest(filePath: String, title: String): Promise<Unit> {
      val promise = AsyncPromise<Unit>()
      invokeLater {
        RToolWindowFactory.showFile(consoleView.project, filePath).onProcessed {
          promise.setResult(Unit)
        }
      }
      return promise
    }
  }

  private val asyncEventsListener = AsyncEventsListener()

  init {
    consolePromptDecorator.mainPrompt = ""
    consolePromptDecorator.indentPrompt = ""
    rInterop.addAsyncEventsListener(asyncEventsListener)
    rInterop.asyncEventsStartProcessing()
  }

  fun interruptTextExecution() {
    if (state == State.BUSY || state == State.READ_LN) {
      rInterop.replInterrupt()
    }
  }

  override fun value(t: LanguageConsoleView?) = state != State.TERMINATED

  override fun runExecuteAction(console: LanguageConsoleView) {
    if (console != this.consoleView) return
    when (state) {
      State.PROMPT, State.DEBUG_PROMPT -> {
        if (RConsoleEnterHandler.handleEnterPressed(consoleView.consoleEditor)) {
          super.runExecuteAction(consoleView)
        }
      }
      State.READ_LN -> {
        val document = consoleView.consoleEditor.document
        runWriteAction {
          document.setText(document.text.lineSequence().first())
        }
        val text = consoleView.prepareExecuteAction(true, false, true)
        (UndoManager.getInstance(consoleView.project) as UndoManagerImpl).invalidateActionsFor(
          DocumentReferenceManager.getInstance().create(consoleView.getCurrentEditor().document))
        state = State.BUSY
        rInterop.replSendReadLn(text)
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
  }

  private fun throwExceptionInTests() {
    check(!ApplicationManager.getApplication().isUnitTestMode) { "Console is busy or terminated" }
  }

  override fun execute(text: String, console: LanguageConsoleView) {
    if (console != consoleView) return
    fireBeforeExecution()
    state = State.BUSY
    rInterop.executeCodeAsync(text, isRepl = true)
  }

  fun addListener(listener: Listener) {
    listeners.add(listener)
  }

  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }

  fun resetListeners() {
    for (listener in listeners) {
      listener.onReset()
    }
  }

  fun fireCommandExecuted() {
    listeners.forEach { it.onCommandExecuted() }
  }

  fun fireBeforeExecution() {
    listeners.forEach { it.beforeExecution() }
  }

  fun fireBusy() {
    if (state != State.BUSY) {
      state = State.BUSY
      listeners.forEach { it.onBusy() }
    }
  }

  interface Listener {
    fun beforeExecution() { }
    fun onCommandExecuted() { }
    fun onBusy() { }
    fun onReset() { }
  }
}

