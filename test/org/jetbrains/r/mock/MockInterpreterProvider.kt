/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.common.emptyExpiringList
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.rinterop.RInterop

interface MockInterpreterProvider {
  val interop: RInterop
  val isUpdating: Boolean?
  val userLibraryPath: String
  val libraryPaths: List<VirtualFile>
  val installedPackages: ExpiringList<RInstalledPackage>

  companion object {
    val DUMMY = object : MockInterpreterProvider {
      override val interop: RInterop
        get() = throw NotImplementedError()

      override val isUpdating: Boolean?
        get() = null  // Note: exception is not thrown intentionally (see `MockInterpreter.isUpdating`)

      override val userLibraryPath: String
        get() = throw NotImplementedError()

      override val libraryPaths: List<VirtualFile>
        get() = throw NotImplementedError()

      override val installedPackages: ExpiringList<RInstalledPackage>
        get() = emptyExpiringList(false)  // Note: exception is not thrown intentionally (see `MockInterpreter.installedPackages`)
    }
  }
}
