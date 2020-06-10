/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.openapi.util.Version

interface RInterpreterInfo {
  val interpreterName: String
  val interpreterLocation: RInterpreterLocation
  val version: Version
}

data class RBasicInterpreterInfo(
  override val interpreterName: String,
  override val interpreterLocation: RInterpreterLocation,
  override val version: Version
) : RInterpreterInfo {
  companion object {
    fun from(name: String, location: RInterpreterLocation): RBasicInterpreterInfo? {
      val version = try {
        location.getVersion()
      } catch (_: Exception) {
        null
      }
      return version?.let { RBasicInterpreterInfo(name, location, it) }
    }
  }
}

fun List<RInterpreterInfo>.findByPath(path: String): RInterpreterInfo? {
  return find { it.interpreterLocation.toLocalPathOrNull() == path }
}
