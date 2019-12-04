/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.hint.HintManager
import com.intellij.execution.console.BaseConsoleExecuteActionHandler
import com.intellij.execution.console.LanguageConsoleView
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPomTarget
import org.jetbrains.r.rendering.editor.ChunkExecutionState
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

  enum class State {
    PROMPT, DEBUG_PROMPT, READ_LN, BUSY, TERMINATED
  }
  @Volatile var state = State.BUSY
    private set(newState) {
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

  internal inner class ReplListener : RInterop.ReplListener {
    private var debugLines = mutableListOf<String>()
    private val stdoutCache = StringBuilder()

    private fun processStdoutLine(s: String) {
      val match = DEBUG_LINE_REGEX.matchEntire(s)
      when {
        match != null -> {
          if (debugLines.isEmpty()) {
            consoleView.print(match.groupValues[1], ConsoleViewContentType.NORMAL_OUTPUT)
          }
          debugLines.add(match.groupValues[2])
        }
        debugLines.isNotEmpty() -> debugLines[debugLines.size - 1] = debugLines.last() + s
        else -> consoleView.print(s, ConsoleViewContentType.NORMAL_OUTPUT)
      }
    }

    override fun onText(text: String, type: ProcessOutputType) {
      when (type) {
        ProcessOutputType.STDOUT -> {
          for (c in text) {
            stdoutCache.append(c)
            if (c == '\n') {
              processStdoutLine(stdoutCache.toString())
              stdoutCache.clear()
            }
          }
        }
        ProcessOutputType.STDERR -> consoleView.print(text, ConsoleViewContentType.ERROR_OUTPUT)
      }
    }

    override fun onBusy() {
      state = State.BUSY
    }

    override fun onRequestReadLn(prompt: String) {
      if (stdoutCache.isNotEmpty()) onText("\n", ProcessOutputType.STDOUT)
      state = State.READ_LN
      if (prompt.isNotBlank()) {
        val lines = prompt.lines()
        lines.dropLast(1).forEach { consoleView.print(it + "\n", ConsoleViewContentType.USER_INPUT) }
        consolePromptDecorator.mainPrompt = lines.last()
      }
    }

    override fun onPrompt(isDebug: Boolean) {
      onPromptAsync(isDebug)
    }

    fun onPromptAsync(isDebug: Boolean): Promise<Unit> {
      val currentDebugLines = debugLines
      debugLines = mutableListOf()
      return consoleView.debugger.handlePrompt(isDebug, currentDebugLines).then { isPrompt ->
        if (!isPrompt) return@then
        if (stdoutCache.isNotEmpty()) onText("\n", ProcessOutputType.STDOUT)
        rInterop.updateSysFrames()
        state = if (isDebug) State.DEBUG_PROMPT else State.PROMPT
        RLibraryWatcher.getInstance(consoleView.project).refresh()
        fireCommandExecuted()
      }
    }

    override fun onTermination() {
      state = State.TERMINATED
      consoleView.print(RBundle.message("console.process.terminated") + "\n", ConsoleViewContentType.SYSTEM_OUTPUT)
    }

    override fun onViewRequest(ref: RRef, title: String, value: RValue): Promise<Unit> {
      return RPomTarget.createPomTarget(RVar(title, ref, value)).navigateAsync(true)
    }
  }

  internal val replListener = ReplListener()

  init {
    rInterop.addReplListener(replListener)
    rInterop.replStartProcessing()
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
    if (text.startsWith("?") && !text.startsWith("??")) {
      val expression = getExpressionForHelp(console.project, text.drop(1).trim())
      DocumentationManager.getInstance(console.project).showJavaDocInfoAtToolWindow(expression, expression.originalElement)
    } else {
      fireBeforeExecution()
      state = State.BUSY
      rInterop.replExecute(text)
    }
  }

  private fun getExpressionForHelp(project: Project, request: String): PsiElement {
    val text = request.dropWhile { it == '"' }.dropLastWhile { it == '"' }
    return RElementFactory.buildRFileFromText(project, "help_from_console(\"$text\")").firstChild
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

  interface Listener {
    fun beforeExecution() { }
    fun onCommandExecuted() { }
    fun onReset() { }
  }

  companion object {
    private val DEBUG_LINE_REGEX = Regex("(.*)((debug( at .*)?|Called from|debugging in|exiting from): .*)", RegexOption.DOT_MATCHES_ALL)
  }
}

