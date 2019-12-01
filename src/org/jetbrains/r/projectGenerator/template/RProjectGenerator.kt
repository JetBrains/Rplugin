/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.template

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.*
import org.jetbrains.r.R_LOGO_16
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.projectGenerator.builder.RModuleBuilder
import org.jetbrains.r.projectGenerator.step.RProjectSettingsStep
import org.jetbrains.r.settings.RSettings
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

abstract class RProjectGenerator : DirectoryProjectGeneratorBase<RProjectSettings>(), CustomStepProjectGenerator<RProjectSettings>, ProjectTemplate {

  override fun getIcon(): Icon? = R_LOGO_16

  override fun createModuleBuilder(): ModuleBuilder = RModuleBuilder(this)

  override fun validateSettings(): ValidationInfo? = validateGeneratorSettings().firstOrNull()

  open val requiredPackageList = false

  /**
   * Unique value
   */
  abstract fun getId(): String

  interface SettingsListener {
    fun stateChanged()
  }

  private var callback = AbstractNewProjectStep.AbstractCallback<RProjectSettings>()
  protected var rProjectSettings: RProjectSettings = RProjectSettings()
  private var settingsListener: SettingsListener? = null

  override fun getLogo(): Icon? {
    return R_LOGO_16
  }

  open fun validateGeneratorSettings(): List<ValidationInfo> {
    return emptyList()
  }

  open fun getSettingsPanel(): JComponent? {
    return null
  }

  override fun createStep(projectGenerator: DirectoryProjectGenerator<RProjectSettings>,
                          callback: AbstractNewProjectStep.AbstractCallback<RProjectSettings>): RProjectSettingsStep {
    return RProjectSettingsStep(rProjectSettings, projectGenerator, this.callback)
  }

  fun createModuleStep(moduleStepButtonUpdater: ((Boolean) -> Unit)?): RProjectSettingsStep {
    return RProjectSettingsStep(rProjectSettings, this, this.callback, moduleStepButtonUpdater)
  }

  fun getSettings() = rProjectSettings

  override fun createPeer(): ProjectGeneratorPeer<RProjectSettings> {
    return GeneratorPeerImpl<RProjectSettings>(rProjectSettings, JPanel())
  }

  /**
   * When overwriting this method, <strong>be sure</strong> to call super(): the correct interpreter will be selected only after that
   */
  override fun generateProject(project: Project, baseDir: VirtualFile, rProjectSettings: RProjectSettings, module: Module) {
    if (rProjectSettings.useNewInterpreter) {
      RSettings.getInstance(project).interpreterPath = rProjectSettings.interpreterPath!!
      RInterpreterManager.getInstance(project).initializeInterpreter(true)
    }
  }

  fun addSettingsStateListener(listener: SettingsListener) {
    settingsListener = listener
  }

  fun stateChanged() {
    settingsListener?.stateChanged()
  }
}
