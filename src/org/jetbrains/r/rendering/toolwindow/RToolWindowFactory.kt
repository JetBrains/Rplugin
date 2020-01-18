/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.toolwindow

import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.psi.PsiElement
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.webcore.packaging.PackagesNotificationPanel
import org.jetbrains.r.R_GRAPH
import org.jetbrains.r.R_HTML
import org.jetbrains.r.R_PACKAGES
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.remote.RPackageManagementService
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.run.graphics.ui.RGraphicsToolWindow
import org.jetbrains.r.run.ui.RNonStealingToolWindowInvoker
import org.jetbrains.r.run.viewer.ui.RViewerToolWindow
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JComponent

class RToolWindowFactory : ToolWindowFactory, DumbAware  {
  private val ELDER = Key.create<JComponent>("org.jetbrains.r.rendering.toolwindow.elder")

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager
    val factory = contentManager.factory
    listOf(
      factory.createContent(RGraphicsToolWindow(project), PLOTS, false).withIcon(R_GRAPH),
      factory.createContent(createPackages(project), PACKAGES, false).withIcon(R_PACKAGES),
      factory.createContent(createHelp(project), HELP, false).withIcon(AllIcons.Windows.Help),
      factory.createContent(RViewerToolWindow(project), VIEWER, false).withIcon(R_HTML)
    ).forEach { contentManager.addContent(it) }
    project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
      override fun stateChanged() {
        handleProjectViewStates(toolWindow, contentManager, project)
      }
    })
    if (showFilesInRTools()) {
      borrowFiles(project, contentManager)
    }
  }

  private fun createHelp(project: Project): JComponent {
    val manager = DocumentationManager.getInstance(project)
    val component = DocumentationComponent(manager)
    Disposer.register(project, component)
    return component
  }

  private fun handleProjectViewStates(toolWindow: ToolWindow,
                                      contentManager: ContentManager,
                                      project: Project) {
    val showFilesInRTools = showFilesInRTools()
    if ((!toolWindow.isVisible || !showFilesInRTools) && contentManager.findContent(FILES) != null) {
      returnFiles(project, contentManager)
    }
    if (showFilesInRTools && toolWindow.isVisible && contentManager.findContent(FILES) != null && ToolWindowManager.getInstance(project).getToolWindow(
        "Project")?.isVisible == true) {
      toolWindow.hide { }
    }
    if (showFilesInRTools && toolWindow.isVisible && contentManager.findContent(FILES) == null) {
      borrowFiles(project, contentManager)
    }
  }

  private fun showFilesInRTools() = Registry.`is`("r.ui.showFilesInRTools", false)

  private fun borrowFiles(project: Project, contentManager: ContentManager) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Project")
    val component = toolWindow.component
    project.putUserData(ELDER, component.parent.parent.parent.parent as JComponent)
    val content = contentManager.factory.createContent(component.parent.parent.parent as JComponent, FILES, false)
                                        .withIcon(AllIcons.Toolwindows.ToolWindowProject)
    contentManager.addContent(content, 0)
    contentManager.setSelectedContent(content)
    toolWindow.hide {}
  }

  private fun returnFiles(project: Project, contentManager: ContentManager) {
    val elder = project.getUserData(ELDER)!!
    val content = contentManager.findContent(FILES)
    elder.add(content.component)
    contentManager.removeContent(content, false)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Project")
    toolWindow.setAvailable(true, null)
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
    const val FILES = "Files"
    const val HELP = "Help"
    const val ID = "R Tools"

    fun showDocumentation(psiElement: PsiElement) {
      val project = psiElement.project
      DocumentationManager.getInstance(project).fetchDocInfo(psiElement, getDocumentationComponent(project))
      RNonStealingToolWindowInvoker(project, HELP).showWindow()
    }

    fun findContent(project: Project, displayName: String): Content =
      ToolWindowManager.getInstance(project).getToolWindow(ID).contentManager.findContent(displayName)

    private fun getDocumentationComponent(project: Project): DocumentationComponent =
      findContent(project, HELP).component as DocumentationComponent
  }
}

private fun Content.withIcon(icon: Icon?): Content = apply {
  this.icon = icon
  putUserData(ToolWindow.SHOW_CONTENT_ICON, java.lang.Boolean.TRUE)
}
