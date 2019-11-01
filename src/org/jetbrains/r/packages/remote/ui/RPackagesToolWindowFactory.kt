// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.webcore.packaging.PackagesNotificationPanel
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.remote.RPackageManagementService
import javax.swing.BorderFactory

class RPackagesToolWindowFactory : ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val notificationPanel = PackagesNotificationPanel()
    val packagesPanel = RInstalledPackagesPanel(project, notificationPanel)
    packagesPanel.border = BorderFactory.createEmptyBorder(4, 0, 0, 0)
    packagesPanel.updatePackages(RPackageManagementService(project, packagesPanel))
    val contentFactory = ContentFactory.SERVICE.getInstance()
    val content = contentFactory.createContent(packagesPanel, "", false)
    toolWindow.contentManager.addContent(content)
  }

  override fun shouldBeAvailable(project: Project) = RConsoleManager.getInstance(project).initialized

  companion object {
    const val ID = "R Packages"
  }
}
