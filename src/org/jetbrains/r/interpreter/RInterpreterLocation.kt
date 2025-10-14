package org.jetbrains.r.interpreter

import com.intellij.openapi.util.Version
import com.intellij.r.psi.interpreter.RInterpreterLocation

fun RInterpreterLocation.getVersion(): Version? {
  return RInterpreterUtil.getVersionByLocation(this)
}
