/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import junit.framework.TestCase
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.interpreter.*
import java.util.concurrent.atomic.AtomicInteger

class RequiredPackageInstallerTest : RInterpreterBaseTestCase() {
  fun testMissingPackagesRemoved() {
    runWithTestPackagesRemoved(LOCAL_PACKAGES) {
      checkMissing(LOCAL_PACKAGES, LOCAL_PACKAGES)
    }
  }

  fun testInstallPackages() {
    runWithTestPackagesRemoved(LOCAL_PACKAGES) {
      installPackages(LOCAL_PACKAGES)
      checkMissing(LOCAL_PACKAGES, emptyList())
    }
  }

  fun testMultipleInstallForSameUtility() {
    val required = listOf(LOCAL_PACKAGES[0])
    runWithTestPackagesRemoved(required) {
      val tasks = (0 until TIMES).map {
        startInstallationTask(UTILITY_NAME, required)
      }
      waitForTasks(tasks)
      checkMissing(required, emptyList())
    }
  }

  fun testInstallForDifferentUtilities() {
    runWithTestPackagesRemoved(LOCAL_PACKAGES) {
      val numGroups = LOCAL_PACKAGES.size - 1
      val tasks = (0 until numGroups).map { i ->
        val required = listOf(LOCAL_PACKAGES[i], LOCAL_PACKAGES[i + 1])  // Note: intersection of requirements is not empty
        startInstallationTask("${UTILITY_NAME}$i", required)
      }
      waitForTasks(tasks)
      checkMissing(LOCAL_PACKAGES, emptyList())
    }
  }

  fun testInstallAfterFailure() {
    val required = listOf(LOCAL_PACKAGES.last())
    runWithTestPackagesRemoved(required) {
      runWithTestPackagesForgotten(required) {
        installPackages(required, false)  // Note: must fail, don't log
        checkMissing(required, required)
      }
      // Note: now required packages can be installed
      installPackages(required)
      checkMissing(required, emptyList())
    }
  }

  private fun checkMissing(packages: List<RequiredPackage>, expected: List<RequiredPackage>) {
    val missing = RequiredPackageInstaller.getInstance(project).getMissingPackages(packages)
    TestCase.assertEquals(expected, missing)
  }

  private fun installPackages(packages: List<RequiredPackage>, isVerbose: Boolean = true) {
    val task = startInstallationTask(UTILITY_NAME, packages, isVerbose)
    waitForTask(task)
  }

  private fun startInstallationTask(utilityName: String, required: List<RequiredPackage>, isVerbose: Boolean = true): Promise<Unit> {
    return RequiredPackageInstaller.getInstance(project)
      .installPackagesWithUserPermission(utilityName, required)
      .onError {
        if (isVerbose) {
          LOG.error("Cannot install packages for test", it)
        }
      }
  }

  private fun waitForTask(task: Promise<Unit>) {
    waitForTasks(listOf(task))
  }

  private fun waitForTasks(tasks: List<Promise<Unit>>) {
    val counter = AtomicInteger(0)
    for (task in tasks) {
      task.onProcessed { counter.getAndIncrement() }
    }
    waitForAtomic(counter, tasks.size)
    updateInterpreter()
  }

  companion object {
    private const val TIMES = 5
    private const val TIMEOUT = 10000
    private const val UTILITY_NAME = "RequiredPackageInstallerTest"

    private fun waitForAtomic(atomic: AtomicInteger, expected: Int) {
      RInterpreterTestUtil.waitForAtomic(atomic, expected, TIMEOUT.toLong())
    }
  }
}
