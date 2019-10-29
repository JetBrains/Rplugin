/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.project.DumbServiceImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.impl.ProjectImpl
import com.intellij.openapi.util.Version
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexImpl
import com.intellij.util.indexing.UnindexedFilesUpdater
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.interpreter.*
import org.jetbrains.r.interpreter.RInterpreterUtil.EDT_TIMEOUT
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.packages.remote.RDefaultRepository
import org.jetbrains.r.packages.remote.RMirror
import org.jetbrains.r.packages.remote.RRepoPackage
import org.jetbrains.r.skeleton.RSkeletonFileType
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.Collections.singletonMap

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

fun Project.setupMockInterpreterManager() {
  prepareTestSkeletons(this)
  (this as ProjectImpl).registerComponentImplementation(RInterpreterManager::class.java, MockInterpreterManager::class.java, true)
  val dumbService = DumbServiceImpl.getInstance(this)
  if (FileBasedIndex.getInstance() is FileBasedIndexImpl) {
    dumbService.queueTask(UnindexedFilesUpdater(this))
  }
}

private val packageNamesForTests: Set<String> = """
  base
  datasets
  data.table
  dplyr
  graphics
  grDevices
  magrittr
  methods
  stats
  utils
""".trimIndent().split("\n").toSet()


private fun prepareTestSkeletons(project: Project) {
  RSkeletonUtil.checkVersion(RUsefulTestCase.SKELETON_LIBRARY_PATH)
  val missingTestSkeletons = missingTestSkeletons()
  if (missingTestSkeletons.isEmpty()) return

  System.err.println("Generate binary summary for: " + missingTestSkeletons)

  val interpreterPath = RInterpreterUtil.suggestHomePath()
  check(!(interpreterPath.isBlank() || RInterpreterUtil.getVersionByPath(interpreterPath) == null)) { "No interpreter to build skeletons" }
  val versionInfo = RInterpreterImpl.loadInterpreterVersionInfo(interpreterPath, project.basePath!!)
  val rInterpreter = RInterpreterImpl(versionInfo, interpreterPath, project)
  rInterpreter.updateState()
  val packagesForTest = missingTestSkeletons.map {
    rInterpreter.getPackageByName(it) ?: throw IllegalStateException("No package $it found for $interpreterPath")
  }
  RSkeletonUtil.generateSkeletons(singletonMap(RUsefulTestCase.SKELETON_LIBRARY_PATH, packagesForTest), rInterpreter)
}

private fun missingTestSkeletons(): Set<String> {
  val skeletonsDirectory = File(RUsefulTestCase.SKELETON_LIBRARY_PATH)
  val existedSkeletons = skeletonsDirectory.listFiles { _, name -> name.endsWith(".${RSkeletonFileType.EXTENSION}") }

  if (existedSkeletons == null) {
    if (!skeletonsDirectory.mkdirs()) {
      throw IOException("Can't create $skeletonsDirectory")
    }
    return packageNamesForTests
  }

  val foundSkeletons =  existedSkeletons.map { it }.map { nameOfBinSummary(it) }.toSet()

  existedSkeletons.forEach {
    if (!packageNamesForTests.contains(nameOfBinSummary(it))) {
      it.delete()
    }
  }

  return packageNamesForTests.minus(foundSkeletons)
}

private fun nameOfBinSummary(file: File): String {
  val fileName = file.nameWithoutExtension
  return fileName.indexOf('-').takeIf { it != -1 }?.let { fileName.substring(0, it) } ?: fileName
}
