/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.ui.AppUIUtil
import com.intellij.ui.ClickListener
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.ListenerUtil
import com.intellij.util.Alarm
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.UIUtil.getMultiClickInterval
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XNamedTreeNode
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.impl.actions.XDebuggerActions
import com.intellij.xdebugger.impl.frame.XVariablesViewBase
import com.intellij.xdebugger.impl.frame.XWatchesView
import com.intellij.xdebugger.impl.frame.actions.XMoveWatchDown
import com.intellij.xdebugger.impl.frame.actions.XMoveWatchUp
import com.intellij.xdebugger.impl.frame.actions.XWatchesTreeActionBase
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.nodes.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.console.RDebuggerPanel
import org.jetbrains.r.rinterop.RVar
import org.jetbrains.r.run.debug.stack.RXDebuggerEvaluator
import org.jetbrains.r.run.debug.stack.RXStackFrame
import org.jetbrains.r.run.debug.stack.RXVariableViewSettings
import java.awt.BorderLayout
import java.awt.event.*
import javax.swing.SwingUtilities
import javax.swing.event.TreeSelectionListener


class RXVariablesView(private val console: RConsoleView, private val debuggerPanel: RDebuggerPanel)
  : XVariablesViewBase(console.rInterop.project, RDebuggerEditorsProvider, null), XWatchesView, DataProvider {
  var stackFrame: RXStackFrame? = null
    set(frame) {
      field = frame
      AppUIUtil.invokeLaterIfProjectAlive(console.project) {
        if (frame == null) {
          clear()
        } else {
          buildTreeAndRestoreState(frame)
        }
      }
    }

  private var rootNode: WatchesRootNode? = null

  val settings = RXVariableViewSettings()

  init {
    DataManager.registerDataProvider(panel, this)
    installActions()
    installEditListeners()
    installShowListener()
    installToolbar()
  }

  override fun clear() {
    tree.sourcePosition = null
    val root = createNewRootNode(null)
    root.setInfoMessage(RBundle.message("debugger.frame.not.available"), null)
    super.clear()
  }

  override fun doCreateNewRootNode(stackFrame: XStackFrame?): XValueContainerNode<*> {
    val watchExpressions = rootNode?.watchChildren.orEmpty().map { it.expression }
    val node = WatchesRootNode(tree, this, watchExpressions, stackFrame, true)
    rootNode = node
    return node
  }

  override fun processSessionEvent(event: SessionEvent, session: XDebugSession) {
  }

  override fun removeWatches(nodes: MutableList<out XDebuggerTreeNode>?) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    val rootNode = rootNode ?: return
    rootNode.removeChildren(nodes)
  }

  override fun removeAllWatches() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    val rootNode = rootNode ?: return
    rootNode.removeAllChildren()
  }

  override fun addWatchExpression(expression: XExpression, index: Int, navigateToWatchNode: Boolean) {
    addWatchExpression(expression, index, navigateToWatchNode, false)
  }

  fun addWatchExpression(expression: XExpression, index: Int, navigateToWatchNode: Boolean, noDuplicates: Boolean) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    val rootNode = rootNode ?: return
    if (noDuplicates) {
      val child = rootNode.watchChildren.firstOrNull { it.expression == expression }
      if (child != null) {
        TreeUtil.selectNode(tree, child)
        return
      }
    }
    rootNode.addWatchExpression(stackFrame, expression, index, navigateToWatchNode)
  }

  fun moveWatchUp(node: WatchNode) {
    rootNode?.moveUp(node)
  }

  fun moveWatchDown(node: WatchNode) {
    rootNode?.moveDown(node)
  }

  private fun installActions() {
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.XNEW_WATCH, tree, this)
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.XREMOVE_WATCH, tree, this)
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.XCOPY_WATCH, tree, this)
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.XEDIT_WATCH, tree, this)
    EmptyAction.registerWithShortcutSet(XDebuggerActions.XNEW_WATCH, CommonShortcuts.getNew(), tree)
    EmptyAction.registerWithShortcutSet(XDebuggerActions.XREMOVE_WATCH, CommonShortcuts.getDelete(), tree)
  }

  private fun installEditListeners() {
    val watchTree = tree
    val quitePeriod = Alarm()
    val editAlarm = Alarm()
    val mouseListener = object : ClickListener() {
      override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
        if (!SwingUtilities.isLeftMouseButton(event) || event.isShiftDown || event.isAltDown || event.isControlDown || event.isMetaDown) {
          return false
        }
        if (!isAboveSelectedItem(event, watchTree, false) || clickCount > 1) {
          editAlarm.cancelAllRequests()
          return false
        }
        val editWatchAction = ActionManager.getInstance().getAction(XDebuggerActions.XEDIT_WATCH)
        val presentation = editWatchAction.getTemplatePresentation().clone()
        val context = DataManager.getInstance().getDataContext(watchTree)
        val actionEvent = AnActionEvent(null, context, "WATCH_TREE", presentation, ActionManager.getInstance(), 0)
        val runnable = { editWatchAction.actionPerformed(actionEvent) }
        if (editAlarm.isEmpty && quitePeriod.isEmpty) {
          editAlarm.addRequest(runnable, getMultiClickInterval())
        } else {
          editAlarm.cancelAllRequests()
        }
        return false
      }
    }
    val mouseEmptySpaceListener = object : DoubleClickListener() {
      override fun onDoubleClick(event: MouseEvent): Boolean {
        if (!isAboveSelectedItem(event, watchTree, true)) {
          rootNode?.addNewWatch()
          return true
        }
        return false
      }
    }
    ListenerUtil.addClickListener(watchTree, mouseListener)
    ListenerUtil.addClickListener(watchTree, mouseEmptySpaceListener)

    val focusListener = object : FocusListener {
      override fun focusGained(e: FocusEvent) {
        quitePeriod.addRequest(EmptyRunnable.getInstance(), getMultiClickInterval())
      }

      override fun focusLost(e: FocusEvent) {
        editAlarm.cancelAllRequests()
      }
    }
    ListenerUtil.addFocusListener(watchTree, focusListener)

    val selectionListener = TreeSelectionListener { quitePeriod.addRequest(EmptyRunnable.getInstance(), getMultiClickInterval()) }
    watchTree.addTreeSelectionListener(selectionListener)
    Disposer.register(this, Disposable {
      ListenerUtil.removeClickListener(watchTree, mouseListener)
      ListenerUtil.removeClickListener(watchTree, mouseEmptySpaceListener)
      ListenerUtil.removeFocusListener(watchTree, focusListener)
      watchTree.removeTreeSelectionListener(selectionListener)
    })
  }

  private fun installShowListener() {
    panel.addComponentListener(object : ComponentListener {
      override fun componentMoved(p0: ComponentEvent?) { }
      override fun componentResized(p0: ComponentEvent?) { }
      override fun componentHidden(p0: ComponentEvent?) { }

      override fun componentShown(p0: ComponentEvent?) {
        rootNode?.computeWatches()
      }
    })
  }

  private fun installToolbar() {
    val actions = DefaultActionGroup(
      ActionManager.getInstance().getAction("XDebugger.NewWatch"),
      ActionManager.getInstance().getAction("XDebugger.RemoveWatch"),
      object : XMoveWatchUp() {
        override fun perform(e: AnActionEvent, tree: XDebuggerTree, watchesView: XWatchesView) {
          (watchesView as? RXVariablesView)?.moveWatchUp(
            ContainerUtil.getFirstItem(XWatchesTreeActionBase.getSelectedNodes<WatchNodeImpl>(tree, WatchNodeImpl::class.java)))
        }
      },
      object : XMoveWatchDown() {
        override fun perform(e: AnActionEvent, tree: XDebuggerTree, watchesView: XWatchesView) {
          (watchesView as? RXVariablesView)?.moveWatchDown(
            ContainerUtil.getFirstItem(XWatchesTreeActionBase.getSelectedNodes<WatchNodeImpl>(tree, WatchNodeImpl::class.java)))
        }
      },
      ActionManager.getInstance().getAction("XDebugger.CopyWatch")
    )
    actions.addSeparator()
    actions.addAction(object : AnAction(RBundle.message("variable.view.clear.environment.action.text"), null, AllIcons.Actions.GC) {
      override fun actionPerformed(e: AnActionEvent) {
        val environment = stackFrame?.environment ?: return
        val yesNo = Messages.showYesNoDialog(e.project, RBundle.message("variable.view.clear.environment.message"),
                                             RBundle.message("variable.view.clear.environment.action.text"), null)
        if (yesNo == Messages.YES) {
          console.rInterop.executeTask {
            console.rInterop.clearEnvironment(environment)
            console.executeActionHandler.fireCommandExecuted()
          }
        }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !console.isRunningCommand
      }
    })
    actions.addAction(createSettingsActionGroup())
    actions.addAction(object : DumbAwareAction(ActionsBundle.message("action.EvaluateExpression.text"), null,
                                               AllIcons.Debugger.EvaluateExpression) {
      override fun actionPerformed(e: AnActionEvent) {
        RDebuggerEvaluateHandler.perform(console.project, RXDebuggerEvaluator(stackFrame ?: return), e.dataContext)
      }
    })
    val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actions, false) as ActionToolbarImpl
    toolbar.setTargetComponent(tree)
    getPanel().add(toolbar.getComponent(), BorderLayout.WEST)
  }

  private fun createSettingsActionGroup(): ActionGroup {
    return DefaultActionGroup(RBundle.message("variable.view.settings.text"), listOf(
      object : DumbAwareToggleAction(RBundle.message("variable.view.show.hidden.variables.action.text")) {
        override fun isSelected(e: AnActionEvent) = settings.showHiddenVariables

        override fun setSelected(e: AnActionEvent, state: Boolean) {
          if (state != settings.showHiddenVariables) {
            settings.showHiddenVariables = state
            debuggerPanel.refreshStackFrames()
          }
        }
      },
      object : DumbAwareToggleAction(RBundle.message("variable.view.show.classes.action.text")) {
        override fun isSelected(e: AnActionEvent) = settings.showClasses

        override fun setSelected(e: AnActionEvent, state: Boolean) {
          if (state != settings.showClasses) {
            settings.showClasses = state
            debuggerPanel.refreshStackFrames()
          }
        }
      },
      object : DumbAwareToggleAction(RBundle.message("variable.view.show.size.action.text")) {
        override fun isSelected(e: AnActionEvent) = settings.showSize

        override fun setSelected(e: AnActionEvent, state: Boolean) {
          if (state != settings.showSize) {
            settings.showSize = state
            debuggerPanel.refreshStackFrames()
          }
        }
      }
    )).also {
      it.templatePresentation.icon = AllIcons.Actions.Show
      it.isPopup = true
    }
  }

  override fun getData(dataId: String): Any? {
    return if (XWatchesView.DATA_KEY.`is`(dataId)) this else null
  }

  fun navigate(rVar: RVar) {
    val proto = rVar.ref.proto
    if (proto.hasMember() && proto.member.env.hasGlobalEnv()) {
      val target = rootNode?.children?.filterIsInstance<XNamedTreeNode>()?.firstOrNull { it.name == rVar.name } ?: return
      TreeUtil.selectNode(tree, target)
    }
  }

  companion object {
    private fun isAboveSelectedItem(event: MouseEvent, watchTree: XDebuggerTree, fullWidth: Boolean): Boolean {
      val bounds = watchTree.getRowBounds(watchTree.leadSelectionRow) ?: return false
      if (fullWidth) {
        bounds.x = 0
      }
      bounds.width = watchTree.width
      return bounds.contains(event.point)
    }
  }
}
