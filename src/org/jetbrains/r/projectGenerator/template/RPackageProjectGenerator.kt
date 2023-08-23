/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.template

import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.RBundle
import org.jetbrains.r.projectGenerator.panel.packageManager.*
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.JComponent

class RPackageProjectGenerator : RProjectGenerator() {
  override fun getName(): String {
    return RBundle.message("project.generator.package.name")
  }

  override fun getDescription(): String {
    return RBundle.message("project.generator.package.description")
  }

  override fun getId(): String {
    return "R_PACKAGE"
  }

  override val requiredPackageList = true
  override val onlyLocalInterpreters = true
  private var settingsPanel: RPackageManagerGroupPanel? = null

  override fun validate(baseDirPath: String): ValidationResult {
    val packageName = Path.of(baseDirPath).fileName.toString()
    return if (R_PACKAGE_NAME_REGEX.matchEntire(packageName) == null) {
      ValidationResult(RBundle.message("project.setting.incorrect.package.name"))
    }
    else {
      ValidationResult.OK
    }
  }

  override fun getSettingsPanel(): JComponent? {
    val defaultPanel = RDefaultPackagePanel(rProjectSettings)
    val panels = listOf(defaultPanel, RPackratPanel(rProjectSettings), RRcppPanel(rProjectSettings),
                        RRcppArmadilloPanel(rProjectSettings), RRcppEigenPanel(rProjectSettings), RDevtoolsPanel(rProjectSettings))
    settingsPanel = RPackageManagerGroupPanel("Choose package manager", logo, rProjectSettings, panels, defaultPanel).apply {
      addChangeListener(Runnable { stateChanged() })
    }
    return settingsPanel
  }

  override fun generateProject(project: Project, baseDir: VirtualFile, rProjectSettings: RProjectSettings, module: Module) {
    super.generateProject(project, baseDir, rProjectSettings, module)
    settingsPanel!!.generateProject(project, baseDir, module)
  }

  override fun validateGeneratorSettings(): List<ValidationInfo> {
    return settingsPanel?.validateSettings() ?: emptyList()
  }

  fun setErrorAction(action: Consumer<List<ValidationInfo>>) {
    settingsPanel!!.setErrorAction(action)
  }

  companion object {
    private val R_PACKAGE_NAME_REGEX = Regex("[a-zA-Z][a-zA-Z0-9.]*[a-zA-Z0-9]")
  }
}