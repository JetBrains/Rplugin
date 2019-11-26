/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.configuration.RManageInterpreterPanel
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.execution.ExecuteExpressionUtils.getSynchronously
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.packages.RPackageService
import org.jetbrains.r.packages.remote.RepoUtils
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.packages.remote.ui.RPackagesToolWindowFactory
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RSettings
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class RActiveInterpreterConfigurable(private val project: Project) : UnnamedConfigurable {
  private val settings = RSettings.getInstance(project)
  private val panel = RManageInterpreterPanel(RBundle.message("project.settings.interpreter.label"), false, null)
  private val wrapper = JPanel(BorderLayout())

  init {
    reset()
    wrapper.add(panel.component, BorderLayout.NORTH)
  }

  override fun isModified(): Boolean {
    return panel.isModified()
  }

  override fun reset() {
    fun String.findAmong(existing: List<RInterpreterInfo>): RInterpreterInfo? {
      return if (this.isNotBlank()) existing.find { it.interpreterPath == this } else null
    }

    val existing = getSynchronously(LOADING_INTERPRETERS_TEXT) {
      RInterpreterUtil.suggestAllInterpreters(true)
    }
    val selection = settings.interpreterPath.findAmong(existing)
    panel.initialSelection = selection
    panel.initialInterpreters = existing
    panel.reset()
  }

  override fun apply() {
    fun restartInterpreter() {
      fun getPackagesPanel(project: Project): RInstalledPackagesPanel {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(RPackagesToolWindowFactory.ID)
        return toolWindow.contentManager.getContent(0)!!.component as RInstalledPackagesPanel
      }

      RInterpreterManager.getInstance(project).initializeInterpreter(true).onSuccess {
        RPackageService.getInstance(project).enabledRepositoryUrls.clear()
        RepoUtils.resetPackageDetails(project)
        ApplicationManager.getApplication().invokeLater {
          getPackagesPanel(project).scheduleRefresh()
        }
      }
    }

    RInterpreterSettings.setEnabledInterpreters(panel.currentInterpreters)
    val path = panel.currentSelection?.interpreterPath ?: ""
    val previousPath = settings.interpreterPath
    if (path != previousPath) {
      settings.interpreterPath = path
      restartInterpreter()
      RAdditionalActionsDialog { shouldCloseOldConsoles, shouldOpenNewConsole ->
        if (shouldCloseOldConsoles) {
          RConsoleManager.closeMismatchingConsoles(project, path)
        }
        if (shouldOpenNewConsole) {
          RConsoleManager.runConsole(project)
        }
      }.show()
    }
    reset()
  }

  override fun createComponent(): JComponent {
    return wrapper
  }

  companion object {
    private val LOADING_INTERPRETERS_TEXT = RBundle.message("project.settings.interpreters.loading")
  }
}
