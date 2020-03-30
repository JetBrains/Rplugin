/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.xdebugger.impl.XDebuggerUtilImpl
import com.intellij.xdebugger.impl.frame.XDebuggerFramesList
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import icons.PlatformDebuggerImplIcons
import org.jetbrains.r.RBundle
import org.jetbrains.r.debugger.RSourcePosition
import org.jetbrains.r.debugger.RStackFrame
import org.jetbrains.r.debugger.RXVariablesView
import org.jetbrains.r.rinterop.RSourceFileManager
import org.jetbrains.r.rinterop.RVar
import org.jetbrains.r.run.debug.stack.RXStackFrame
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

class RDebuggerPanel(private val console: RConsoleView): JPanel(BorderLayout()), Disposable, RConsoleExecuteActionHandler.Listener {
  private val rInterop = console.rInterop
  private val variablesView = RXVariablesView(console, this).also { Disposer.register(this, it) }
  private val framesView = XDebuggerFramesList(console.project)
  private val framesViewScrollPane: JBScrollPane
  private var variablesAndFramesView: JBSplitter? = null
  private val actionToolbar: ActionToolbar
  private var currentRXStackFrames = listOf<RXStackFrame>()

  private val positionHighlighter = ExecutionPointHighlighter(console.project)
  private var wasSelected: Any? = null

  private var bottomComponent: JComponent? = null

  private var isFrameViewShown: Boolean = false
    set(value) {
      invokeLater {
        if (value == field) return@invokeLater
        field = value
        if (value) {
          remove(variablesView.panel)
          variablesAndFramesView = JBSplitter(false, 0.5f).also {
            it.firstComponent = variablesView.panel
            it.secondComponent = framesViewScrollPane
            add(it, BorderLayout.CENTER)
          }
        } else {
          variablesAndFramesView?.let { remove(it) }
          add(variablesView.panel, BorderLayout.CENTER)
        }
        validate()
        repaint()
      }
    }

  init {
    framesView.addListSelectionListener {
      val frame = framesView.selectedValue as? RXStackFrame
      if (isEnabled) {
        val position = frame?.sourcePosition
        if (position == null) {
          positionHighlighter.hide()
        } else {
          positionHighlighter.show(position, frame != framesView.model.getElementAt(0), null)
        }
      }
      variablesView.stackFrame = frame
    }
    framesViewScrollPane = JBScrollPane(framesView)

    add(variablesView.panel, BorderLayout.CENTER)

    val toolbarActions = createDebugActions()
    actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, true)
    actionToolbar.component.isVisible = false
    add(actionToolbar.component, BorderLayout.NORTH)
    onCommandExecuted()
  }

  private fun createDebugActions(): ActionGroup {
    class ResumeAction : RBaseDebuggerAction(
      ActionsBundle.message("action.Resume.text"),
      AllIcons.Actions.Resume,
      "Resume",
      callback = {
        console.executeActionHandler.fireBusy()
        console.rInterop.debugCommandContinue()
      })
    class PauseAction : RBaseDebuggerAction(
      ActionsBundle.message("action.Pause.text"),
      AllIcons.Actions.Pause,
      "Pause",
      isActive = { rInterop.isDebug && console.executeActionHandler.state == RConsoleExecuteActionHandler.State.BUSY },
      callback = { console.rInterop.debugCommandPause() })
    class StopAction : RBaseDebuggerAction(
      ActionsBundle.message("action.Stop.text"),
      AllIcons.Actions.Suspend,
      "Stop",
      isActive = { rInterop.isDebug },
      callback = { console.rInterop.debugCommandStop() }
    )
    class StepOverAction : RBaseDebuggerAction(
      ActionsBundle.message("action.StepOver.text"),
      AllIcons.Actions.TraceOver,
    "StepOver",
      callback = {
        console.executeActionHandler.fireBusy()
        console.rInterop.debugCommandStepOver()
      }
    )
    class StepInto : RBaseDebuggerAction(
      ActionsBundle.message("action.StepInto.text"),
      AllIcons.Actions.TraceInto,
      "StepInto",
      callback = {
        console.executeActionHandler.fireBusy()
        console.rInterop.debugCommandStepInto()
      }
    )
    class ForceStepInto : RBaseDebuggerAction(
      ActionsBundle.message("action.ForceStepInto.text"),
      PlatformDebuggerImplIcons.Actions.Force_step_into,
      "ForceStepInto",
      callback = {
        console.executeActionHandler.fireBusy()
        console.rInterop.debugCommandForceStepInto()
      }
    )
    class StepOut : RBaseDebuggerAction(
      ActionsBundle.message("action.StepOut.text"),
      AllIcons.Actions.StepOut,
      "StepOut",
      callback = {
        console.executeActionHandler.fireBusy()
        console.rInterop.debugCommandStepOut()
      }
    )
    class RunToCursor : RBaseDebuggerAction(
      ActionsBundle.message("action.RunToCursor.text"),
      AllIcons.Actions.RunToCursor,
      "RunToCursor",
      callback = {
        val position = XDebuggerUtilImpl.getCaretPosition(console.project, it.dataContext)
        if (position != null) {
          console.executeActionHandler.fireBusy()
          console.rInterop.debugCommandRunToPosition(RSourcePosition(position.file, position.line))
        }
      }
    )

    val actions = DefaultActionGroup()
    actions.add(ResumeAction())
    actions.add(PauseAction())
    actions.add(PauseAction())
    actions.addSeparator()
    actions.add(StepOverAction())
    actions.add(StepInto())
    actions.add(ForceStepInto())
    actions.add(StepOut())
    actions.add(RunToCursor())
    actions.addSeparator()
    actions.add(ActionManager.getInstance().getAction("ViewBreakpoints"))
    actions.add(createMuteBreakpointsAction())
    return actions
  }

  private fun createAction(
    text: String, icon: Icon, actionId: String? = null,
    isActive: () -> Boolean = { console.executeActionHandler.state == RConsoleExecuteActionHandler.State.DEBUG_PROMPT },
    callback: (AnActionEvent) -> Unit): AnAction {
    val toolWindow: ToolWindow? = RConsoleToolWindowFactory.getRConsoleToolWindows(console.project)
    val action = object : AnAction(text, null, icon) {
      override fun actionPerformed(e: AnActionEvent) = callback(e)

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = toolWindow?.isVisible == true && isActive()
      }
    }
    if (actionId != null) {
      action.registerCustomShortcutSet(ShortcutSet { KeymapManager.getInstance().activeKeymap.getShortcuts(actionId) },
                                       WindowManager.getInstance().getFrame(console.project)?.rootPane, this)
    }
    return action
  }

  private fun createMuteBreakpointsAction(): ToggleAction {
    return object : DumbAwareToggleAction(ActionsBundle.message("action.Debugger.MuteBreakpoints.text"), null,
                                          AllIcons.Debugger.MuteBreakpoints) {
      private var selected = false

      override fun isSelected(e: AnActionEvent) = selected

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (selected != state) {
          selected = state
          rInterop.debugMuteBreakpoints(state)
        }
      }
    }
  }

  override fun dispose() {
    updateStack(listOf())
    positionHighlighter.hide()
  }

  override fun onCommandExecuted() {
    if (rInterop.isDebug) {
      isFrameViewShown = true
      actionToolbar.component.isVisible = true
      updateStack(createRXStackFrames(rInterop.debugStack))
    } else {
      isFrameViewShown = false
      actionToolbar.component.isVisible = false
      val stackFrame = RXStackFrame(
        RBundle.message("debugger.global.stack.frame"), null, rInterop.globalEnvLoader, false, variablesView.settings,
        rInterop.globalEnvEqualityObject)
      updateStack(listOf(stackFrame))
    }
  }

  override fun onBusy() {
    updateStack(emptyList())
    actionToolbar.component.isVisible = rInterop.isDebug
  }

  override fun beforeExecution() {
    updateStack(emptyList())
  }

  fun refreshStackFrames() {
    updateStack(currentRXStackFrames.map {
      RXStackFrame(it.functionName, it.sourcePosition, it.loader,
                   it.grayAttributes, variablesView.settings, it.equalityObject)
    })
  }

  private fun createRXStackFrames(stack: List<RStackFrame>): List<RXStackFrame> {
    return stack.mapIndexed { index, it ->
      val functionName = it.functionName ?: if (index == 0 && it.equalityObject == rInterop.globalEnvEqualityObject) {
        RBundle.message("debugger.global.stack.frame")
      } else {
        RBundle.message("debugger.anonymous.stack.frame")
      }
      RXStackFrame(functionName, it.position?.xSourcePosition, it.environment.createVariableLoader(),
                   it.position == null || RSourceFileManager.isTemporary(it.position.file),
                   variablesView.settings, it.equalityObject)
    }.reversed()
  }

  private fun updateStack(stack: List<RXStackFrame>) {
    bottomComponent?.let {
      bottomComponent = null
      remove(it)
    }
    variablesView.stackFrame?.let {
      wasSelected = it.equalityObject?.takeIf { framesView.selectedIndex != 0 }
    }
    currentRXStackFrames.forEach { Disposer.dispose(it) }
    currentRXStackFrames = stack
    framesView.model.replaceAll(stack)
    if (stack.isEmpty()) {
      variablesView.stackFrame = null
    } else {
      framesView.selectedIndex = stack.indexOfFirst { wasSelected != null && it.equalityObject == wasSelected }.takeIf { it != -1 } ?: 0
    }
  }

  fun navigate(rVar: RVar) {
    variablesView.navigate(rVar)
  }

  fun showLastErrorStack() {
    isFrameViewShown = true
    val stack = createRXStackFrames(rInterop.lastErrorStack)
    updateStack(stack)
    framesView.grabFocus()
    if (stack.isNotEmpty()) {
      framesView.selectedIndex =
        stack
          .indexOfFirst { it.sourcePosition?.file?.let { file -> !RSourceFileManager.isTemporary(file) } ?: false }
          .takeIf { it != -1 } ?: 0
    }

    JPanel(BorderLayout()).also {
      bottomComponent = it
      it.add(JBLabel(RBundle.message("debugger.panel.stack.trace.is.shown")), BorderLayout.WEST)
      add(it, BorderLayout.SOUTH)
    }
  }

  abstract inner class RBaseDebuggerAction(
    text: String,
    icon: Icon,
    actionId: String? = null,
    private val isActive: (() -> Boolean)? = null,
    private val callback: (AnActionEvent) -> Unit
  ): AnAction(text, null, icon) {
    init {
      if (actionId != null) {
        registerCustomShortcutSet(ShortcutSet { KeymapManager.getInstance().activeKeymap.getShortcuts(actionId) },
                                                WindowManager.getInstance().getFrame(console.project)?.rootPane,
                                  this@RDebuggerPanel)
      }
    }
    override fun actionPerformed(e: AnActionEvent) = callback(e)

    override fun update(e: AnActionEvent) {
      val toolWindow: ToolWindow? = RConsoleToolWindowFactory.getRConsoleToolWindows(console.project)
      e.presentation.isEnabled = toolWindow?.isVisible == true &&
                                 (isActive?.invoke() ?: console.executeActionHandler.state == RConsoleExecuteActionHandler.State.DEBUG_PROMPT)
    }
  }
}