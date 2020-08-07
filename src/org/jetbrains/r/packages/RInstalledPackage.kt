// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import com.intellij.webcore.packaging.InstalledPackage

private const val INTERPRETER = "interpreter"

enum class RPackagePriority {
  BASE,
  RECOMMENDED,
  NA
}

class RInstalledPackage(val packageName: String, val packageVersion: String, val priority: RPackagePriority?, val libraryPath: String,
                        val canonicalPackagePath: String, val description: Map<String, String>) : InstalledPackage(packageName, packageVersion) {
  val isStandard: Boolean
    get() = priority == RPackagePriority.BASE || priority == RPackagePriority.RECOMMENDED

  val isUser: Boolean
    get() = priority == null || priority == RPackagePriority.NA
}
