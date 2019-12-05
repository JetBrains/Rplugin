/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.remote.RDefaultRepository
import org.jetbrains.r.packages.remote.RMirror
import org.jetbrains.r.packages.remote.RRepoPackage
import java.io.File

interface RInterpreter : RInterpreterInfo {
  val installedPackages: List<RPackage>

  val libraryPaths: List<VirtualFile>

  val userLibraryPath: String

  val cranMirrors: List<RMirror>

  val defaultRepositories: List<RDefaultRepository>

  fun getAvailablePackages(repoUrls: List<String>): Promise<List<RRepoPackage>>

  fun getPackageByName(name: String): RPackage?

  fun getLibraryPathByName(name: String): VirtualFile?

  fun getProcessOutput(scriptText: String): ProcessOutput?

  fun runHelperWithArgs(helper: File, vararg args: String): ProcessOutput

  /**
   * @return a system-dependant paths to the skeleton roots
   */
  val skeletonPaths: List<String>

  val skeletonRoots: Set<VirtualFile>

  /** A place where all skeleton-related data will be stored */
  val skeletonsDirectory: String

  fun runCommand(cmd: String): String? {
    return getProcessOutput(cmd)?.stdout
  }

  fun getHelperOutput(helper: File, vararg args: String): String? {
    return runHelperWithArgs(helper, *args).stdout
  }

  fun getSkeletonFileByPackageName(name: String): PsiFile?

  fun updateState()

  fun findLibraryPathBySkeletonPath(skeletonPath: String): String?
}
