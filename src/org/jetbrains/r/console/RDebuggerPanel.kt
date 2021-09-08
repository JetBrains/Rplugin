/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.DocumentUtil
import com.intellij.xdebugger.XSourcePositionWrapper
import com.intellij.xdebugger.impl.DebuggerSupport
import com.intellij.xdebugger.impl.XDebuggerUtilImpl
import com.intellij.xdebugger.impl.actions.DebuggerToggleActionHandler
import com.intellij.xdebugger.impl.frame.XDebuggerFramesList
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import icons.RIcons
import org.jetbrains.r.RBundle
import org.jetbrains.r.debugger.RSourcePosition
import org.jetbrains.r.debugger.RStackFrame
import org.jetbrains.r.debugger.RXVariablesView
import org.jetbrains.r.rinterop.RSourceFileManager
import org.jetbrains.r.rinterop.RVar
import org.jetbrains.r.run.debug.stack.RXStackFrame
import org.jetbrains.r.util.tryRegisterDisposable
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
  private var isActionToolbarShown: Boolean = false
    set(value) {
      if (field != value) {
        field = value
        invokeLater {
          if (value) {
            add(actionToolbar.component, BorderLayout.NORTH)
          } else {
            remove(actionToolbar.component)
          }
          validate()
          repaint()
        }
      }
    }
  private var currentRXStackFrames = listOf<RXStackFrame>()

  private val positionHighlighter = ExecutionPointHighlighter(console.project)
  private var shouldUpdateHighlighter = true
  private var wasCommandExecuted = false
  private var highlightedPosition: RSourcePosition? = null
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
  internal var breakpointsMuted = false
    set(value) {
      if (field != value) {
        field = value
        rInterop.debugMuteBreakpoints(value)
      }
    }
  private var fileWithNonMatchingSourceNotification: VirtualFile? = null
    set(value) {
      if (value == field) return
      field?.let {
        it.putUserData(RSourceChangedEditorNotificationProvider.FILE_KEY, null)
        EditorNotifications.getInstance(console.project).updateNotifications(it)
      }
      field = value
      field?.let {
        it.putUserData(RSourceChangedEditorNotificationProvider.FILE_KEY, true)
        EditorNotifications.getInstance(console.project).updateNotifications(it)
      }
    }

  init {
    framesView.addListSelectionListener {
      val frame = framesView.selectedValue as? RXStackFrame
      fileWithNonMatchingSourceNotification = null
      if (isEnabled) {
        val position = frame?.sourcePosition
        if (position == null) {
          if (shouldUpdateHighlighter) {
            positionHighlighter.hide()
            highlightedPosition = null
          }
        } else {
          val rStackFrame = frame.rStackFrame
          val newPosition = if (rStackFrame?.extendedPosition != null) {
            object : XSourcePositionWrapper(position), ExecutionPointHighlighter.HighlighterProvider {
              override fun getHighlightRange() = rStackFrame.extendedPosition
            }
          } else {
            position
          }
          if (shouldUpdateHighlighter || highlightedPosition != rStackFrame?.position) {
            positionHighlighter.show(newPosition, frame != framesView.model.getElementAt(0), null)
            highlightedPosition = rStackFrame?.position
          }
          updateNonMatchingSourceNotification(rStackFrame)
        }
      }
      variablesView.stackFrame = frame
    }
    framesViewScrollPane = JBScrollPane(framesView)

    add(variablesView.panel, BorderLayout.CENTER)

    val toolbarActions = createDebugActions()
    actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.DEBUGGER_TOOLBAR, toolbarActions, true)
    onCommandExecuted()
  }

  private fun updateNonMatchingSourceNotification(frame: RStackFrame?) {
    val textFromR = frame?.sourcePositionText ?: return
    val file = frame.position?.file ?: return
    val textInFile = runReadAction {
      val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runReadAction null
      val line = frame.position.line
      try {
        val range = frame.extendedPosition ?: DocumentUtil.getLineTextRange(document, line)
        document.getText(range)
      } catch (e: IndexOutOfBoundsException) {
        null
      }
    }
    if (textInFile == null || !StringUtil.equalsIgnoreWhitespaces(textInFile, textFromR)) {
      fileWithNonMatchingSourceNotification = file
    }
  }

  private fun createDebugActions(): ActionGroup {
    class ResumeAction : RBaseDebuggerAction(
      ActionsBundle.message("action.Resume.text"),
      AllIcons.Actions.Resume,
      "Resume",
      callback = {
        wasCommandExecuted = true
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
        wasCommandExecuted = true
        console.executeActionHandler.fireBusy()
        console.rInterop.debugCommandStepOver()
      }
    )
    class StepInto : RBaseDebuggerAction(
      ActionsBundle.message("action.StepInto.text"),
      AllIcons.Actions.TraceInto,
      "StepInto",
      callback = {
        wasCommandExecuted = true
        console.executeActionHandler.fireBusy()
        console.rInterop.debugCommandStepInto()
      }
    )
    class StepIntoMyCode : RBaseDebuggerAction(
      RBundle.message("action.StepIntoMyCode.text"),
      RIcons.Debug.StepIntoMyCode,
      "ForceStepInto",
      callback = {
        wasCommandExecuted = true
        console.executeActionHandler.fireBusy()
        console.rInterop.debugCommandStepIntoMyCode()
      }
    )
    class StepOut : RBaseDebuggerAction(
      ActionsBundle.message("action.StepOut.text"),
      AllIcons.Actions.StepOut,
      "StepOut",
      callback = {
        wasCommandExecuted = true
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
          wasCommandExecuted = true
          console.executeActionHandler.fireBusy()
          console.rInterop.debugCommandRunToPosition(RSourcePosition(position.file, position.line))
        }
      }
    )

    val actions = DefaultActionGroup()
    actions.add(ResumeAction())
    actions.add(PauseAction())
    actions.add(StopAction())
    actions.addSeparator()
    actions.add(StepOverAction())
    actions.add(StepInto())
    actions.add(StepIntoMyCode())
    actions.add(StepOut())
    actions.add(RunToCursor())
    actions.addSeparator()
    actions.add(ActionManager.getInstance().getAction("ViewBreakpoints"))
    actions.add(createMuteBreakpointsAction())
    return actions
  }

  private fun createMuteBreakpointsAction(): ToggleAction {
    return object : DumbAwareToggleAction(ActionsBundle.message("action.XDebugger.MuteBreakpoints.text"), null,
                                          AllIcons.Debugger.MuteBreakpoints) {
      override fun isSelected(e: AnActionEvent) = breakpointsMuted

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        breakpointsMuted = state
      }
    }
  }

  override fun dispose() = invokeLater {
    positionHighlighter.hide()
    fileWithNonMatchingSourceNotification = null
  }

  override fun onCommandExecuted() {
    if (rInterop.isDebug) {
      isFrameViewShown = true
      isActionToolbarShown = true
      updateStack(createRXStackFrames(rInterop.debugStack))
      invokeLater { shouldUpdateHighlighter = true }
    } else {
      wasCommandExecuted = false
      shouldUpdateHighlighter = false
      isFrameViewShown = false
      isActionToolbarShown = false
      val stackFrame = RXStackFrame(
        RBundle.message("debugger.global.stack.frame"), null, rInterop.globalEnvLoader, false, variablesView.settings,
        rInterop.globalEnvEqualityObject)
      updateStack(listOf(stackFrame))
    }
  }

  override fun onBusy() {
    if (!wasCommandExecuted) {
      shouldUpdateHighlighter = false
    }
    wasCommandExecuted = false
    updateStack(emptyList())
    isActionToolbarShown = rInterop.isDebug
  }

  override fun beforeExecution() {
    updateStack(emptyList())
  }

  fun refreshStackFrames() {
    updateStack(currentRXStackFrames.map {
      RXStackFrame(it.functionName, it.rStackFrame, it.loader,
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
      RXStackFrame(functionName, it, it.environment.createVariableLoader(),
                   it.position == null || RSourceFileManager.isTemporary(it.position.file),
                   variablesView.settings, it.equalityObject)
    }.reversed()
  }

  private fun updateStack(stack: List<RXStackFrame>) {
    stack.forEach { tryRegisterDisposable(it) }
    invokeLater {
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
    shortcutsActionId: String? = null,
    private val isActive: (() -> Boolean)? = null,
    private val callback: (AnActionEvent) -> Unit
  ): DumbAwareAction(text, null, icon) {
    init {
      if (shortcutsActionId != null) {
        registerCustomShortcutSet(ShortcutSet { KeymapManager.getInstance().activeKeymap.getShortcuts(shortcutsActionId) },
                                  WindowManager.getInstance().getFrame(console.project)?.rootPane,
                                  this@RDebuggerPanel)
      }
    }
    override fun actionPerformed(e: AnActionEvent) = callback(e)

    override fun update(e: AnActionEvent) {
      val toolWindow: ToolWindow? = RConsoleToolWindowFactory.getRConsoleToolWindows(console.project)
      e.presentation.isEnabled = toolWindow?.isVisible == true &&
                                 (isActive?.invoke() ?:
                                  (console.executeActionHandler.state == RConsoleExecuteActionHandler.State.DEBUG_PROMPT))
    }
  }
}

class RDebuggerSupport : DebuggerSupport() {
  override fun getMuteBreakpointsHandler(): DebuggerToggleActionHandler {
    return object : DebuggerToggleActionHandler() {
      override fun isEnabled(project: Project, event: AnActionEvent?): Boolean {
        return getPanel(project) != null
      }

      override fun isSelected(project: Project, event: AnActionEvent?): Boolean {
        return getPanel(project)?.breakpointsMuted ?: false
      }

      override fun setSelected(project: Project, event: AnActionEvent?, state: Boolean) {
        getPanel(project)?.apply { breakpointsMuted = state }
      }

      private fun getPanel(project: Project): RDebuggerPanel? {
        return RConsoleManager.getInstance(project).currentConsoleOrNull?.debuggerPanel
      }
    }
  }
}

class RSourceChangedEditorNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {
  private val KEY: Key<EditorNotificationPanel> = Key.create("org.jetbrains.r.console.RSourceChangedEditorNotificationProvider")

  override fun getKey() = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    if (file.getUserData(FILE_KEY) != true) return null
    return EditorNotificationPanel(fileEditor).text(RBundle.message("debugger.file.has.changed.notification"))
  }

  companion object {
    val FILE_KEY = Key.create<Boolean>("org.jetbrains.r.console.RSourceChangedEditorNotificationProvider.fileKey")
  }
}