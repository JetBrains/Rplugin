package com.intellij.r.psi.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.r.psi.RBundle
import java.io.File

data class RLocalInterpreterLocation(val path: String): RInterpreterLocation {
  override fun toString(): String = path

  override fun getWidgetSwitchInterpreterActionHeader(): String = RBundle.message("interpreter.status.bar.local.interpreters.header")

  override fun runInterpreterOnHost(args: List<String>,
                                    workingDirectory: String?,
                                    environment: Map<String, String>?): BaseProcessHandler<*> {
    return runProcessOnHost(GeneralCommandLine().withExePath(path).withParameters(args).withEnvironment(environment), workingDirectory)
  }

  override fun runProcessOnHost(command: GeneralCommandLine, workingDirectory: String?, isSilent: Boolean): BaseProcessHandler<*> {
    val commandWithWD = command.withWorkDirectory(workingDirectory)
    val handler = if (isSilent) OSProcessHandler.Silent(commandWithWD) else OSProcessHandler(commandWithWD)
    return handler.apply {
      setShouldDestroyProcessRecursively(true)
    }
  }

  override fun uploadFileToHost(file: File, preserveName: Boolean): String = file.path

  override fun createInterpreter(project: Project): Result<RInterpreterBase> {
    return runCatching {
      if (!RInterpreterUtil.checkInterpreterLocation(project, this)) {
        throw RuntimeException("Invalid R Interpreter")
      }
      RLocalInterpreterProvider.getInstance(project).instantiate(this, project)
    }
  }

  override fun lastModified(): Long {
    return File(path).lastModified()
  }

  override fun canRead(path: String): Boolean = File(path).canRead()

  override fun canWrite(path: String): Boolean = File(path).canWrite()

  override fun canExecute(path: String): Boolean = File(path).canExecute()
}

fun RInterpreterLocation.toLocalPathOrNull(): String? {
  return (this as? RLocalInterpreterLocation)?.path
}