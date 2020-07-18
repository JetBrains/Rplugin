/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.common.emptyExpiringList
import org.jetbrains.r.interpreter.RInterpreterState
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.rinterop.RInterop

interface MockInterpreterStateProvider {
  val rInterop: RInterop
  val isUpdating: Boolean?
  val userLibraryPath: String
  val libraryPaths: List<RInterpreterState.LibraryPath>
  val installedPackages: ExpiringList<RInstalledPackage>
  val skeletonFiles: Set<VirtualFile>

  companion object {
    val DUMMY = object : MockInterpreterStateProvider {
      override val rInterop: RInterop
        get() = throw NotImplementedError()

      override val isUpdating: Boolean?
        get() = null  // Note: exception is not thrown intentionally (see `MockInterpreterState.isUpdating`)

      override val userLibraryPath: String
        get() = throw NotImplementedError()

      override val libraryPaths: List<RInterpreterState.LibraryPath>
        get() = throw NotImplementedError()

      override val installedPackages: ExpiringList<RInstalledPackage>
        get() = emptyExpiringList(false)  // Note: exception is not thrown intentionally (see `MockInterpreter.installedPackages`)

      override val skeletonFiles: Set<VirtualFile>
        get() = emptySet()
    }
  }
}
