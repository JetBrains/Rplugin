package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.r.psi.rinterop.RObject
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.rinterop.RInteropImpl
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRString

object ProjectsUtils {
  fun getActiveProject(rInterop: RInteropImpl): RObject {
    val path = rInterop.interpreter.basePath
    return path.toRString()
  }

  fun openProject(rInterop: RInteropImpl, args: RObject): Promise<RObject> {
    TODO()
  }

  fun initializeProject(rInterop: RInteropImpl, args: RObject): Promise<RObject> {
    TODO()
  }
}