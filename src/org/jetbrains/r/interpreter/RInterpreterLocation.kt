/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import org.jetbrains.annotations.Nls
import org.jetbrains.r.RBundle
import java.io.File

interface RInterpreterLocation {
  @Nls
  fun additionalShortRepresentationSuffix(): String = ""

  @Nls
  fun getWidgetSwitchInterpreterActionHeader(): String

  // workingDirectory is a separate parameter and not a part of GeneralCommandLine because it does not work well with remote paths
  fun runProcessOnHost(command: GeneralCommandLine, workingDirectory: String? = null, isSilent: Boolean = false): BaseProcessHandler<*>

  fun runInterpreterOnHost(args: List<String>, workingDirectory: String? = null, environment: Map<String, String>? = null): BaseProcessHandler<*>

  fun uploadFileToHost(file: File, preserveName: Boolean = false): String

  fun createInterpreter(project: Project): RInterpreterBase

  fun lastModified(): Long? = null

  fun canRead(path: String): Boolean

  fun canWrite(path: String): Boolean

  fun canExecute(path: String): Boolean
}

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

  override fun createInterpreter(project: Project): RInterpreterBase {
    if (!RInterpreterUtil.checkInterpreterLocation(project, this)) {
      throw RuntimeException("Invalid R Interpreter")
    }
    val versionInfo = RInterpreterUtil.loadInterpreterVersionInfo(this)
    return RLocalInterpreterImpl(this, versionInfo, project)
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

fun RInterpreterLocation.getVersion(): Version? {
  return RInterpreterUtil.getVersionByLocation(this)
}
