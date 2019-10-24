/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.template

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.platform.ProjectGeneratorPeer
import org.jetbrains.r.R_LOGO_16
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.projectGenerator.step.RProjectSettingsStep
import org.jetbrains.r.settings.RSettings
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

abstract class RProjectGenerator : DirectoryProjectGeneratorBase<RProjectSettings>(), CustomStepProjectGenerator<RProjectSettings> {

  open val requiredPackageList = false

  interface SettingsListener {
    fun stateChanged()
  }

  private var callback = AbstractNewProjectStep.AbstractCallback<RProjectSettings>()
  protected var rProjectSettings: RProjectSettings = RProjectSettings()
  private var settingsListener: SettingsListener? = null

  override fun getLogo(): Icon? {
    return R_LOGO_16
  }

  open fun validateSettings(): List<ValidationInfo> {
    return emptyList()
  }

  open fun getSettingsPanel(): JComponent? {
    return null
  }

  override fun createStep(projectGenerator: DirectoryProjectGenerator<RProjectSettings>,
                          callback: AbstractNewProjectStep.AbstractCallback<RProjectSettings>): RProjectSettingsStep {
    return RProjectSettingsStep(rProjectSettings, projectGenerator, this.callback)
  }

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
