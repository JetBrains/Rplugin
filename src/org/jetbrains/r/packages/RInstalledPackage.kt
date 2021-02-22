// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import com.intellij.webcore.packaging.InstalledPackage

enum class RPackagePriority {
  BASE,
  RECOMMENDED,
  NA
}

class RInstalledPackage(
  packageName: String,
  version: String,
  val priority: RPackagePriority?,
  val libraryPath: String,
  val canonicalPackagePath: String,
  val description: Map<String, String>,
) : InstalledPackage(packageName, version) {

  val isBase: Boolean
    get() = priority == RPackagePriority.BASE

  override fun getVersion(): String = super.getVersion()!!
}
