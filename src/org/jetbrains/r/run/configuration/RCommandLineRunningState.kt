package org.jetbrains.r.run.configuration

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.interpreter.RInterpreterManager
import com.intellij.r.psi.interpreter.RInterpreterUtil.DEFAULT_TIMEOUT
import com.intellij.r.psi.interpreter.runHelperProcess

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

    val interpreter = RInterpreterManager.getInterpreterBlocking(project, DEFAULT_TIMEOUT)
    if (interpreter == null) {
      throw ExecutionException(RBundle.message("r.run.cant.retrieve.r.interpreter.error.message"))
    }

    val processHandler = interpreter.runHelperProcess(virtualFile.path,
                                                      scriptArgs = configuration.scriptArguments.split(" "),
                                                      workingDirectory = configuration.workingDirectory,
                                                      environment = configuration.environmentVariablesData.envs,
                                                      interpreterArgs = configuration.interpreterArgs.split(" "))
    processHandler.addProcessListener(object : ProcessListener {
      override fun processTerminated(event: ProcessEvent) {
        processHandler.notifyTextAvailable(RBundle.message("r.run.exit.message", event.exitCode), ProcessOutputTypes.SYSTEM)
      }
    })
    return processHandler
  }
}