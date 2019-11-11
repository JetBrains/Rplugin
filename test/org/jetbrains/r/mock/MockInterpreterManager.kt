/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.RInterpreterUtil.EDT_TIMEOUT
import org.jetbrains.r.interpreter.R_UNKNOWN
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.packages.remote.RDefaultRepository
import org.jetbrains.r.packages.remote.RMirror
import org.jetbrains.r.packages.remote.RRepoPackage
import java.io.File
import java.nio.file.Paths

class MockInterpreterManager(private val project: Project) : RInterpreterManager {
  override fun initializeInterpreter(force: Boolean): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    promise.setResult(Unit)
    return promise
  }

  override val interpreter: RInterpreter? = object: RInterpreter {
    override val interpreterPath = RInterpreterUtil.suggestHomePath()

    override val interpreterName = "test"

    override val version: Version = R_UNKNOWN

    override val skeletonPaths = listOf(RUsefulTestCase.SKELETON_LIBRARY_PATH)

    override val installedPackages: List<RPackage> = RSkeletonUtil.getSkeletonFiles(skeletonPaths.first(), "").keys.toList()

    override val skeletonRoots = setOf(VfsUtil.findFile(Paths.get(RUsefulTestCase.SKELETON_LIBRARY_PATH), false)!!)

    override val userLibraryPath: String
      get() = throw NotImplementedError()

    override val cranMirrors: List<RMirror>
      get() = throw NotImplementedError()

    override val defaultRepositories: List<RDefaultRepository>
      get() = throw NotImplementedError()

    override fun getAvailablePackages(repoUrls: List<String>): Promise<List<RRepoPackage>> {
      throw NotImplementedError()
    }

    override fun getPackageByName(name: String): RPackage? = installedPackages.firstOrNull { it.packageName == name }

    override fun getProcessOutput(scriptText: String): ProcessOutput? = throw NotImplementedError()

    override val libraryPaths get() = throw NotImplementedError()

    override val skeletonsDirectory: String
      get() = RUsefulTestCase.SKELETON_LIBRARY_PATH

    override fun findLibraryPathBySkeletonPath(skeletonPath: String): String? = ""

    override fun runHelperWithArgs(helper: File, vararg args: String) = throw NotImplementedError()

    override fun getSkeletonFileByPackageName(name: String): PsiFile? {
      val ioFile = File(skeletonPaths.first(), getPackageByName(name)?.getLibraryBinFileName() ?: return null)
      val virtualFile = runAsync { VfsUtil.findFileByIoFile(ioFile, true) }
                          .onError { throw it }
                          .blockingGet(EDT_TIMEOUT) ?: return null
      return PsiManager.getInstance(project).findFile(virtualFile)
    }

    override fun updateState() {}
  }

  override fun hasInterpreter(): Boolean = true
}
