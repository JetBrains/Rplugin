// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.Key
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RSourceFileManager
import org.jetbrains.r.run.debug.RLineBreakpointType

object RDebuggerUtil {
  fun createBreakpointListener(rInterop: RInterop, parentDisposable: Disposable? = rInterop) {
    val breakpointManager = XDebuggerManager.getInstance(rInterop.project).breakpointManager
    val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
    val listener = object : XBreakpointListener<XLineBreakpoint<XBreakpointProperties<*>>> {
      override fun breakpointAdded(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        if (RSourceFileManager.isInvalid(breakpoint.fileUrl)) {
          runWriteAction { breakpointManager.removeBreakpoint(breakpoint) }
          return
        }
        val sourcePosition = breakpoint.sourcePosition ?: return
        val position = RSourcePosition(sourcePosition.file, sourcePosition.line)
        breakpoint.putUserData(BREAKPOINT_POSITION, position)
        if (breakpoint.isEnabled) {
          rInterop.debugAddBreakpoint(
            position.file, position.line,
            suspend = breakpoint.suspendPolicy == SuspendPolicy.ALL,
            evaluateAndLog = breakpoint.logExpressionObject?.expression,
            condition = breakpoint.conditionExpression?.expression
          )
        }
      }

      override fun breakpointRemoved(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        val position = breakpoint.getUserData(BREAKPOINT_POSITION) ?: return
        rInterop.debugRemoveBreakpoint(position.file, position.line)
      }

      override fun breakpointChanged(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        breakpointRemoved(breakpoint)
        breakpointAdded(breakpoint)
      }
    }
    breakpointManager.addBreakpointListener(breakpointType, listener, parentDisposable)
    breakpointManager.getBreakpoints(breakpointType).forEach { listener.breakpointAdded(it) }
  }

  private val BREAKPOINT_POSITION = Key<RSourcePosition>("org.jetbrains.r.debugger.RDebuggerUtil.RSourcePosition")
}
