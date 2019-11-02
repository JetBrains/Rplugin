// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element
import org.jetbrains.r.run.RCommandLineState

class RRunConfiguration internal constructor(project: Project, configurationFactory: ConfigurationFactory)
  : LocatableConfigurationBase<Any>(project, configurationFactory, ""),
  RunConfigurationWithSuppressedDefaultRunAction, RunConfigurationWithSuppressedDefaultDebugAction {
  var scriptPath = ""

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    return RCommandLineState(environment)
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return RRunConfigurationEditor()
  }

  @Throws(RuntimeConfigurationException::class)
  override fun checkConfiguration() {
    try {
      RRunConfigurationUtils.checkConfiguration(this)
    } catch (e: ConfigurationException) {
      throw RuntimeConfigurationException(e.message)
    }
  }

  override fun suggestedName(): String? {
    return RRunConfigurationUtils.suggestedName(this)
  }

  @Throws(InvalidDataException::class)
  override fun readExternal(element: Element) {
    PathMacroManager.getInstance(project).expandPaths(element)
    super.readExternal(element)
    scriptPath = JDOMExternalizerUtil.readField(element, SCRIPT_PATH, "")
  }

  @Throws(WriteExternalException::class)
  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    JDOMExternalizerUtil.writeField(element, SCRIPT_PATH, scriptPath)
    PathMacroManager.getInstance(project).collapsePathsRecursively(element)
  }

  companion object {
    private const val SCRIPT_PATH = "SCRIPT_PATH"
  }
}
