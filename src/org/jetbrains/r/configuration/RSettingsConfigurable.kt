/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.execution.ExecuteExpressionUtils.getSynchronously
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.packages.remote.RepoProvider
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RSettings
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

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
    fun RInterpreterLocation.findAmong(existing: List<RInterpreterInfo>): RInterpreterInfo? {
      return existing.find { it.interpreterLocation == this }
    }

    val existing = getSynchronously(LOADING_INTERPRETERS_TEXT) {
      RInterpreterUtil.suggestAllInterpreters(true)
    }
    val selection = settings.interpreterLocation?.findAmong(existing)
    interpreterPanel.initialSelection = selection
    interpreterPanel.initialInterpreters = existing
    interpreterPanel.reset()
    loadWorkspaceCheckBox.isSelected = settings.loadWorkspace
    saveWorkspaceCheckBox.isSelected = settings.saveWorkspace
  }

  override fun apply() {
    RInterpreterSettings.setEnabledInterpreters(interpreterPanel.currentInterpreters)
    val location = interpreterPanel.currentSelection?.interpreterLocation
    val previousLocation = settings.interpreterLocation
    if (location != previousLocation) {
      settings.interpreterLocation = location
      onInterpreterLocationChanged(location)
    }
    settings.loadWorkspace = loadWorkspaceCheckBox.isSelected
    if (settings.saveWorkspace != saveWorkspaceCheckBox.isSelected) {
      settings.saveWorkspace = saveWorkspaceCheckBox.isSelected
      onSaveWorkspaceChanged()
    }
    reset()
  }

  override fun createComponent(): JComponent {
    return component
  }

  private fun onSaveWorkspaceChanged() {
    if (project.isDefault) return
    RConsoleManager.getInstance(project).consoles.forEach {
      it.rInterop.saveOnExit = settings.saveWorkspace
    }
  }

  private fun onInterpreterLocationChanged(location: RInterpreterLocation?) {
    if (project.isDefault) return
    restartInterpreter()
    RConsoleManager.runConsole(project).onSuccess {
      runInEdt {
        RConsoleManager.closeMismatchingConsoles(project, location)
      }
    }
  }

  private fun restartInterpreter() {
    RInterpreterManager.getInterpreterAsync(project, true).onSuccess {
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
