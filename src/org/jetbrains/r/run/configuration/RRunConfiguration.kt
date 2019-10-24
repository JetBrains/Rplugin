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

open class RRunConfiguration internal constructor(project: Project, configurationFactory: ConfigurationFactory)
  : LocatableConfigurationBase<Any>(project, configurationFactory, ""), RRunConfigurationParams,
  RunConfigurationWithSuppressedDefaultRunAction, RunConfigurationWithSuppressedDefaultDebugAction {
  private var myScriptPath: String
  private var myWorkingDirectoryPath: String

  init {
    myScriptPath = ""
    myWorkingDirectoryPath = ""
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    return RCommandLineState(environment, this)
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return RRunConfigurationEditor(project)
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

  override fun getScriptPath(): String {
    return myScriptPath
  }

  override fun setScriptPath(scriptPath: String) {
    myScriptPath = scriptPath
  }

  override fun getWorkingDirectoryPath(): String {
    return myWorkingDirectoryPath
  }

  override fun setWorkingDirectoryPath(workingDirectoryPath: String) {
    myWorkingDirectoryPath = workingDirectoryPath
  }

  @Throws(InvalidDataException::class)
  override fun readExternal(element: Element) {
    PathMacroManager.getInstance(project).expandPaths(element)

    super.readExternal(element)

    myScriptPath = JDOMExternalizerUtil.readField(element, SCRIPT_PATH, "")
    myWorkingDirectoryPath = JDOMExternalizerUtil.readField(element, WORKING_DIRECTORY_PATH, "")
  }

  @Throws(WriteExternalException::class)
  override fun writeExternal(element: Element) {
    super.writeExternal(element)

    JDOMExternalizerUtil.writeField(element, SCRIPT_PATH, myScriptPath)
    JDOMExternalizerUtil.writeField(element, WORKING_DIRECTORY_PATH, myWorkingDirectoryPath)
    PathMacroManager.getInstance(project).collapsePathsRecursively(element)
  }

  companion object {
    private const val SCRIPT_PATH = "SCRIPT_PATH"
    private const val SCRIPT_ARGS = "SCRIPT_ARGS"
    private const val WORKING_DIRECTORY_PATH = "WORKING_DIRECTORY_PATH"
    private const val PASS_PARENT_ENVS = "PASS_PARENT_ENVS"

    internal fun copyParams(source: RRunConfigurationParams, target: RRunConfigurationParams) {
      target.scriptPath = source.scriptPath
      target.workingDirectoryPath = source.workingDirectoryPath
    }
  }
}
