/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.r.projectGenerator.template.RProjectGenerator
import javax.swing.JComponent
import javax.swing.JPanel

class RGeneratorSettingsWizardStep(private val generator: RProjectGenerator, context: WizardContext) : ModuleWizardStep(), Disposable {
  private var projectSettingsStep: RProjectSettingsStep? = null
  private var panel: JPanel? = null

  private val wizard = context.wizard

  init {
    Disposer.register(context.getDisposable(), this)
  }

  override fun getComponent(): JComponent {
    if (projectSettingsStep == null) {
      projectSettingsStep = generator.createModuleStep {
        wizard.updateButtons(false, it, false)
      }
      panel = projectSettingsStep!!.createPanel()
    }

    return panel!!
  }

  override fun updateDataModel() {}

  override fun getPreferredFocusedComponent(): JComponent? {
    // This is a terrible crutch for manipulating the activity of the "Next" button
    checkValid()
    return super.getPreferredFocusedComponent()
  }

  fun checkValid() = projectSettingsStep?.checkValid() ?: true

  override fun dispose() {
    if (projectSettingsStep != null) {
      Disposer.dispose(projectSettingsStep!!)
      projectSettingsStep = null
      panel = null
    }
  }
}