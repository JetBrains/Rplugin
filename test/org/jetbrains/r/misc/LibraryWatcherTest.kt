/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.misc

import org.jetbrains.r.interpreter.RInterpreterBaseTestCase
import org.jetbrains.r.interpreter.RInterpreterTestUtil
import org.jetbrains.r.interpreter.RLibraryWatcher
import java.util.concurrent.atomic.AtomicInteger

class LibraryWatcherTest : RInterpreterBaseTestCase() {
  private val packageName = "rplugin.test.package"
  private val packagePath = "$testDataPath/packages/$packageName.tar.gz"

  fun testPackageInstallUninstall() {
    val project = myFixture.project
    val interpreter = RInterpreterTestUtil.makeChildInterpreter(project)
    RInterpreterTestUtil.removePackage(interpreter, packageName)
    val libraryWatcher = RLibraryWatcher.getInstance(project)
    assertNotEmpty(rInterop.state.libraryPaths)
    libraryWatcher.updateRootsToWatch()

    val atomicInteger = AtomicInteger(0)
    RLibraryWatcher.subscribeAsync(project, RLibraryWatcher.TimeSlot.LAST) {
      atomicInteger.incrementAndGet()
    }
    RInterpreterTestUtil.installPackage(interpreter, packagePath)
    assertEquals(0, atomicInteger.get())
    libraryWatcher.refresh()
    waitForAtomic(atomicInteger, 1)
    assertEquals(1, atomicInteger.get())
    RInterpreterTestUtil.removePackage(interpreter, packageName)
    assertEquals(1, atomicInteger.get())
    libraryWatcher.refresh()
    waitForAtomic(atomicInteger, 2)
    assertEquals(2, atomicInteger.get())
  }

  companion object {
    private const val TIMEOUT = 5000L

    private fun waitForAtomic(atomic: AtomicInteger, expected: Int) {
      RInterpreterTestUtil.waitForAtomic(atomic, expected, TIMEOUT)
    }
  }
}