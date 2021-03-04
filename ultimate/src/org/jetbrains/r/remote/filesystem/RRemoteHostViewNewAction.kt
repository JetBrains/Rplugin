/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.remote.filesystem

import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.util.PathUtilRt
import com.jetbrains.plugins.webDeployment.WDBundle
import com.jetbrains.plugins.webDeployment.actions.WebDeploymentDataKeys
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.util.RPathUtil

class RRemoteHostViewNewActionGroup : ActionGroup(WDBundle.message("new.remote.item.new"), true), DumbAware {
  override fun getChildren(event: AnActionEvent?): Array<AnAction> {
    return arrayOf(
      NewActionWrapper(ActionManager.getInstance().getAction("NewFile")),
      NewActionWrapper(ActionManager.getInstance().getAction("NewDir")),
      Separator(),
      NewActionWrapper(ActionManager.getInstance().getAction("NewRScriptAction")),
      NewActionWrapper(ActionManager.getInstance().getAction("NewRMarkdownAction"))
    )
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project != null && e.getData(RRemoteHostViewPanel.HOST_VIEW_PANEL_KEY) != null &&
                               e.getData(WebDeploymentDataKeys.SERVER_NODES)?.size == 1
  }
}

private class NewActionWrapper(private val action: AnAction) :  AnAction(action.templateText, action.templatePresentation.description,
                                                                         action.templatePresentation.icon) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val hostViewPanel = e.getData(RRemoteHostViewPanel.HOST_VIEW_PANEL_KEY) ?: return
    val remoteHost = hostViewPanel.remoteHost
    val nodes = e.getData(WebDeploymentDataKeys.SERVER_NODES)?.takeIf { it.size == 1 } ?: return
    val selectedNode = nodes[0]
    val selectedPath = RPathUtil.join(hostViewPanel.rootFolder, selectedNode.path.path)
    val parentPath = if (selectedNode.isDirectory) {
      RPathUtil.join(selectedPath)
    } else {
      PathUtilRt.getParentPath(selectedPath)
    }

    runAsync {
      val virtualFile = RRemoteVFS.instance.findFileByPath(remoteHost, parentPath) ?: return@runAsync
      invokeLater {
        val psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile) ?: return@invokeLater
        val ideView = object : IdeView {
          override fun getDirectories() = arrayOf(psiDirectory)
          override fun getOrChooseDirectory() = psiDirectory
        }
        action.actionPerformed(createEvent(e, ideView))
      }
    }
  }

  override fun update(e: AnActionEvent) {
    // Dummy IdeView that makes action.update happy.
    // We can't use IdeView that returns actual selection here because getting PsiDirectory from selection is a blocking operation.
    val ideView = object : IdeView {
      override fun getOrChooseDirectory(): PsiDirectory? {
        val project = e.project
        val projectDir = project?.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) } ?: return null
        return PsiManager.getInstance(project).findDirectory(projectDir)
      }

      override fun getDirectories() = orChooseDirectory?.let { arrayOf(it) } ?: emptyArray()
    }
    action.update(createEvent(e, ideView))
  }

  private fun createEvent(e: AnActionEvent, ideView: IdeView? = null): AnActionEvent {
    val dataContext = SimpleDataContext.getSimpleContext(mapOf(LangDataKeys.IDE_VIEW.name to ideView), e.dataContext)
    return AnActionEvent.createFromInputEvent(e.inputEvent, e.place, e.presentation, dataContext)
  }
}
