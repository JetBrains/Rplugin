/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote.filesystem

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import org.jetbrains.r.remote.host.RRemoteHost

class RRemoteHostViewManager(private val project: Project) {
  private val contents = mutableMapOf<RRemoteHost, Content>()

  fun addRemoteHost(remoteHost: RRemoteHost) {
    invokeLater {
      if (remoteHost in contents) return@invokeLater
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(
        RRemoteHostToolWindowFactory.ID) ?: return@invokeLater
      val toolWindowPanel = RRemoteHostViewPanel(project, remoteHost)
      val content = ContentFactory.SERVICE.getInstance().createContent(toolWindowPanel, remoteHost.presentableName, true)
      Disposer.register(content, toolWindowPanel)
      contents[remoteHost] = content
      toolWindow.contentManager.addContent(content)
      toolWindow.isAvailable = true
    }
  }

  fun removeRemoteHost(remoteHost: RRemoteHost) {
    invokeLater {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(
        RRemoteHostToolWindowFactory.ID) ?: return@invokeLater
      val content = contents.remove(remoteHost) ?: return@invokeLater
      toolWindow.contentManager.removeContent(content, true)
      toolWindow.isAvailable = toolWindow.contentManager.contentCount > 0
    }
  }

  companion object {
    fun getInstance(project: Project) = project.service<RRemoteHostViewManager>()
  }
}

class RRemoteHostToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun init(toolWindow: ToolWindow) {
    toolWindow.isAvailable = false
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
  }

  companion object {
    const val ID = "R Remote Host"
  }
}
