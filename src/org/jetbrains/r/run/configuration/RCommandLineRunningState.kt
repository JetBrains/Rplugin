package org.jetbrains.r.run.configuration

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.runHelperProcess

class RCommandLineRunningState(environment: ExecutionEnvironment?) : CommandLineState(environment) {
  override fun startProcess(): ProcessHandler {
    val configurationSettings = environment.runnerAndConfigurationSettings
    if (configurationSettings == null) {
      throw ExecutionException(RBundle.message("r.run.missing.run.configuration.settings.error.message"))
    }

    val configuration = configurationSettings.configuration
    if (configuration !is RRunConfiguration) {
      throw ExecutionException(RBundle.message("r.run.wrong.run.configuration.class.of.r.file.error.message"))
    }

    val project = environment.project
    val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://" + configuration.filePath)
    if (virtualFile == null || !virtualFile.exists()) {
      throw ExecutionException(RBundle.message("r.run.r.file.doesnt.exist.error.message"))
    }

    val interpreter = RInterpreterManager.getInterpreterOrNull(project)
    if (interpreter == null) {
      throw ExecutionException(RBundle.message("r.run.cant.retrieve.r.interpreter.error.message"))
    }

    val processHandler = interpreter.runHelperProcess(virtualFile.path, configuration.scriptArguments.split(" "),
                                                      workingDirectory = configuration.workingDirectory)

    return processHandler
  }
}