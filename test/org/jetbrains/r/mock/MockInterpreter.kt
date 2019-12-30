/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.R_UNKNOWN
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.packages.remote.RDefaultRepository
import org.jetbrains.r.packages.remote.RMirror
import org.jetbrains.r.packages.remote.RRepoPackage
import org.jetbrains.r.rinterop.RInterop
import java.io.File
import java.nio.file.Paths

class MockInterpreter(private val project: Project, var provider: MockInterpreterProvider) : RInterpreter {
  override val interpreterPath = RInterpreterUtil.suggestHomePath()

  override val interpreterName = "test"

  override val version: Version = R_UNKNOWN

  override val skeletonPaths = listOf(RUsefulTestCase.SKELETON_LIBRARY_PATH)

  private val skeletonFiles = RSkeletonUtil.getSkeletonFiles(skeletonPaths.first(), "")

  override val installedPackages: ExpiringList<RPackage>
    get() = provider.installedPackages.takeIf { it.isNotEmpty() } ?: ExpiringList(skeletonFiles.keys.toList()) { false }

  override val skeletonRoots = setOf(VfsUtil.findFile(Paths.get(RUsefulTestCase.SKELETON_LIBRARY_PATH), false)!!)

  override val userLibraryPath: String
    get() = provider.userLibraryPath

  override val cranMirrors: List<RMirror>
    get() = provider.cranMirrors

  override val defaultRepositories: List<RDefaultRepository>
    get() = provider.defaultRepositories

  override val interop: RInterop
    get() = provider.interop

  override val packageDetails: Map<String, RRepoPackage>?
    get() = provider.packageDetails

  override fun getAvailablePackages(repoUrls: List<String>): Promise<List<RRepoPackage>> {
    return provider.getAvailablePackages(repoUrls)
  }

  override fun getPackageByName(name: String): RPackage? = installedPackages.firstOrNull { it.packageName == name }

  override fun getLibraryPathByName(name: String): VirtualFile? {
    throw NotImplementedError()
  }

  override fun getProcessOutput(scriptText: String): ProcessOutput? = throw NotImplementedError()

  override val libraryPaths: List<VirtualFile>
    get() = provider.libraryPaths

  override val skeletonsDirectory: String
    get() = RUsefulTestCase.SKELETON_LIBRARY_PATH

  override fun findLibraryPathBySkeletonPath(skeletonPath: String): String? = ""

  override fun getSkeletonFileByPackageName(name: String): PsiFile? {
    val ioFile = File(skeletonPaths.first(), getPackageByName(name)?.getLibraryBinFileName() ?: return null)
    val virtualFile = runAsync { VfsUtil.findFileByIoFile(ioFile, true) }
                        .onError { throw it }
                        .blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT) ?: return null
    return PsiManager.getInstance(project).findFile(virtualFile)
  }

  override fun updateState() = runAsync {  }
}
