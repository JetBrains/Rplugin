package org.jetbrains.r.rinterop.rstudioapi

import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject

fun getActiveProject(rInterop: RInterop): RObject {
  val path = rInterop.interpreter.basePath
  return path.toRString()
}