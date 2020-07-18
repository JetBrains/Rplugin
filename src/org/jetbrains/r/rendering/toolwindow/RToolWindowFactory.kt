/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.toolwindow

import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.webcore.packaging.PackagesNotificationPanel
import icons.RIcons
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.build.RPackageBuildUtil
import org.jetbrains.r.packages.build.ui.RPackageBuildToolWindow
import org.jetbrains.r.packages.remote.RPackageManagementService
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.run.graphics.ui.RGraphicsToolWindow
import org.jetbrains.r.run.ui.RNonStealingToolWindowInvoker
import org.jetbrains.r.run.viewer.ui.RViewerToolWindow
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JComponent

class RToolWindowFactory : ToolWindowFactory, DumbAware  {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager
    val factory = contentManager.factory
    for (content in createNestedToolWindows(project, factory)) {
      contentManager.addContent(content)
    }
  }

  private fun createNestedToolWindows(project: Project, factory: ContentFactory): List<Content> {
    val holders = mutableListOf(
      Triple(createPackages(project), PACKAGES, RIcons.ToolWindow.RPackages),
      Triple(RGraphicsToolWindow(project), PLOTS, RIcons.ToolWindow.RGraph),
      Triple(createHelp(project), HELP, AllIcons.Toolwindows.Documentation),
      Triple(RViewerToolWindow(project), VIEWER, RIcons.ToolWindow.RHtml)
    )
    if (RPackageBuildUtil.isPackage(project)) {
      val holder = Triple(RPackageBuildToolWindow(project), BUILD, AllIcons.Toolwindows.ToolWindowBuild)
      holders.add(holder)
    }
    return holders.map { (component, title, icon) ->
      factory.createContent(component, title, false).withIcon(icon)
    }
  }

  private fun createHelp(project: Project): JComponent {
    val component = RDocumentationComponent(project)
    component.setText(HELP_DEFAULT_TEXT, null, null)
    return component
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
    const val HELP = "Documentation"
    const val BUILD = "Build"
    const val ID = "R Tools"

    fun showDocumentation(psiElement: PsiElement) {
      val project = psiElement.project
      DocumentationManager.getInstance(project).fetchDocInfo(psiElement, getDocumentationComponent(project))
      RNonStealingToolWindowInvoker(project, HELP).showWindow()
    }

    fun findContent(project: Project, displayName: String): Content =
      ToolWindowManager.getInstance(project).getToolWindow(ID)!!.contentManager.findContent(displayName)

    fun showFile(project: Project, path: String): Promise<Unit> {
      return getViewerComponent(project).refresh(path).also {
        RNonStealingToolWindowInvoker(project, VIEWER).showWindow()
      }
    }

    fun refreshPackagePanel(project: Project) {
      runInEdt {
        val panel = ToolWindowManager.getInstance(project).getToolWindow(ID)?.contentManager?.findContent(PACKAGES)?.component
        (panel as? RInstalledPackagesPanel)?.scheduleRefresh()
      }
    }

    private fun getDocumentationComponent(project: Project): DocumentationComponent =
      findContent(project, HELP).component as DocumentationComponent

    private fun getViewerComponent(project: Project): RViewerToolWindow =
      findContent(project, VIEWER).component as RViewerToolWindow
  }
}

private fun Content.withIcon(icon: Icon?): Content = apply {
  this.icon = icon
  putUserData(ToolWindow.SHOW_CONTENT_ICON, java.lang.Boolean.TRUE)
}


private const val HELP_DEFAULT_TEXT = """
  <html>
  <body>
  <p><span></span></p>
  <p><span>With the plugin for the </span>
  <span><a href="https://www.r-project.org/">R language</a></span><span>,</span><span
     >&nbsp;you can perform various statistical computing and enjoy your favorite features of the integrated development environment:</span>
  </p>
  <ul>
    <li><span>Coding assistance:</span>
      <ul>
        <li><span>Error and syntax</span><span>&nbsp;highlighting</span><span>.</span></li>
        <li><span>Code completion</span><span>.</span></li>
        <li><span>Intention actions and quick fixes.</span></li>
      </ul>
     </li>
  </ul>
  <ul>
    <li><span>Smart editing and auto-saving changes in your R files. Supported formats:</span>
      <ul>
        <li><span>R Script</span></li>
        <li><span>R Markdown</span></li>
      </ul>
    </li>
  </ul>
  <ul>
    <li><span>Previewing data in the graphic and tabular forms:</span>
      <ul>
        <li><span>R Graphics viewer</span></li>
        <li><span>Table View</span></li>
        <li><span>R HTML viewer</span></li>
        <li><span>R Markdown preview</span></li>
      </ul>
     </li>
  </ul>
  <ul>
    <li><span>Running and debugging R scripts with the live variables view.</span></li>
    <li><span>Managing R packages; ability to create your own R packages.</span></li>
  </ul>
  <p><span>Find a bug? Please, file an issue <a href="https://youtrack.jetbrains.com/issues/R">here</a></span>.</p>
  <p><span>For more details, see </span><span><a href="https://www.jetbrains.com/help/pycharm/2019.3/r-plugin-support.html">PyCharm web help</a></span><span>.</span></p>
  <br>
  <p>
  This plugin comes with ABSOLUTELY NO WARRANTY.</p>
  <p>
  This is free software, and you are welcome to redistribute it under certain conditions.
  </p>
  <br>
  <p>
  Please note the plugin distribution contains Rkernel program covered by
  <a href="https://www.gnu.org/licenses/gpl-3.0.en.html">GPL-3</a>/<a href="https://www.gnu.org/licenses/agpl-3.0.en.html">AGPL-3</a> licenses.
  </p>
  <br>
  <p>
  You can find the source code in the following repositories:
  <ul>
      <li><a href="https://github.com/JetBrains/Rplugin">Rplugin</a></li>
      <li><a href="https://github.com/JetBrains/Rkernel-proto">Rkernel-proto</a></li>
      <li><a href="https://github.com/JetBrains/Rkernel">Rkernel</a></li>
  </ul>
  </p>
  </body>

  </html>
"""