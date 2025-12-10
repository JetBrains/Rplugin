package com.intellij.r.psi.run.debug

import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointType
import org.jetbrains.annotations.Nls

class RExceptionBreakpointType :  XBreakpointType<XBreakpoint<RExceptionBreakpointProperties>, RExceptionBreakpointProperties>("r-exception", "R Exception Breakpoints") {
  override fun getDisplayText(breakpoint: XBreakpoint<RExceptionBreakpointProperties>?): @Nls String? = null
  companion object {
    private const val ID = "the-r-exception"
    private const val TITLE = "R Exception Breakpoints"
  }
}