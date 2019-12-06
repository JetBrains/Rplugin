/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.ui.*
import com.intellij.ui.border.CustomLineBorder
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
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.rinterop.RVar
import org.jetbrains.r.run.debug.stack.RXStackFrame
import java.awt.BorderLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.event.TreeSelectionListener


internal class RXVariablesView(project: Project, val console: RConsoleView)
  : XVariablesViewBase(project, RDebuggerEditorsProvider, null), XWatchesView, DataProvider {
  var stackFrame: RXStackFrame? = null
    set(frame) {
      field = frame
      AppUIUtil.invokeLaterIfProjectAlive(getTree().getProject()) {
        if (frame == null) {
          clear()
        } else {
          buildTreeAndRestoreState(frame)
        }
      }
    }

  private var rootNode: WatchesRootNode? = null

  init {
    DataManager.registerDataProvider(panel, this)
    installActions()
    installEditListeners()
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
    actions.addAction(object : AnAction("Clear global environment", null, AllIcons.Actions.GC) {
      override fun actionPerformed(e: AnActionEvent) = console.rInterop.executeTask {
        console.rInterop.clearEnvironment(console.rInterop.globalEnvRef)
        console.executeActionHandler.fireCommandExecuted()
        console.debugger.refreshVariableView()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !console.isRunningCommand
      }
    })
    val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actions, false) as ActionToolbarImpl
    toolbar.border = CustomLineBorder(CaptionPanel.CNT_ACTIVE_BORDER_COLOR, 0, 1, 0, 0)
    toolbar.setTargetComponent(tree)
    getPanel().add(toolbar.getComponent(), BorderLayout.WEST)
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
