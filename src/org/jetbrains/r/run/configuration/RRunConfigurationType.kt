// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import org.jetbrains.r.R_LOGO_16

class RRunConfigurationType : ConfigurationTypeBase("RRunConfigurationType", "R", "R run configuration", R_LOGO_16) {
  val mainFactory: ConfigurationFactory
    get() = configurationFactories[0]

  init {
    addFactory(RConfigurationFactory(this))
  }

  private class RConfigurationFactory(configurationType: ConfigurationType) : ConfigurationFactory(configurationType) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
      return RRunConfiguration(project, this)
    }
  }

  companion object {
    val instance: RRunConfigurationType
      get() = ConfigurationTypeUtil.findConfigurationType(RRunConfigurationType::class.java)
  }
}
