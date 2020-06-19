/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote.filesystem

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ssh.interaction.ConnectionOwnerFactory
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeSpeedSearch
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.plugins.webDeployment.PublishUtils
import com.jetbrains.plugins.webDeployment.WebDeploymentTopics
import com.jetbrains.plugins.webDeployment.config.FileTransferConfig
import com.jetbrains.plugins.webDeployment.config.PublishConfig
import com.jetbrains.plugins.webDeployment.ui.DefaultServerTreeContentListener
import com.jetbrains.plugins.webDeployment.ui.remotebrowser.ServerTree
import com.jetbrains.plugins.webDeployment.ui.remotebrowser.ServerTreeNode
import com.jetbrains.plugins.webDeployment.ui.remotebrowser.ServerTreeViewOptions
import com.jetbrains.plugins.webDeployment.ui.remotebrowser.WebServerTreeBuilder
import org.apache.commons.vfs2.FileName
import org.apache.commons.vfs2.FileType
import org.jetbrains.r.remote.host.RRemoteHost
import java.awt.event.MouseEvent
import javax.swing.tree.TreePath

class RRemoteHostViewPanel(val project: Project, val remoteHost: RRemoteHost) : SimpleToolWindowPanel(true), Disposable {
  private val serverConfig = remoteHost.config
  private val root: ServerTreeNode
  val serverTree: ServerTree
  private val treeBuilder: WebServerTreeBuilder

  private val showHiddenFilesAction = MyToggleAction("Show Hidden Files", false)
  private val showSizeAction = MyToggleAction("Show Size", false)
  private val showTimestampAction = MyToggleAction("Show Date", false)
  private val showPermissionsAction = MyToggleAction("Show Permissions", false)

  init {
    val viewOptions = object : ServerTreeViewOptions {
      override fun isShowPermissionsAsNumber() = false
      override fun isHighlightSymlinks() = true
      override fun isShowSize() = showSizeAction.value
      override fun isHighlightMappings() = true
      override fun isShowPermissions() = showPermissionsAction.value
      override fun isShowTimestamp() = showTimestampAction.value

      override fun isHidden(name: FileName?): Boolean {
        if (name == null || showHiddenFilesAction.value) return false
        return name.baseName.startsWith(".")
      }
    }
    root = ServerTreeNode(
      project, ConnectionOwnerFactory.createConnectionOwnerWithDialogMessages(project),
      serverConfig, PublishConfig.getInstance(project), false, FileTransferConfig.Origin.Default,
      viewOptions)
    serverTree = object : ServerTree(project, serverConfig, root) {
      override fun getData(dataId: String): Any? {
        if (HOST_VIEW_PANEL_KEY.`is`(dataId)) {
          return this@RRemoteHostViewPanel
        }
        return super.getData(dataId)
      }
    }

    treeBuilder = WebServerTreeBuilder.createInstance(root, serverTree).also { Disposer.register(this, it) }
    TreeSpeedSearch(serverTree)
    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(WebDeploymentTopics.SERVER_TREE, DefaultServerTreeContentListener(serverTree, treeBuilder, serverConfig))

    PopupHandler.installPopupHandler(serverTree, "org.jetbrains.r.remote.filesystem.RRemoteHostViewPopupMenu",
                                     ActionPlaces.REMOTE_HOST_VIEW_POPUP)

    object : EditSourceOnDoubleClickHandler.TreeMouseListener(serverTree, null)  {
      override fun processDoubleClick(e: MouseEvent, dataContext: DataContext, treePath: TreePath) {
        val node = TreeUtil.getLastUserObject(ServerTreeNode::class.java, treePath) ?: return
        val file = node.fileObject ?: return
        if (file.type != FileType.FILE) return
        RRemoteFilesystemUtil.editRemoteFile(project, remoteHost, node.path.path)
      }
    }.installOn(serverTree)

    setContent(ScrollPaneFactory.createScrollPane(serverTree))
    toolbar = ActionManager.getInstance().createActionToolbar(
      "RRemoteHostToolWindow", DefaultActionGroup(createActions()), true).component
  }

  private fun createActions(): List<AnAction> {
    return listOf(
      object : RefreshAction(IdeBundle.message("action.refresh"), IdeBundle.message("action.refresh"),
                             AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) {
          refresh()
        }

        override fun update(e: AnActionEvent) {
          e.presentation.isEnabled = true
        }
      },
      createShowActions()
    )
  }

  fun refresh() {
    val fileObject = root.fileObject ?: return
    val fileSystemManager = PublishUtils.getManager() ?: return
    fileSystemManager.filesCache.clear(fileObject.fileSystem)
    treeBuilder.refreshRoot()
  }

  private fun createShowActions(): AnAction {
    return DefaultActionGroup("Show...", listOf(
      showHiddenFilesAction,
      showSizeAction,
      showTimestampAction,
      showPermissionsAction
    )).also {
      it.templatePresentation.icon = AllIcons.Actions.Show
      it.isPopup = true
    }
  }

  override fun dispose() {
  }

  inner class MyToggleAction(text: String, var value: Boolean = false) : DumbAwareToggleAction(text) {
    override fun isSelected(e: AnActionEvent) = value

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      if (state != value) {
        value = state
        treeBuilder.queueUpdate()
        treeBuilder.refreshRoot()
      }
    }
  }

  companion object {
    val HOST_VIEW_PANEL_KEY = DataKey.create<RRemoteHostViewPanel>("intellij.r.remote.RRemoteHostViewPanel")
  }
}