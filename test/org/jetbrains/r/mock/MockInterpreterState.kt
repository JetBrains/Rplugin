/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterState
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.skeleton.RSkeletonFileType
import java.nio.file.Path

class MockInterpreterState(override val project: Project, var provider: MockInterpreterStateProvider) : RInterpreterState {

  override val isSkeletonInitialized: Boolean = true

  override val rInterop: RInterop
    get() = provider.rInterop

  override val installedPackages: ExpiringList<RInstalledPackage>
    get() = provider.installedPackages.takeIf { it.isNotEmpty() } ?: ExpiringList(
      skeletonFiles
        .mapNotNull { RSkeletonUtil.skeletonFileToRPackage(it) }
        .map {
          val canonicalPath = Path.of(skeletonsDirectory, it.name).toString()
          RInstalledPackage(it.name, it.version, null, skeletonsDirectory, canonicalPath, emptyMap())
        }) { false }

  override val skeletonFiles: Set<VirtualFile>
    get() = provider.skeletonFiles.takeIf { it.isNotEmpty() } ?: findSkeletonFiles()

  private fun findSkeletonFiles(): Set<VirtualFile> {
    val rootPath = Path.of(skeletonsDirectory)
    return VfsUtil.findFile(rootPath, true)?.let { root ->
      val res = mutableListOf<VirtualFile>()
      VfsUtil.iterateChildrenRecursively(root, null) {
        if (it.extension == RSkeletonFileType.EXTENSION) res.add(it)
        true
      }
      val prefix = rootPath.joinToString(separator = "") { it.toString().subSequence(0, 1) }
      res.filter { it.name.startsWith(prefix) }.toSet() // Leave only skeletons for mock
    }.orEmpty()
  }

  override val isUpdating: Boolean
    get() = provider.isUpdating ?: false

  override val userLibraryPath: String
    get() = provider.userLibraryPath

  override val libraryPaths: List<RInterpreterState.LibraryPath>
    get() = provider.libraryPaths

  override val skeletonsDirectory by lazy {
    val version = RInterpreterManager.getInterpreterBlocking(project, RInterpreterUtil.DEFAULT_TIMEOUT)!!.version.toString()
    Path.of(RUsefulTestCase.SKELETON_LIBRARY_PATH, version).toString()
  }

  override fun getPackageByName(name: String): RInstalledPackage? = installedPackages.firstOrNull { it.name == name }

  override fun getLibraryPathByName(name: String): Nothing = throw NotImplementedError()

  override fun getSkeletonFileByPackageName(name: String): PsiFile? {
    val installedPackage = getPackageByName(name) ?: return null
    val virtualFile = RSkeletonUtil.installedPackageToSkeletonFile(skeletonsDirectory, installedPackage) ?: return null
    return PsiManager.getInstance(project).findFile(virtualFile)
  }

  override fun updateState() = resolvedPromise<Unit>()

  override fun markOutdated() { }

  override fun cancelStateUpdating() { }

  override fun scheduleSkeletonUpdate() { }
}
