/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.process.CapturingProcessRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.r.psi.interpreter.RInterpreter
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

object RInterpreterTestUtil {
  private val LOGGER = Logger.getInstance(RInterpreterTestUtil::class.java)

  fun makeChildInterpreter(project: Project): RLocalInterpreterImpl {
    val interpreterPath = timeoutRunBlocking { RInterpreterUtil.suggestHomePath() }
    val location = RLocalInterpreterLocation(interpreterPath)
    return RLocalInterpreterImpl(location, project)
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

  fun installPackage(interpreter: RInterpreter, packagePath: String) {
    runCommand(interpreter, "CMD", "INSTALL", packagePath)
  }

  fun removePackage(interpreter: RInterpreter, packageName: String) {
    runCommand(interpreter, "CMD", "REMOVE", packageName)
  }

  private fun runCommand(interpreter: RInterpreter, vararg args: String) {
    LOGGER.warn("Running: " + args.joinToString())
    val process = interpreter.interpreterLocation.runInterpreterOnHost(args.toList())
    val processOutput = CapturingProcessRunner(process).runProcess()
    LOGGER.warn("STDOUT: " + processOutput.stdout)
    LOGGER.warn("STDERR: " + processOutput.stderr)
  }
}
