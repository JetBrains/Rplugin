/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.mock.MockInterpreter
import org.jetbrains.r.mock.MockInterpreterProvider
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.remote.RDefaultRepository
import org.jetbrains.r.packages.remote.RMirror
import org.jetbrains.r.packages.remote.RRepoPackage
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.run.RProcessHandlerBaseTestCase

abstract class RInterpreterBaseTestCase : RProcessHandlerBaseTestCase() {
  private lateinit var slaveInterpreter: RInterpreter
  private lateinit var localProvider: LocalProvider

  override fun setUp() {
    super.setUp()
    addLibraries()
    setupMockInterpreter()
  }

  fun runWithTestPackagesForgotten(packages: List<RequiredPackage>, task: () -> Unit) {
    try {
      localProvider.knownPackages.removeAll(packages)
      task()
    } finally {
      localProvider.knownPackages.addAll(packages)
    }
  }

  fun runWithTestPackagesRemoved(packages: List<RequiredPackage>, task: () -> Unit) {
    try {
      removeAllLocalPackages(packages)
      updateInterpreter()
      task()
    } finally {
      removeAllLocalPackages(packages)
    }
  }

  fun updateInterpreter() {
    slaveInterpreter.updateState().blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
  }

  private fun removeAllLocalPackages(packages: List<RequiredPackage>) {
    for (aPackage in packages) {
      removeLocalPackage(aPackage)
    }
  }

  private fun removeLocalPackage(aPackage: RequiredPackage) {
    RInterpreterTestUtil.removePackage(slaveInterpreter, aPackage.name)
  }

  private fun setupMockInterpreter() {
    RInterpreterManager.getInterpreter(project)?.let { interpreter ->
      val mock = interpreter as MockInterpreter
      slaveInterpreter = RInterpreterTestUtil.makeSlaveInterpreter(project)
      localProvider = LocalProvider(rInterop, slaveInterpreter)
      mock.provider = localProvider
    }
  }

  private class LocalProvider(override val interop: RInterop, private val slaveInterpreter: RInterpreter) : MockInterpreterProvider {
    val knownPackages = LOCAL_PACKAGES.toMutableList()

    override val userLibraryPath: String
      get() = slaveInterpreter.userLibraryPath

    override val cranMirrors: List<RMirror>
      get() = slaveInterpreter.cranMirrors

    override val libraryPaths: List<VirtualFile>
      get() = slaveInterpreter.libraryPaths

    override val installedPackages: ExpiringList<RPackage>
      get() = slaveInterpreter.installedPackages

    override val packageDetails: Map<String, RRepoPackage>?
      get() = knownPackages.map { Pair(it.name, it.toRepoPackage()) }.toMap()

    override val defaultRepositories: List<RDefaultRepository>
      get() = listOf(RDefaultRepository(LOCAL_REPO_URL, false))

    override fun getAvailablePackages(repoUrls: List<String>): Promise<List<RRepoPackage>> {
      return runAsync {
        knownPackages.map { it.toRepoPackage() }
      }
    }
  }

  companion object {
    private const val LOCAL_REPO_NAME = "local-cran"
    private const val LOCAL_PACKAGE_VERSION = "0.1.0"
    private const val LOCAL_PACKAGE_BASE_NAME = "anRPluginTestPackage"  // Exercise for the reader: why it's prepended with "an"?

    private val LOCAL_REPO_PATH = "$TEST_DATA_PATH/$LOCAL_REPO_NAME"
    private val LOCAL_REPO_URL = "file://$LOCAL_REPO_PATH"

    val LOCAL_PACKAGES = (1..3).map { index ->
      val name = "${LOCAL_PACKAGE_BASE_NAME}$index"
      RequiredPackage(name, LOCAL_PACKAGE_VERSION)
    }

    private fun RequiredPackage.toRepoPackage() = RRepoPackage(name, LOCAL_REPO_URL, minimalVersion, null)
  }
}
