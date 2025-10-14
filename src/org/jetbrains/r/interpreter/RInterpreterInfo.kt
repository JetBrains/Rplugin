package org.jetbrains.r.interpreter

import com.intellij.openapi.util.Version
import com.intellij.r.psi.interpreter.RInterpreterInfo
import com.intellij.r.psi.interpreter.RInterpreterLocation

fun List<RInterpreterInfo>.findByPath(path: String): RInterpreterInfo? {
  return find { it.interpreterLocation.toLocalPathOrNull() == path }
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