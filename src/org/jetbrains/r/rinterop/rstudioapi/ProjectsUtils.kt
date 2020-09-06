package org.jetbrains.r.rinterop.rstudioapi

import org.jetbrains.concurrency.Promise
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject

object ProjectsUtils {
  fun getActiveProject(rInterop: RInterop): RObject {
    val path = rInterop.interpreter.basePath
    return path.toRString()
  }

  fun openProject(rInterop: RInterop, args: RObject): Promise<RObject> {
    TODO()
  }

  fun initializeProject(rInterop: RInterop, args: RObject): Promise<RObject> {
    TODO()
  }
}