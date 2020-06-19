/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.remote.filesystem

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ssh.SftpChannelException
import com.intellij.util.PathUtilRt
import com.jetbrains.plugins.webDeployment.WDBundle
import com.jetbrains.plugins.webDeployment.actions.WebDeploymentDataKeys
import org.jetbrains.r.remote.RRemoteBundle
import org.jetbrains.r.util.RPathUtil

class RRemoteHostViewNewActionGroup : ActionGroup("New...", true), DumbAware {
  override fun getChildren(event: AnActionEvent?): Array<AnAction> {
    return arrayOf(
      ActionManager.getInstance().getAction("org.jetbrains.r.remote.filesystem.RRemoteHostViewNewAction.NewFile"),
      ActionManager.getInstance().getAction("org.jetbrains.r.remote.filesystem.RRemoteHostViewNewAction.NewRFile"),
      ActionManager.getInstance().getAction("org.jetbrains.r.remote.filesystem.RRemoteHostViewNewAction.NewRMarkdownFile"),
      ActionManager.getInstance().getAction("RemoteHostView.CreateFolder")
    )
  }
}

abstract class RRemoteHostViewNewAction(val actionName: String, private val templateName: String? = null) : DumbAwareAction(actionName) {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isEnabled(e.dataContext)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val hostViewPanel = e.getData(RRemoteHostViewPanel.HOST_VIEW_PANEL_KEY) ?: return
    val remoteHost = hostViewPanel.remoteHost
    val nodes = e.getData(WebDeploymentDataKeys.SERVER_NODES) ?: return
    if (nodes.size != 1) return

    val template = templateName?.let { FileTemplateManager.getInstance(project).getInternalTemplate(it) }
    val extension = template?.extension?.takeIf { it.isNotEmpty() }
    val newFileName = Messages.showInputDialog(
      project, WDBundle.message("prompt.enter.new.file.name"), actionName, Messages.getQuestionIcon(),
      DEFAULT_FILE_NAME + extension?.let { ".$it" }.orEmpty(),
      object : InputValidator {
        override fun checkInput(inputString: String) = canClose(inputString)
        override fun canClose(inputString: String) = !StringUtil.isEmptyOrSpaces(inputString) && !inputString.contains("/")
      }, TextRange(0, DEFAULT_FILE_NAME.length)) ?: return
    val content = template?.getText(FileTemplateManager.getInstance(project).defaultProperties).orEmpty()

    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "$actionName $newFileName") {
      override fun run(indicator: ProgressIndicator) {
        val selectedNode = nodes[0]
        val parentPath = if (selectedNode.isDirectory) {
          selectedNode.path.path
        } else {
          PathUtilRt.getParentPath(selectedNode.path.path)
        }
        val newFilePath = RPathUtil.join(parentPath, newFileName)
        try {
          remoteHost.useSftpChannel { channel ->
            val file = channel.file(newFilePath)
            if (file.exists()) {
              Messages.showWarningDialog(project, WDBundle.message("0.already.exists", newFileName), title)
              return
            }
            file.outputStream(false).use {
              it.write(content.toByteArray())
            }
          }
          RRemoteFilesystemUtil.editRemoteFile(project, remoteHost, newFilePath)
        } catch (e: SftpChannelException) {
          invokeLater {
            Messages.showErrorDialog(
              project, e.message ?: RRemoteBundle.message("remote.host.view.cannot.create.file", newFileName),
              title)
          }
        }
        hostViewPanel.refresh()
      }
    })
  }

  class NewFile : RRemoteHostViewNewAction(RRemoteBundle.message("remote.host.view.new.file"))
  class NewRFile : RRemoteHostViewNewAction(RRemoteBundle.message("remote.host.view.new.r.script"), "R Script")
  class NewRMarkdownFile : RRemoteHostViewNewAction(RRemoteBundle.message("remote.host.view.new.rmarkdown"), "RMarkdown")

  companion object {
    private const val DEFAULT_FILE_NAME = "a"

    private fun isEnabled(dataContext: DataContext): Boolean {
      if (RRemoteHostViewPanel.HOST_VIEW_PANEL_KEY.getData(dataContext) == null) return false
      val nodes = WebDeploymentDataKeys.SERVER_NODES.getData(dataContext)
      return nodes?.size == 1
    }
  }
}
