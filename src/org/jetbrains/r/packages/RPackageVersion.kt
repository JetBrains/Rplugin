/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import kotlin.math.min

data class RPackageVersion(val numbers: List<Int>) : Comparable<RPackageVersion> {
  override fun compareTo(other: RPackageVersion): Int {
    val minSize = min(numbers.size, other.numbers.size)
    for (i in 0 until minSize) {
      when {
        numbers[i] > other.numbers[i] -> return GREATER
        numbers[i] < other.numbers[i] -> return LESS
      }
    }
    return when {
      numbers.size > other.numbers.size -> GREATER
      numbers.size < other.numbers.size -> LESS
      else -> EQUAL
    }
  }

  companion object {
    private const val GREATER = 1
    private const val EQUAL = 0
    private const val LESS = -1

    /**
     * **Note:** blank version string is considered correct and yields [RPackageVersion] with empty [RPackageVersion.numbers] list
     */
    fun from(version: String): RPackageVersion? {
      return if (version.isNotBlank()) {
        val tokens = version.trim().split('.', '-')
        val numbers = tokens.mapNotNull { it.toIntOrNull() }
        if (numbers.size == tokens.size) RPackageVersion(numbers) else null
      } else {
        RPackageVersion(emptyList())
      }
    }

    /**
     * Compare two packages' version strings in *almost* (see notes) the same manner
     * as the R function `compareVersion`.
     * **Note:** `null`s are treated as empty strings.
     * **Note:** two `null`s are considered equal (contrary to `compareVersion`)
     * @return negative for less, positive for greater, zero for equal
     * and `null` if version strings are invalid
     */
    fun compare(aVersion: String?, bVersion: String?): Int? {
      return from(aVersion ?: "")?.let { a ->
        from(bVersion ?: "")?.let { b ->
          a.compareTo(b)
        }
      }
    }
  }
}

fun String.isNewerOrSame(version: String?): Boolean {
  val difference = RPackageVersion.compare(this, version)
  return difference != null && difference >= 0
}
