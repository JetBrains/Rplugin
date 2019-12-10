/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.AppUIUtil
import com.intellij.ui.JBSplitter
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.IJSwingUtilities
import com.intellij.util.ui.FontInfo
import com.intellij.util.ui.UIUtil
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RLanguage
import org.jetbrains.r.debugger.RDebugger
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rendering.editor.AdvancedTextEditor
import org.jetbrains.r.rinterop.RInterop
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JComponent

class RConsoleView(val rInterop: RInterop,
                   val interpreterPath: String,
                   project: Project,
                   title: String) : LanguageConsoleImpl(project, title, RLanguage.INSTANCE) {
  val consoleRuntimeInfo = RConsoleRuntimeInfoImpl(rInterop)
  val isRunningCommand: Boolean
    get() = executeActionHandler.state != RConsoleExecuteActionHandler.State.PROMPT &&
            executeActionHandler.state != RConsoleExecuteActionHandler.State.DEBUG_PROMPT

  internal val debugger = RDebugger(this).also { Disposer.register(this, it) }
  val executeActionHandler = RConsoleExecuteActionHandler(this)

  private val onSelectListeners = mutableListOf<() -> Unit>()
  private var promiseToInterrupt: CancellablePromise<Unit>? = null

  init {
    consolePromptDecorator.mainPrompt = ""
    consolePromptDecorator.indentPrompt = ""
    Disposer.register(this, rInterop)
    file.putUserData(IS_R_CONSOLE_KEY, true)
    consoleEditor.putUserData(RConsoleAutopopupBlockingHandler.REPL_KEY, this)
    executeActionHandler.addListener(object : RConsoleExecuteActionHandler.Listener {
      var previousWidth = 0

      override fun beforeExecution() {
        val width = (editor as? EditorImpl)?.let {
          val fontMetrics = it.getFontMetrics(Font.PLAIN)
          if (FontInfo.isMonospaced(fontMetrics.font)) {
            (it.component.width - it.scrollPane.verticalScrollBar.width) / fontMetrics.charWidth(' ')
          } else {
            null
          }
        } ?: DEFAULT_WIDTH
        if (previousWidth != width) {
          previousWidth = width
          rInterop.setOutputWidth(width)
        }
      }
    })
  }

  var workingDirectory: String = ""
    set(directory) {
      if (field != directory) {
        field = directory
        invokeLater {
          val contentManager = RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.contentManager ?: return@invokeLater
          val content = contentManager.contents.firstOrNull { UIUtil.findComponentOfType(it.component, RConsoleView::class.java) == this }
                        ?: return@invokeLater
          content.displayName = "[ " + directory + " ]"
        }
      }
    }

  fun executeText(text: String) {
    AppUIUtil.invokeOnEdt {
      runWriteAction {
        consoleEditor.document.setText(text)
      }
      consoleEditor.caretModel.moveToOffset(consoleEditor.document.textLength)
      executeActionHandler.runExecuteAction(this)
    }
  }

  fun executeCodeAsyncWithBusy(code: String, consumer: ((String, ProcessOutputType) -> Unit)? = null): CancellablePromise<Unit> {
    if (isRunningCommand) {
      throw RDebuggerException(RBundle.message("console.previous.command.still.running"))
    }
    val isDebug = executeActionHandler.state == RConsoleExecuteActionHandler.State.DEBUG_PROMPT
    val executePromise = rInterop.executeCodeAsync(code, consumer)
    val result = object : AsyncPromise<Unit>() {
      override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        executePromise.cancel(mayInterruptIfRunning)
        return super.cancel(mayInterruptIfRunning)
      }
    }
    rInterop.executeTask {
      promiseToInterrupt = executePromise
      executeActionHandler.replListener.onBusy()
      executePromise.onProcessed {
        rInterop.executeTask {
          promiseToInterrupt = null
          rInterop.invalidateCaches()
          executeActionHandler.replListener.onPromptAsync(isDebug).processed(result)
        }
      }
    }
    return result
  }

  fun createDebuggerPanel() {
    if (ApplicationManager.getApplication().isUnitTestMode) return
    splitWindow(debugger.createDebugWindow())
  }

  fun resetHandler() {
    executeActionHandler.resetListeners()
  }

  fun addOnSelectListener(listener: () -> Unit) {
    onSelectListeners.add(listener)
  }

  fun onSelect() {
    for (listener in onSelectListeners) {
      listener()
    }
  }

  private fun splitWindow(splitView: JComponent) {
    val console = getComponent(0)
    removeAll()
    val p = JBSplitter(false, 2f / 3)
    p.setFirstComponent(console as JComponent)
    p.setSecondComponent(splitView)
    p.isShowDividerControls = true
    p.setHonorComponentsMinimumSize(true)

    add(p, BorderLayout.CENTER)
    validate()
    repaint()
  }

  companion object {
    private val EXECUTION_SERVICE = ConcurrencyUtil.newSingleThreadExecutor("RConsole variable view")
    private const val DEFAULT_WIDTH = 160
    val IS_R_CONSOLE_KEY = Key.create<Boolean>("IS_R_CONSOLE")
    val R_CONSOLE_DATA_KEY = DataKey.create<RConsoleView>("R_CONSOLE")
    const val INTERRUPT_ACTION_ID = "org.jetbrains.r.console.RConsoleView.RInterruptAction"

    private fun getConsole(e: AnActionEvent) =
      e.getData(R_CONSOLE_DATA_KEY) ?: e.project?.let { RConsoleManager.getInstance(it).currentConsoleOrNull }
  }

  class RInterruptAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val console = getConsole(e) ?: return
      if (console.isRunningCommand) {
        console.promiseToInterrupt?.cancel() ?: run {
          if (console.debugger.isEnabled) {
            console.debugger.stop()
          } else {
            console.executeActionHandler.interruptTextExecution()
          }
        }
        console.print("^C\n", ConsoleViewContentType.SYSTEM_OUTPUT)
      } else {
        val document = console.getConsoleEditor().getDocument()
        if (document.getTextLength() != 0) {
          runWriteAction {
            CommandProcessor.getInstance().runUndoTransparentAction {
              document.deleteString(0, document.getLineEndOffset(document.getLineCount() - 1))
            }
          }
        }
      }
    }

    override fun update(e: AnActionEvent) {
      val console = getConsole(e)
      val consoleEditor = console?.getConsoleEditor()
      val enabled = consoleEditor != null &&
                    IJSwingUtilities.hasFocus(consoleEditor.getComponent()) &&
                    !consoleEditor.getSelectionModel().hasSelection() &&
                    console.executeActionHandler.state != RConsoleExecuteActionHandler.State.TERMINATED
      e.presentation.isEnabled = enabled
    }
  }

  class RSetCurrentDirectoryFromEditor : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val console = getConsole(e) ?: return
      val project = e.project ?: return
      val path = getVirtualFile(project)?.parent?.path ?: return
      runAsync {
        console.rInterop.setWorkingDir(path)
        console.debugger.refreshVariableView()
      }
    }

    override fun update(e: AnActionEvent) {
      val project = e.project
      e.presentation.isEnabled = project != null && getVirtualFile(project)?.isInLocalFileSystem == true
    }

    private fun getVirtualFile(project: Project): VirtualFile? =
      (FileEditorManager.getInstance(project).selectedEditor as? AdvancedTextEditor)?.virtualFile
  }
}

internal const val R_CONSOLE_PROMPT = "> "
internal const val R_CONSOLE_DEBUG_PROMPT = "Debug> "
internal const val R_CONSOLE_CONTINUE = "+ "
internal const val R_CONSOLE_READ_LN_PROMPT = "?> "
