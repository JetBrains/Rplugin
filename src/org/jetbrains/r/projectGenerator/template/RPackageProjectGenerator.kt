/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.template

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.projectGenerator.panel.packageManager.RDefaultPackagePanel
import org.jetbrains.r.projectGenerator.panel.packageManager.*
import java.util.function.Consumer
import javax.swing.JComponent

class RPackageProjectGenerator : RProjectGenerator() {
  override fun getName(): String {
    return RBundle.message("project.generator.package.name")
  }

  override fun getDescription(): String? {
    return RBundle.message("project.generator.package.description")
  }

  override fun getId():String  {
    return "R_PACKAGE"
  }

  override val requiredPackageList = true
  private var settingsPanel: RPackageManagerGroupPanel? = null

  override fun getSettingsPanel(): JComponent? {
    val defaultPanel = RDefaultPackagePanel(rProjectSettings)
    val panels = listOf(defaultPanel, RPackratPanel(rProjectSettings), RRcppPanel(rProjectSettings),
                        RRcppArmadilloPanel(rProjectSettings), RRcppEigenPanel(rProjectSettings), RDevtoolsPanel(rProjectSettings))
    settingsPanel = RPackageManagerGroupPanel("Choose package manager", getLogo(), rProjectSettings, panels, defaultPanel).apply {
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
}