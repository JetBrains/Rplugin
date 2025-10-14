/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.registerServiceInstance
import com.intellij.r.psi.common.ExpiringList
import com.intellij.r.psi.interpreter.RInterpreterState
import com.intellij.r.psi.interpreter.RInterpreterStateManager
import org.jetbrains.r.interpreter.RInterpreterTestUtil.makeChildInterpreter
import org.jetbrains.r.mock.MockInterpreterState
import org.jetbrains.r.mock.MockInterpreterStateProvider
import org.jetbrains.r.mock.MockRepoProvider
import com.intellij.r.psi.packages.RInstalledPackage
import com.intellij.r.psi.interpreter.uploadFileToHost
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.remote.RDefaultRepository
import org.jetbrains.r.packages.remote.RRepoPackage
import org.jetbrains.r.packages.remote.RRepositoryWithSelection
import org.jetbrains.r.packages.remote.RepoProvider
import com.intellij.r.psi.rinterop.RInterop
import org.jetbrains.r.run.RProcessHandlerBaseTestCase
import java.io.File

abstract class RInterpreterBaseTestCase : RProcessHandlerBaseTestCase() {
  private lateinit var childInterpreter: RLocalInterpreterImpl
  private lateinit var childInterpreterState: RInterpreterStateImpl
  private lateinit var localRepoProvider: LocalRepoProvider

  override fun setUp() {
    super.setUp()
    addLibraries()
    childInterpreter = makeChildInterpreter(project)
    setupMockInterpreterState()
    setupMockRepoProvider()
  }

  fun runWithTestPackagesForgotten(packages: List<RequiredPackage>, task: () -> Unit) {
    try {
      localRepoProvider.knownPackages.removeAll(packages)
      task()
    } finally {
      localRepoProvider.knownPackages.addAll(packages)
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
    childInterpreterState.updateState().blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
  }

  private fun removeAllLocalPackages(packages: List<RequiredPackage>) {
    for (aPackage in packages) {
      removeLocalPackage(aPackage)
    }
  }

  private fun removeLocalPackage(aPackage: RequiredPackage) {
    RInterpreterTestUtil.removePackage(childInterpreter, aPackage.name)
  }

  private fun setupMockInterpreterState() {
    RInterpreterStateManager.getCurrentStateBlocking(project, DEFAULT_TIMEOUT)?.let { state ->
      val mock = state as MockInterpreterState
      childInterpreterState = rInterop.state as RInterpreterStateImpl
      mock.provider = LocalInterpreterStateProvider(childInterpreterState)
    }
  }

  private fun setupMockRepoProvider() {
    val repoUrl = if (interpreter.isLocal()) {
      LOCAL_REPO_URL
    } else {
      "file:${interpreter.uploadFileToHost(File(LOCAL_REPO_PATH))}"
    }
    localRepoProvider = LocalRepoProvider(repoUrl)
    project.registerServiceInstance(RepoProvider::class.java, localRepoProvider)
  }

  private class LocalInterpreterStateProvider(private val childState: RInterpreterState) : MockInterpreterStateProvider {
    override val rInterop: RInterop
      get() = childState.rInterop

    override val isUpdating: Boolean
      get() = childState.isUpdating

    override val userLibraryPath: String
      get() = childState.userLibraryPath

    override val libraryPaths: List<RInterpreterState.LibraryPath>
      get() = childState.libraryPaths

    override val installedPackages: ExpiringList<RInstalledPackage>
      get() = childState.installedPackages

    override val skeletonFiles: Set<VirtualFile>
      get() = childState.skeletonFiles
  }

  private class LocalRepoProvider(val repoUrl: String) : RepoProvider by MockRepoProvider() {
    val knownPackages = LOCAL_PACKAGES.toMutableList()

    override val name2AvailablePackages: Map<String, RRepoPackage>
      get() = knownPackages.associate { Pair(it.name, it.toRepoPackage()) }

    override suspend fun getMappedEnabledRepositoryUrls(): List<String> = listOf(repoUrl)

    override suspend fun getRepositorySelections(): List<RRepositoryWithSelection>
      = listOf(RRepositoryWithSelection(RDefaultRepository(repoUrl, false), true))

    override suspend fun loadAllPackages(): List<RRepoPackage> = knownPackages.map { it.toRepoPackage() }
  }

  companion object {
    private const val LOCAL_REPO_NAME = "local-cran"
    private const val LOCAL_PACKAGE_VERSION = "0.1.0"
    private const val LOCAL_PACKAGE_BASE_NAME = "anRPluginTestPackage"  // Exercise for the reader: why it's prepended with "an"?

    private val LOCAL_REPO_PATH = "$TEST_DATA_PATH/$LOCAL_REPO_NAME"
    private val LOCAL_REPO_URL = "file:$LOCAL_REPO_PATH"

    val LOCAL_PACKAGES = (1..3).map { index ->
      val name = "${LOCAL_PACKAGE_BASE_NAME}$index"
      RequiredPackage(name, LOCAL_PACKAGE_VERSION)
    }

    private fun RequiredPackage.toRepoPackage() = RRepoPackage(name, LOCAL_REPO_URL, minimalVersion, null)
  }
}
