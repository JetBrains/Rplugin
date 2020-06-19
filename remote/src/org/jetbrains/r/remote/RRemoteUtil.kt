/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Version
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.RInterpreterBase
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.remote.host.RRemoteHost
import java.io.File

object RRemoteUtil {
  const val DEFAULT_TIMEOUT_MILLIS = 60000L
  
  fun getInterpreterVersion(remoteHost: RRemoteHost, interpreterPath: String): Version? {
    fun checkOutput(output: String): Version? {
      return RInterpreterUtil.parseVersion(output.lineSequence().firstOrNull() ?: return null)
    }
    
    val output = remoteHost.runCommand(GeneralCommandLine(interpreterPath, "--version"))
    if (!output.checkSuccess(LOG)) return null
    return checkOutput(output.stdout) ?: checkOutput(output.stderr)
  }
  
  fun loadInterpreterVersionInfo(remoteHost: RRemoteHost, interpreterPath: String): Map<String, String> {
    val output = remoteHost.runCommand(GeneralCommandLine(interpreterPath, "--no-restore", "--slave", "-e", "version"))
    if (!output.checkSuccess(LOG)) return emptyMap()
    return output.stdout.lines()
      .map { it.split(' ', limit = 2) }
      .filter { it.size == 2 }
      .map { it[0] to it[1].trim() }
      .toMap()
  }

  fun runHelper(remoteHost: RRemoteHost, interpreterPath: String, helper: File,
                args: List<String>, errorHandler: ((ProcessOutput) -> Unit)?): String {
    val time = System.currentTimeMillis()
    try {
      val result = runAsync { runHelperScript(remoteHost, interpreterPath, helper, args) }
                     .onError { RInterpreterBase.LOG.error("Running $helper on ${remoteHost.presentableName}", it) }
                     .blockingGet(DEFAULT_TIMEOUT_MILLIS.toInt()) ?: throw RuntimeException("Timeout for helper '$helper'")
      if (result.exitCode != 0) {
        if (errorHandler != null) {
          errorHandler(result)
          return ""
        } else {
          val message = "Running helper '$helper' on ${remoteHost.presentableName} has non-zero exit code: ${result.exitCode}"
          throw RuntimeException("$message\nStdout: ${result.stdout}\nStderr: ${result.stderr}")
        }
      }
      if (result.stderr.isNotBlank()) {
        // Note: this could be a warning message therefore there is no need to throw
        LOG.warn("Running '$helper' on ${remoteHost.presentableName} has non-blank stderr:\n${result.stderr}")
      }
      if (result.stdout.isBlank()) {
        if (errorHandler != null) {
          errorHandler(result)
          return ""
        }
        throw RuntimeException("Running helper '$helper' on ${remoteHost.presentableName}: cannot get any output")
      }
      return RInterpreterUtil.getScriptStdout(result.stdout)
    } finally {
      LOG.warn("Running ${helper} on ${remoteHost.presentableName} took ${System.currentTimeMillis() - time}ms")
    }
  }

  fun runHelperScript(remoteHost: RRemoteHost, interpreterPath: String,
                      helper: File, args: List<String>, timeout: Int = DEFAULT_TIMEOUT_MILLIS.toInt()): ProcessOutput {
    val remoteHelperPath = remoteHost.uploadRHelper(helper)
    val command = GeneralCommandLine(*getRunHelperCommands(interpreterPath, remoteHelperPath, args).toTypedArray())
    return remoteHost.runCommand(command, timeout.toLong())
  }

  private fun getRunHelperCommands(interpreterPath: String, remoteHelperPath: String, args: List<String>): List<String> {
    return mutableListOf(interpreterPath, "--no-save", "--no-restore", "--slave", "-f", remoteHelperPath, "--args").apply { addAll(args) }
  }

  private val LOG = Logger.getInstance(RRemoteUtil::class.java)
}
