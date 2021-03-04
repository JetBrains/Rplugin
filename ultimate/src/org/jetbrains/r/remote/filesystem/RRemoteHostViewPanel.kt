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
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ssh.interaction.ConnectionOwnerFactory
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeSpeedSearch
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.plugins.webDeployment.WebDeploymentTopics
import com.jetbrains.plugins.webDeployment.config.Deployable
import com.jetbrains.plugins.webDeployment.config.FileTransferConfig
import com.jetbrains.plugins.webDeployment.config.PublishConfig
import com.jetbrains.plugins.webDeployment.connections.RemoteConnectionManager
import com.jetbrains.plugins.webDeployment.ui.DefaultServerTreeContentListener
import com.jetbrains.plugins.webDeployment.ui.remotebrowser.ServerTree
import com.jetbrains.plugins.webDeployment.ui.remotebrowser.ServerTreeNode
import com.jetbrains.plugins.webDeployment.ui.remotebrowser.ServerTreeViewOptions
import com.jetbrains.plugins.webDeployment.ui.remotebrowser.WebServerTreeBuilder
import org.apache.commons.vfs2.FileName
import org.apache.commons.vfs2.FileType
import org.jetbrains.plugins.notebooks.jupyter.remote.vfs.JupyterVFileCreatedOnServerEvent
import org.jetbrains.plugins.notebooks.jupyter.remote.vfs.JupyterVFileDeletedOnServerEvent
import org.jetbrains.r.remote.RRemoteBundle
import org.jetbrains.r.remote.RRemoteInterpreterImpl
import org.jetbrains.r.util.RPathUtil
import java.awt.event.MouseEvent
import javax.swing.tree.TreePath

class RRemoteHostViewPanel(val project: Project, private val interpreter: RRemoteInterpreterImpl) : SimpleToolWindowPanel(true), Disposable {
  val rootFolder get() = interpreter.basePath
  val remoteHost get() = interpreter.remoteHost
  private lateinit var serverConfig: Deployable
  private lateinit var root: ServerTreeNode
  lateinit var serverTree: ServerTree
    private set
  private lateinit var treeBuilder: WebServerTreeBuilder
  private var currentDisposable: Disposable? = null

  private val showHiddenFilesAction = MyToggleAction(RRemoteBundle.message("remote.host.view.show.hidden.files"), false)
  private val showSizeAction = MyToggleAction(RRemoteBundle.message("remote.host.view.show.size"), false)
  private val showTimestampAction = MyToggleAction(RRemoteBundle.message("remote.host.view.show.date"), false)
  private val showPermissionsAction = MyToggleAction(RRemoteBundle.message("remote.host.view.show.permissions"), false)

  init {
    rebuild()
  }

  fun rebuild() {
    currentDisposable?.let { Disposer.dispose(it) }
    val currentDisposable = Disposer.newDisposable(this, "RRemoteHostViewPanel.currentDisposable")
      .also { this.currentDisposable = it }

    serverConfig = remoteHost.config.clone().also {
      it.server.fileTransferConfig.rootFolder = rootFolder
    }

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

    treeBuilder = WebServerTreeBuilder.createInstance(root, serverTree).also { Disposer.register(currentDisposable, it) }
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
        RRemoteFilesystemUtil.editRemoteFile(project, remoteHost, RPathUtil.join(rootFolder, node.path.path))
      }
    }.installOn(serverTree)

    setContent(ScrollPaneFactory.createScrollPane(serverTree))
    toolbar = ActionManager.getInstance().createActionToolbar(
      "RRemoteHostToolWindow", DefaultActionGroup(createActions()), true).component

    ApplicationManager.getApplication().messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
      override fun after(events: MutableList<out VFileEvent>) {
        val any = events.any { event ->
          val file = event.file ?: return@any false
          if (RRemoteVFS.getHostAndPath(file)?.first != remoteHost) return@any false
          event is JupyterVFileCreatedOnServerEvent || event is JupyterVFileDeletedOnServerEvent
        }
        if (any) refresh()
      }
    })
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
    val fileSystemManager = RemoteConnectionManager.getInstance().manager
    fileSystemManager.filesCache.clear(fileObject.fileSystem)
    treeBuilder.refreshRoot()
  }

  private fun createShowActions(): AnAction {
    return DefaultActionGroup(RRemoteBundle.message("remote.host.view.show.group.title"), listOf(
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