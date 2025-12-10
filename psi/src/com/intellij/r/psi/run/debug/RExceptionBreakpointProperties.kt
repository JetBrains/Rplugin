package com.intellij.r.psi.run.debug

import com.intellij.xdebugger.breakpoints.XBreakpointProperties

class RExceptionBreakpointProperties : XBreakpointProperties<RExceptionBreakpointProperties>() {
  override fun getState(): RExceptionBreakpointProperties? = null
  override fun loadState(state: RExceptionBreakpointProperties) {}
}