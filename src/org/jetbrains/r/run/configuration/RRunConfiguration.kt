package org.jetbrains.r.run.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import org.jdom.Element

class RRunConfiguration(project: Project, factory: ConfigurationFactory): RunConfigurationBase<RRunConfiguration>(project, factory, null) {
  var filePath: String = ""
  var workingDirectory: String = ""
  var arguments: String = ""

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? = RCommandLineRunningState(environment)

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = RRunConfigurationEditor()

  override fun readExternal(element: Element) {
    super.readExternal(element)

    val readRFilePath = JDOMExternalizerUtil.readCustomField(element, FILE_PATH)
    if (readRFilePath != null) {
      filePath = readRFilePath
    }

    val readWorkingDirectory = JDOMExternalizerUtil.readCustomField(element, WORKING_DIRECTORY)
    if (readWorkingDirectory != null) {
      workingDirectory = readWorkingDirectory
    }

    val readArgs = JDOMExternalizerUtil.readCustomField(element, ARGS)
    if (readArgs != null) {
      arguments = readArgs
    }
  }

  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    JDOMExternalizerUtil.writeCustomField(element, FILE_PATH, filePath)
    JDOMExternalizerUtil.writeCustomField(element, WORKING_DIRECTORY, workingDirectory)
    JDOMExternalizerUtil.writeCustomField(element, ARGS, arguments)
  }
}

private const val FILE_PATH = "r-file-path"
private const val WORKING_DIRECTORY = "working-directory"
private const val ARGS = "args"