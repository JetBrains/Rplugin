package com.intellij.r.psi.interpreter

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.r.psi.common.ExpiringList
import com.intellij.r.psi.packages.RInstalledPackage
import com.intellij.r.psi.rinterop.RInterop
import org.jetbrains.concurrency.Promise

interface RInterpreterState {
  /**
   *  true if skeletons update was performed at least once.
   */
  val isSkeletonInitialized: Boolean

  /** A place where all skeleton-related data will be stored */
  val skeletonsDirectory: String

  val project: Project

  val rInterop: RInterop

  data class LibraryPath(val path: String, val isWritable: Boolean)
  val libraryPaths: List<LibraryPath>

  val skeletonFiles: Set<VirtualFile>

  val installedPackages: ExpiringList<RInstalledPackage>

  val userLibraryPath: String

  val isUpdating: Boolean

  fun getPackageByName(name: String): RInstalledPackage?

  fun getLibraryPathByName(name: String): LibraryPath?

  fun getSkeletonFileByPackageName(name: String): PsiFile?

  fun updateState(): Promise<Unit>

  fun cancelStateUpdating()

  fun markOutdated()

  fun scheduleSkeletonUpdate()

  fun hasPackage(name: String): Boolean {
    return getPackageByName(name) != null
  }
}
