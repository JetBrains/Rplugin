/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.r.interpreter.RInterpreterUtil.DEFAULT_TIMEOUT
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

object RInterpreterTestUtil {
  private val LOGGER = Logger.getInstance(RInterpreterTestUtil::class.java)

  fun makeSlaveInterpreter(project: Project): RLocalInterpreterImpl {
    val interpreterPath = RInterpreterUtil.suggestHomePath()
    val versionInfo = RLocalInterpreterImpl.loadInterpreterVersionInfo(interpreterPath, project.basePath!!)
    return RLocalInterpreterImpl(RLocalInterpreterLocation(interpreterPath), versionInfo, project).apply {
      updateState().blockingGet(DEFAULT_TIMEOUT)
    }
  }

  fun waitForAtomic(atomic: AtomicInteger, expected: Int, timeout: Long) {
    val start = System.currentTimeMillis()
    while (atomic.get() != expected) {
      if (System.currentTimeMillis() - start > timeout) {
        throw TimeoutException("Waiting for atomic counter for $timeout ms")
      }
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      Thread.sleep(20L)
    }
  }

  fun installPackage(interpreter: RLocalInterpreterImpl, packagePath: String) {
    runCommand(interpreter.interpreterPath, "CMD", "INSTALL", packagePath)
  }

  fun removePackage(interpreter: RLocalInterpreterImpl, packageName: String) {
    runCommand(interpreter.interpreterPath, "CMD", "REMOVE", packageName)
  }

  private fun runCommand(vararg args: String) {
    LOGGER.warn("Running: " + args.joinToString())
    val generalCommandLine = GeneralCommandLine(*args)
    val processHandler = CapturingProcessHandler(generalCommandLine)
    val processOutput = processHandler.runProcess(DEFAULT_TIMEOUT)
    LOGGER.warn("STDOUT: " + processOutput.stdout)
    LOGGER.warn("STDERR: " + processOutput.stderr)
  }
}
