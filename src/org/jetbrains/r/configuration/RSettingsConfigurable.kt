/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.execution.ExecuteExpressionUtils.getSynchronously
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.packages.remote.RepoProvider
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RSettings
import java.awt.*
import javax.swing.*

class RSettingsConfigurable(private val project: Project) : UnnamedConfigurable {
  private val settings = RSettings.getInstance(project)
  private val interpreterPanel = RManageInterpreterPanel(RBundle.message("project.settings.interpreter.label"), false, null)
  private val loadWorkspaceCheckBox = JCheckBox(RBundle.message("project.settings.load.workspace.checkbox"))
  private val saveWorkspaceCheckBox = JCheckBox(RBundle.message("project.settings.save.workspace.checkbox"))
  private val component = JPanel()

  init {
    fun createConstraints(gridY: Int, weightY: Double = 0.0) = GridBagConstraints().apply {
      gridx = 0
      gridy = gridY
      anchor = GridBagConstraints.NORTHWEST
      weightx = 1.0
      weighty = weightY
      fill = GridBagConstraints.HORIZONTAL
    }
    reset()
    component.layout = GridBagLayout()
    component.add(interpreterPanel.component, createConstraints(0))
    component.add(loadWorkspaceCheckBox, createConstraints(1))
    component.add(saveWorkspaceCheckBox, createConstraints(2, 1.0))
  }

  override fun isModified(): Boolean {
    return interpreterPanel.isModified() ||
           loadWorkspaceCheckBox.isSelected != settings.loadWorkspace ||
           saveWorkspaceCheckBox.isSelected != settings.saveWorkspace
  }

  override fun reset() {
    fun String.findAmong(existing: List<RInterpreterInfo>): RInterpreterInfo? {
      return if (this.isNotBlank()) existing.find { it.interpreterPath == this } else null
    }

    val existing = getSynchronously(LOADING_INTERPRETERS_TEXT) {
      RInterpreterUtil.suggestAllInterpreters(true)
    }
    val selection = settings.interpreterPath.findAmong(existing)
    interpreterPanel.initialSelection = selection
    interpreterPanel.initialInterpreters = existing
    interpreterPanel.reset()
    loadWorkspaceCheckBox.isSelected = settings.loadWorkspace
    saveWorkspaceCheckBox.isSelected = settings.saveWorkspace
  }

  override fun apply() {
    RInterpreterSettings.setEnabledInterpreters(interpreterPanel.currentInterpreters)
    val path = interpreterPanel.currentSelection?.interpreterPath ?: ""
    val previousPath = settings.interpreterPath
    if (path != previousPath) {
      settings.interpreterPath = path
      RConsoleManager.closeMismatchingConsoles(project, path)
      restartInterpreter()
      RConsoleManager.runConsole(project)
    }
    settings.loadWorkspace = loadWorkspaceCheckBox.isSelected
    if (settings.saveWorkspace != saveWorkspaceCheckBox.isSelected) {
      settings.saveWorkspace = saveWorkspaceCheckBox.isSelected
      RConsoleManager.getInstance(project).consoles.forEach {
        it.rInterop.saveOnExit = settings.saveWorkspace
      }
    }
    reset()
  }

  override fun createComponent(): JComponent {
    return component
  }

  private fun restartInterpreter() {
    RInterpreterManager.getInstance(project).initializeInterpreter(true).onSuccess {
      RepoProvider.getInstance(project).onInterpreterVersionChange()
      ApplicationManager.getApplication().invokeLater {
        getPackagesPanel(project).scheduleRefresh()
      }
    }
  }

  companion object {
    private val LOADING_INTERPRETERS_TEXT = RBundle.message("project.settings.interpreters.loading")

    private fun getPackagesPanel(project: Project) =
      RToolWindowFactory.findContent(project, RToolWindowFactory.PACKAGES).component as RInstalledPackagesPanel
  }
}
