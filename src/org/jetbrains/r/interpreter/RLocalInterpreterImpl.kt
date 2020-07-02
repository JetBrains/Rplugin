/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import java.io.File

class RLocalInterpreterImpl(
  override val interpreterLocation: RLocalInterpreterLocation,
  versionInfo: Map<String, String>, project: Project) : RInterpreterBase(versionInfo, project) {
  val interpreterPath get() = interpreterLocation.path
  override val basePath = project.basePath!!
  override val hostOS get() = OperatingSystem.current()

  override fun getProcessOutput(scriptText: String) = runScript(scriptText, interpreterPath, basePath)

  override fun runHelper(helper: File, workingDirectory: String?, args: List<String>, errorHandler: ((ProcessOutput) -> Unit)?): String {
    return RInterpreterUtil.runHelper(interpreterPath, helper, workingDirectory, args, errorHandler)
  }

  override fun runMultiOutputHelper(helper: File,
                                    workingDirectory: String?,
                                    args: List<String>,
                                    processor: RMultiOutputProcessor) {
    RInterpreterUtil.runMultiOutputHelper(interpreterPath, helper, workingDirectory, args, processor)
  }

  override fun createRInteropForProcess(process: ProcessHandler, port: Int): RInterop {
    return RInteropUtil.createRInteropForLocalProcess(this, process, port)
  }

  override fun uploadHelperToHost(helper: File): String {
    return helper.absolutePath
  }

  override fun runProcessOnHost(command: GeneralCommandLine): ColoredProcessHandler {
    return ColoredProcessHandler(command.withWorkDirectory(basePath)).apply {
      setShouldDestroyProcessRecursively(true)
    }
  }

  companion object {
    val LOG = Logger.getInstance(RLocalInterpreterImpl::class.java)

    // TODO: run via helper
    fun loadInterpreterVersionInfo(interpreterPath: String, workingDirectory: String): Map<String, String> {
      return runScript("version", interpreterPath, workingDirectory)?.stdoutLines?.map { it.split(' ', limit = 2) }
               ?.filter { it.size == 2 }?.map { it[0] to it[1].trim() }?.toMap() ?: emptyMap()
    }

    private fun runScript(scriptText: String, interpreterPath: String, workingDirectory: String): ProcessOutput? {
      val commandLine = arrayOf<String>(interpreterPath, "--no-restore", "--quiet", "--slave", "-e", scriptText)
      try {
        val processHandler = CapturingProcessHandler(GeneralCommandLine(*commandLine).withWorkDirectory(workingDirectory))
        return processHandler.runProcess(RInterpreterUtil.DEFAULT_TIMEOUT)
      } catch (e: Throwable) {
        LOG.info("Failed to run R executable: \n" +
                 "Interpreter path " + interpreterPath + "\n" +
                 "Exception occurred: " + e.message)
      }
      return null
    }
  }
}
