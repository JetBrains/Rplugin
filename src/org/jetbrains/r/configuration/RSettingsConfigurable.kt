/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.layout.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.execution.ExecuteExpressionUtils.getSynchronously
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.packages.remote.RepoProvider
import org.jetbrains.r.packages.remote.ui.RInstalledPackagesPanel
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RInterpreterSettingsProvider
import org.jetbrains.r.settings.RSettings
import javax.swing.JCheckBox
import javax.swing.JComponent

class RSettingsConfigurable(private val project: Project) : UnnamedConfigurable {
  private val settings = RSettings.getInstance(project)
  private val interpreterPanel = RManageInterpreterPanel(RBundle.message("project.settings.interpreter.label"), false, null)
  private val loadWorkspaceCheckBox = JCheckBox(RBundle.message("project.settings.load.workspace.checkbox"))
  private val saveWorkspaceCheckBox = JCheckBox(RBundle.message("project.settings.save.workspace.checkbox"))
  private val component: JComponent
  private val extensionConfigurables = RInterpreterSettingsProvider.getProviders().mapNotNull { it.createSettingsConfigurable(project) }

  init {
    component = panel(LCFlags.fillX) {
      row { component(interpreterPanel.component).constraints(growX) }
      row { component(loadWorkspaceCheckBox) }
      row { component(saveWorkspaceCheckBox) }
      extensionConfigurables
        .mapNotNull { it.createComponent() }
        .forEach { newComponent ->
          row { component(newComponent).constraints(growX) }
        }
    }
  }

  override fun isModified(): Boolean {
    return interpreterPanel.isModified() ||
           loadWorkspaceCheckBox.isSelected != settings.loadWorkspace ||
           saveWorkspaceCheckBox.isSelected != settings.saveWorkspace ||
           extensionConfigurables.any { it.isModified }
  }

  override fun reset() {
    fun RInterpreterLocation.findAmong(existing: List<RInterpreterInfo>): RInterpreterInfo? {
      return existing.find { it.interpreterLocation == this }
    }

    extensionConfigurables.forEach { it.reset() }
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
    extensionConfigurables.forEach { it.apply() }
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
    RConsoleManager.getInstance(project).currentConsoleAsync.onSuccess {
      runInEdt {
        RConsoleManager.closeMismatchingConsoles(project, location)
        RConsoleToolWindowFactory.getRConsoleToolWindows(project)?.show {}
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
