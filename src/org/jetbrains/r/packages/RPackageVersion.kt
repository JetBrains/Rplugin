/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import kotlin.math.min

object RPackageVersion {
  fun isNewerOrSame(version: String?, comparedToVersion: String?): Boolean {
    val difference = compare(version, comparedToVersion)
    return difference != null && difference >= 0
  }

  fun isOlder(version: String?, comparedToVersion: String?) = !isNewerOrSame(version, comparedToVersion)

  fun isSame(version: String?, comparedToVersion: String?) = compare(version, comparedToVersion) == 0

  /**
   * Compare two packages' version strings in *almost* (see notes) the same manner
   * as the R function `compareVersion`.
   * **Note:** `null`s are treated as empty strings.
   * **Note:** two `null`s are considered equal (contrary to `compareVersion`)
   * @return negative for less, positive for greater, zero for equal
   * and `null` if version strings are invalid
   */
  fun compare(aVersion: String?, bVersion: String?): Int? {
    return parse(aVersion ?: "")?.let { a ->
      parse(bVersion ?: "")?.let { b ->
        a.compareTo(b)
      }
    }
  }

  /**
   * Convert version string to a list of version numbers.
   * **Note:** blank version string is considered correct and yields an empty list
   */
  fun parse(version: String): List<Int>? {
    return if (version.isNotBlank()) {
      val tokens = version.trim().split('.', '-')
      val numbers = tokens.mapNotNull { it.toIntOrNull() }
      numbers.takeIf { it.size == tokens.size }
    } else {
      emptyList()
    }
  }

  private fun List<Int>.compareTo(other: List<Int>): Int {
    val minSize = min(size, other.size)
    for (i in 0 until minSize) {
      val result = this[i].compareTo(other[i])
      if (result != 0) {
        return result
      }
    }
    return size.compareTo(other.size)
  }
}
