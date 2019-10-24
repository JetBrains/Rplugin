/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.misc

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.interpreter.RInterpreterImpl
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.RInterpreterUtil.DEFAULT_TIMEOUT
import org.jetbrains.r.interpreter.RLibraryListener
import org.jetbrains.r.interpreter.RLibraryWatcher
import java.util.concurrent.atomic.AtomicInteger

class LibraryWatcherTest : RUsefulTestCase() {
  private val packageName = "rplugin.test.package"
  private val packageFile = "$testDataPath/packages/$packageName.tar.gz"

  fun testPackageInstallUninstall() {
    val project = myFixture.project
    val interpreterPath = RInterpreterUtil.suggestHomePath()
    val interpreter = RInterpreterImpl(RInterpreterImpl.loadInterpreterVersionInfo(interpreterPath, project.basePath!!), interpreterPath, project)
    interpreter.updateState()
    runCommand(interpreterPath, "CMD", "REMOVE", packageName)
    val libraryWatcher = RLibraryWatcher.getInstance(project)
    assertNotEmpty(interpreter.libraryPaths)

    libraryWatcher.registerRootsToWatch(interpreter.libraryPaths)

    val atomicInteger = AtomicInteger(0)
    RLibraryWatcher.subscribe(project, RLibraryWatcher.TimeSlot.LATE, object : RLibraryListener {
      override fun libraryChanged() {
        atomicInteger.incrementAndGet()
      }
    })
    runCommand(interpreterPath, "CMD", "INSTALL", packageFile)
    assertEquals(0, atomicInteger.get())
    libraryWatcher.refresh()
    assertEquals(1, atomicInteger.get())
    runCommand(interpreterPath, "CMD", "REMOVE", packageName)
    assertEquals(1, atomicInteger.get())
    libraryWatcher.refresh()
    assertEquals(2, atomicInteger.get())
  }

  private fun runCommand(vararg args: String) {
    LOG.info("Running: " + args.joinToString())
    val generalCommandLine = GeneralCommandLine(*args)
    val processHandler = CapturingProcessHandler(generalCommandLine)
    val processOutput = processHandler.runProcess(DEFAULT_TIMEOUT)
    LOG.info("STDOUT: " + processOutput.stdout)
    LOG.info("STDERR: " + processOutput.stderr)
  }
}