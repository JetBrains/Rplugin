/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.openapi.options.DslConfigurableBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.interpreter.RInterpreterInfo
import com.intellij.r.psi.interpreter.RInterpreterLocation
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.settings.RInterpreterSettings
import org.jetbrains.r.settings.RInterpreterSettingsProvider
import org.jetbrains.r.settings.RSettings

class RSettingsConfigurable(private val project: Project) : DslConfigurableBase() {
  private val settings = RSettings.getInstance(project)
  private val interpreterPanel = RManageInterpreterPanel(RBundle.message("project.settings.interpreter.label"), false, null)
  private val extensionConfigurables = RInterpreterSettingsProvider.getProviders().mapNotNull { it.createSettingsConfigurable(project) }

  override fun createPanel(): DialogPanel {
    return panel {
      row {
        cell(interpreterPanel.component).align(AlignX.FILL)
      }
      row {
        checkBox(RBundle.message("project.settings.load.workspace.checkbox"))
          .bindSelected(settings::loadWorkspace)
      }
      row {
        checkBox(RBundle.message("project.settings.save.workspace.checkbox"))
          .bindSelected(settings::saveWorkspace)
      }
      row {
        checkBox(RBundle.message("project.settings.disable.rprofile.checkbox"))
          .bindSelected(settings::disableRprofile)
      }
      row {
        checkBox(RBundle.message("project.settings.enable.rstudioapi.checkbox"))
          .bindSelected(settings::rStudioApiEnabled)
      }
      extensionConfigurables
        .mapNotNull { it.createComponent() }
        .forEach { newComponent ->
          row { cell(newComponent).align(AlignX.FILL) }
        }
    }
  }

  override fun isModified(): Boolean {
    return super.isModified() ||
           interpreterPanel.isModified() ||
           extensionConfigurables.any { it.isModified }
  }

  override fun reset() {
    super.reset()
    fun RInterpreterLocation.findAmong(existing: List<RInterpreterInfo>): RInterpreterInfo? {
      return existing.find { it.interpreterLocation == this }
    }

    extensionConfigurables.forEach { it.reset() }
    val existing = runWithModalProgressBlocking(project, RBundle.message("project.settings.interpreters.loading")) {
      RInterpreterUtil.suggestAllInterpreters(true)
    }
    val selection = settings.interpreterLocation?.findAmong(existing)
    interpreterPanel.initialSelection = selection
    interpreterPanel.initialInterpreters = existing
    interpreterPanel.reset()
  }

  override fun apply() {
    extensionConfigurables.forEach { it.apply() }
    RInterpreterSettings.setEnabledInterpreters(interpreterPanel.currentInterpreters)
    val location = interpreterPanel.currentSelection?.interpreterLocation
    val previousLocation = settings.interpreterLocation
    if (location != previousLocation) {
      settings.interpreterLocation = location
      onInterpreterLocationChanged()
    }
    val oldSaveWorkspace = settings.saveWorkspace
    val oldRStudioApiEnabled = settings.rStudioApiEnabled

    super.apply()

    if (settings.saveWorkspace != oldSaveWorkspace) {
      onSaveWorkspaceChanged()
    }
    if (settings.rStudioApiEnabled != oldRStudioApiEnabled) {
      onRStudioApiEnabledChanged(settings.rStudioApiEnabled)
    }
    reset()
  }

  private fun onSaveWorkspaceChanged() {
    if (project.isDefault) return
    RConsoleManager.getInstance(project).consoles.forEach {
      it.rInterop.saveOnExit = settings.saveWorkspace
    }
  }

  private fun onRStudioApiEnabledChanged(value: Boolean) {
    if (project.isDefault) return
    RConsoleManager.getInstance(project).consoles.forEach { rConsoleView ->
      runAsync {
        if (Disposer.isDisposed(rConsoleView)) return@runAsync
        rConsoleView.rInterop.setRStudioApiEnabled(value)
      }
    }
  }

  private fun onInterpreterLocationChanged() {
    if (project.isDefault) return
    RInterpreterManager.restartInterpreter(project)
  }
}
