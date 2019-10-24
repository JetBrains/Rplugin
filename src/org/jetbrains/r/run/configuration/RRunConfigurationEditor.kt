// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

import javax.swing.*

class RRunConfigurationEditor(project: Project) : SettingsEditor<RRunConfiguration>() {
  private var myForm: RRunConfigurationForm = RRunConfigurationForm(project)

  public override fun resetEditorFrom(config: RRunConfiguration) {
    RRunConfiguration.copyParams(config, myForm)
  }

  public override fun applyEditorTo(config: RRunConfiguration) {
    RRunConfiguration.copyParams(myForm, config)
  }

  override fun createEditor(): JComponent {
    return myForm.panel
  }
}
