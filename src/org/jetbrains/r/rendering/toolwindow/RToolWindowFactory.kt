/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.webcore.packaging.PackagesNotificationPanel
import org.jetbrains.r.R_GRAPH
import org.jetbrains.r.R_HTML
import org.jetbrains.r.R_PACKAGES
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.remote.RPackageManagementService
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.run.graphics.ui.RGraphicsToolWindow
import org.jetbrains.r.run.viewer.ui.RViewerToolWindow
import javax.swing.BorderFactory

class RToolWindowFactory : ToolWindowFactory, DumbAware  {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager
    val factory = contentManager.factory
    listOf(
      factory.createContent(RGraphicsToolWindow(project), PLOTS, false).apply {
        icon = R_GRAPH
        putUserData(ToolWindow.SHOW_CONTENT_ICON, java.lang.Boolean.TRUE)
      },
      factory.createContent(createPackages(project), PACKAGES, false).apply {
        icon = R_PACKAGES
        putUserData(ToolWindow.SHOW_CONTENT_ICON, java.lang.Boolean.TRUE)
      },
      factory.createContent(RViewerToolWindow(project), VIEWER, false).apply {
        icon = R_HTML
        putUserData(ToolWindow.SHOW_CONTENT_ICON, java.lang.Boolean.TRUE)
      }
    ).forEach { contentManager.addContent(it) }
  }

  override fun shouldBeAvailable(project: Project) = RConsoleManager.getInstance(project).initialized

  private fun createPackages(project: Project): RInstalledPackagesPanel {
    val notificationPanel = PackagesNotificationPanel()
    val packagesPanel = RInstalledPackagesPanel(project, notificationPanel)
    packagesPanel.border = BorderFactory.createEmptyBorder(4, 0, 0, 0)
    packagesPanel.updatePackages(RPackageManagementService(project, packagesPanel))
    return packagesPanel
  }

  companion object {
    const val PLOTS = "Plots"
    const val VIEWER = "Viewer"
    const val PACKAGES = "Packages"
    const val ID = "R Tools"

    fun findContent(project: Project, displayName: String): Content =
      ToolWindowManager.getInstance(project).getToolWindow(ID).contentManager.findContent(displayName)
  }
}
