/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.util.Version

object RVersion {
  fun forceParse(string: String): Version {
    return Version.parseVersion(string) ?: throw RuntimeException("Cannot parse R version from string '$string'")
  }
}

val R_UNKNOWN = Version(0, 0, 0)
val R_3_0 = Version(3, 0, 0)
val R_3_1 = Version(3, 1, 0)
val R_3_2 = Version(3, 2, 0)
val R_3_3 = Version(3, 3, 0)
val R_3_4 = Version(3, 4, 0)
val R_3_5 = Version(3, 5, 0)
val R_3_6 = Version(3, 6, 0)
