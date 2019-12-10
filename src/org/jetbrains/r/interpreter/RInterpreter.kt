/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.google.common.collect.Lists
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.remote.RDefaultRepository
import org.jetbrains.r.packages.remote.RMirror
import org.jetbrains.r.packages.remote.RRepoPackage
import java.io.File
import java.util.*

interface RInterpreter : RInterpreterInfo {
  val installedPackages: List<RPackage>

  val libraryPaths: List<VirtualFile>

  val userLibraryPath: String

  val cranMirrors: List<RMirror>

  val defaultRepositories: List<RDefaultRepository>

  fun getAvailablePackages(repoUrls: List<String>): Promise<List<RRepoPackage>>

  fun getPackageByName(name: String): RPackage?

  fun getLibraryPathByName(name: String): VirtualFile?

  fun getProcessOutput(scriptText: String): ProcessOutput?

  /**
   * @return a system-dependant paths to the skeleton roots
   */
  val skeletonPaths: List<String>

  val skeletonRoots: Set<VirtualFile>

  /** A place where all skeleton-related data will be stored */
  val skeletonsDirectory: String

  fun runCommand(cmd: String): String? {
    return getProcessOutput(cmd)?.stdout
  }

  fun getSkeletonFileByPackageName(name: String): PsiFile?

  fun updateState()

  fun findLibraryPathBySkeletonPath(skeletonPath: String): String?

  companion object {
    private const val RPLUGIN_OUTPUT_BEGIN = ">>>RPLUGIN>>>"
    private const val RPLUGIN_OUTPUT_END = "<<<RPLUGIN<<<"

    fun forceRunHelperOutput(interpreterPath: String,
                             helper: File,
                             workingDirectory: String?,
                             args: List<String>,
                             errorHandler: ((ProcessOutput) -> Unit)? = null): String {
      val scriptName = helper.name
      val time = System.currentTimeMillis()
      try {
        val result = runAsync { runHelperWithArgs(interpreterPath, helper, workingDirectory, *args.toTypedArray()) }
                       .onError { RInterpreterImpl.LOG.error(it) }
                       .blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT) ?: throw RuntimeException("Timeout for helper '$scriptName'")
        if (result.exitCode != 0) {
          if (errorHandler != null) {
            errorHandler(result)
            return ""
          } else {
            val message = "Helper '$scriptName' has non-zero exit code: ${result.exitCode}"
            throw RuntimeException("$message\nStdout: ${result.stdout}\nStderr: ${result.stderr}")
          }
        }
        if (result.stderr.isNotBlank()) {
          // Note: this could be a warning message therefore there is no need to throw
          RInterpreterImpl.LOG.warn("Helper '$scriptName' has non-blank stderr:\n${result.stderr}")
        }
        if (result.stdout.isBlank()) {
          throw RuntimeException("Cannot get any output from helper '$scriptName'")
        }
        val lines = result.stdout
        val start = lines.indexOf(RPLUGIN_OUTPUT_BEGIN).takeIf { it != -1 }
                    ?: throw RuntimeException("Cannot find start marker, output '$lines'")
        val end = lines.indexOf(RPLUGIN_OUTPUT_END).takeIf { it != -1 }
                  ?: throw RuntimeException("Cannot find end marker, output '$lines'")
        return lines.substring(start + RPLUGIN_OUTPUT_BEGIN.length, end)
      } finally {
        RInterpreterImpl.LOG.warn("Running ${scriptName} took ${System.currentTimeMillis() - time}ms")
      }
    }

    fun forceRunHelper(interpreterPath: String,
                       helper: File,
                       workingDirectory: String?,
                       args: List<String>,
                       errorHandler: ((ProcessOutput) -> Unit)? = null): List<String> {
      val output = forceRunHelperOutput(interpreterPath, helper, workingDirectory, args)
      return when {
        output.contains(System.lineSeparator()) -> output.split(System.lineSeparator())
        else -> output.split("\n")
      }
    }

    private fun runHelperWithArgs(interpreterPath: String, helper: File, workingDirectory: String?, vararg args: String): ProcessOutput {
      val command = Lists.newArrayList<String>(
        interpreterPath,
        "--slave",
        "-f", helper.getAbsolutePath(),
        "--args")

      Collections.addAll(command, *args)

      val processHandler = CapturingProcessHandler(GeneralCommandLine(command).withWorkDirectory(workingDirectory))
      val output = processHandler.runProcess(RInterpreterUtil.DEFAULT_TIMEOUT)

      if (output.exitCode != 0) {
        RInterpreterImpl.LOG.warn("Failed to run script. Exit code: " + output.exitCode)
        RInterpreterImpl.LOG.warn(output.stderr)
      }

      return output
    }
  }
}
