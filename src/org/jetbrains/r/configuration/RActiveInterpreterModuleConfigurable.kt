/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.configuration

import com.intellij.application.options.ModuleAwareProjectConfigurable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import icons.org.jetbrains.r.RBundle

class RActiveInterpreterModuleConfigurable(project: Project) :
  ModuleAwareProjectConfigurable<RActiveInterpreterConfigurable>(
    project,
    NAME,
    "reference.settings.project.interpreter"
  )
{
  override fun createModuleConfigurable(module: Module?): RActiveInterpreterConfigurable {
    val project = module?.project ?: project
    return RActiveInterpreterConfigurable(project)
  }

  override fun createDefaultProjectConfigurable(): RActiveInterpreterConfigurable? {
    return RActiveInterpreterConfigurable(project)
  }

  companion object {
    private val NAME = RBundle.message("project.settings.module.name")
  }
}
