package com.intellij.r.psi.console

import com.intellij.openapi.Disposable
import com.intellij.r.psi.rinterop.RInterop

interface RConsoleView : Disposable {
  val rInterop: RInterop
}