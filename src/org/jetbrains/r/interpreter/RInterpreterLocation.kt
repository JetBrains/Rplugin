/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import java.io.File

interface RInterpreterLocation {
  fun getVersion(): Version?

  fun runHelper(helper: File, workingDirectory: String?, args: List<String>, errorHandler: ((ProcessOutput) -> Unit)? = null): String

  fun runHelperScript(helper: File, args: List<String>, timeout: Int = RInterpreterUtil.DEFAULT_TIMEOUT): ProcessOutput

  fun createInterpreter(project: Project): RInterpreterBase

  fun lastModified(): Long? = null
}

data class RLocalInterpreterLocation(val path: String): RInterpreterLocation {
  override fun toString(): String = path

  override fun getVersion(): Version? {
    if (path.isBlank()) return null
    return RInterpreterUtil.getVersionByPath(path)
  }

  override fun runHelper(helper: File, workingDirectory: String?, args: List<String>, errorHandler: ((ProcessOutput) -> Unit)?): String {
    return RInterpreterUtil.runHelper(path, helper, workingDirectory, args, errorHandler)
  }

  override fun runHelperScript(helper: File, args: List<String>, timeout: Int): ProcessOutput {
    val generalCommandLine = GeneralCommandLine(*RInterpreterUtil.getRunHelperCommands(path, helper, args).toTypedArray())
    return CapturingProcessHandler(generalCommandLine).runProcess(timeout)
  }

  override fun createInterpreter(project: Project): RInterpreterBase {
    val versionInfo = RLocalInterpreterImpl.loadInterpreterVersionInfo(path, project.basePath!!)
    return RLocalInterpreterImpl(this, versionInfo, project)
  }

  override fun lastModified(): Long {
    return File(path).lastModified()
  }
}

fun RInterpreterLocation.toLocalPathOrNull(): String? {
  return (this as? RLocalInterpreterLocation)?.path
}
