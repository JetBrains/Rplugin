// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.*
import com.intellij.xdebugger.impl.breakpoints.XBreakpointManagerImpl
import com.intellij.xdebugger.impl.breakpoints.XDependentBreakpointListener
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.rinterop.ExecuteCodeRequest
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RSourceFileManager
import org.jetbrains.r.run.debug.RLineBreakpointType
import kotlin.math.max
import kotlin.math.min

object RDebuggerUtil {
  fun createBreakpointListener(rInterop: RInterop, parentDisposable: Disposable? = rInterop) {
    val breakpointManager = XDebuggerManager.getInstance(rInterop.project).breakpointManager
    val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
    val dependentBreakpointManager = (breakpointManager as? XBreakpointManagerImpl)?.dependentBreakpointManager
    val enabledSlaveBreakpoints = getEnabledSlaveBreakpoints(rInterop)

    fun shouldSuspendInRWrapper(breakpoint: XLineBreakpoint<*>): Boolean {
      return breakpoint.suspendPolicy != SuspendPolicy.NONE || breakpoint.isLogStack || breakpoint.isLogMessage ||
             !dependentBreakpointManager?.getSlaveBreakpoints(breakpoint).isNullOrEmpty()
    }

    val listener = object : XBreakpointListener<XLineBreakpoint<XBreakpointProperties<*>>> {
      override fun breakpointAdded(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) = breakpointAddedImpl(breakpoint)

      override fun breakpointRemoved(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) = breakpointRemovedImpl(breakpoint)

      override fun breakpointChanged(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) = breakpointChangedImpl(breakpoint)

      fun breakpointAddedImpl(breakpoint: XLineBreakpoint<*>) {
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
            suspend = shouldSuspendInRWrapper(breakpoint),
            evaluateAndLog = breakpoint.logExpressionObject?.expression,
            condition = breakpoint.conditionExpression?.expression
          )
        }
      }

      fun breakpointRemovedImpl(breakpoint: XLineBreakpoint<*>) {
        rInterop.executeTask { enabledSlaveBreakpoints.remove(breakpoint) }
        val position = breakpoint.getUserData(BREAKPOINT_POSITION) ?: return
        rInterop.debugRemoveBreakpoint(position.file, position.line)
      }

      fun breakpointChangedImpl(breakpoint: XLineBreakpoint<*>) {
        val position = breakpoint.getUserData(BREAKPOINT_POSITION) ?: return
        rInterop.debugRemoveBreakpoint(position.file, position.line)
        breakpointAddedImpl(breakpoint)
      }
    }
    breakpointManager.addBreakpointListener(breakpointType, listener, parentDisposable ?: rInterop)
    breakpointManager.getBreakpoints(breakpointType).forEach { listener.breakpointAdded(it) }
    rInterop.project.messageBus.connect(parentDisposable ?: rInterop)
      .subscribe(XDependentBreakpointListener.TOPIC, object : XDependentBreakpointListener {
        override fun dependencyCleared(breakpoint: XBreakpoint<*>?) {
          if (breakpoint?.type !is RLineBreakpointType) return
          rInterop.executeTask { enabledSlaveBreakpoints.remove(breakpoint) }
        }

        override fun dependencySet(slave: XBreakpoint<*>, master: XBreakpoint<*>) {
          if (slave.type !is RLineBreakpointType) return
          if (master.type !is RLineBreakpointType || master !is XLineBreakpoint<*>) return
          listener.breakpointChangedImpl(master)
        }
      })
  }

  private fun haveBreakpoints(project: Project, file: VirtualFile, range: TextRange? = null): Boolean {
    return runReadAction {
      val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
      val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
      val breakpoints = breakpointManager.getBreakpoints(breakpointType)
      if (range == null) {
        breakpoints.any { it.fileUrl == file.url }
      } else {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runReadAction false
        fun toValidPosition(x: Int) = max(0, min(document.textLength, x))
        val start = document.getLineNumber(toValidPosition(range.startOffset))
        val end = document.getLineNumber(toValidPosition(range.endOffset - 1))
        breakpoints.any { it.fileUrl == file.url && it.line in start..end }
      }
    }
  }

  fun getFirstDebugCommand(project: Project, file: VirtualFile, range: TextRange? = null): ExecuteCodeRequest.DebugCommand {
    return if (haveBreakpoints(project, file, range)) {
      ExecuteCodeRequest.DebugCommand.CONTINUE
    } else {
      ExecuteCodeRequest.DebugCommand.STOP
    }
  }

  fun processBreakpoint(console: RConsoleView): Boolean {
    val project = console.project
    val rInterop = console.rInterop
    val stack = rInterop.debugStack
    val frame = stack.lastOrNull()
    val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
    val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
    val dependentBreakpointManager = (breakpointManager as? XBreakpointManagerImpl)?.dependentBreakpointManager

    var masterBreakpoint: XBreakpoint<*>? = null
    var slaveBreakpoints = emptyList<XBreakpoint<*>>()
    var logMessage = false
    var logStack = false
    var suspend = false
    var leaveEnabled = false
    val enabledSlaveBreakpoints = getEnabledSlaveBreakpoints(rInterop)
    val breakpoint = runReadAction {
      val breakpoint = frame?.position?.let { position ->
        breakpointManager.findBreakpointAtLine(breakpointType, position.file, position.line)
      }
      breakpoint?.also {
        masterBreakpoint = dependentBreakpointManager?.getMasterBreakpoint(it)
        slaveBreakpoints = dependentBreakpointManager?.getSlaveBreakpoints(it)?.toList().orEmpty()
        logMessage = it.isLogMessage
        logStack = it.isLogStack
        suspend = it.suspendPolicy != SuspendPolicy.NONE
        leaveEnabled = dependentBreakpointManager?.isLeaveEnabled(it) ?: false
      }
    }
    if (breakpoint == null) return false
    if (masterBreakpoint != null && breakpoint !in enabledSlaveBreakpoints) return false

    if (masterBreakpoint != null && !leaveEnabled) enabledSlaveBreakpoints.remove(breakpoint)
    enabledSlaveBreakpoints.addAll(slaveBreakpoints.filter { it.type is RLineBreakpointType })

    invokeLater {
      fun printPosition(position: RSourcePosition) {
        console.print(" (", ConsoleViewContentType.ERROR_OUTPUT)
        console.printHyperlink("${position.file.name}:${position.line + 1}") {
          position.xSourcePosition.createNavigatable(it).navigate(true)
        }
        console.print(")", ConsoleViewContentType.ERROR_OUTPUT)
      }
      if (logMessage && frame != null) {
        console.print("\n", ConsoleViewContentType.ERROR_OUTPUT)
        if (frame.functionName == null || stack.size == 1) {
          console.print(RBundle.message("console.message.breakpoint.hit"), ConsoleViewContentType.ERROR_OUTPUT)
        } else {
          console.print(RBundle.message("console.message.breakpoint.hit.in", frame.functionName), ConsoleViewContentType.ERROR_OUTPUT)
        }
        frame.position?.let { printPosition(it) }
        console.print("\n", ConsoleViewContentType.ERROR_OUTPUT)
      }
      if (logStack) {
        console.print("\n${RBundle.message("console.message.breakpoint.hit")}\n", ConsoleViewContentType.ERROR_OUTPUT)
        stack.reversed().forEachIndexed { index, it ->
          val name = if (index == stack.lastIndex) {
            RBundle.message("debugger.global.stack.frame")
          } else {
            it.functionName ?: RBundle.message("debugger.anonymous.stack.frame")
          }
          console.print("  ${index + 1}: $name", ConsoleViewContentType.ERROR_OUTPUT)
          it.position?.let { printPosition(it) }
          console.print("\n", ConsoleViewContentType.ERROR_OUTPUT)
        }
      }
    }

    return suspend
  }

  private fun getEnabledSlaveBreakpoints(rInterop: RInterop): MutableSet<XBreakpoint<*>> {
    return rInterop.getUserData(ENABLED_SLAVE_BREAKPOINTS) ?: mutableSetOf<XBreakpoint<*>>().also {
      rInterop.putUserData(ENABLED_SLAVE_BREAKPOINTS, it)
    }
  }

  private val BREAKPOINT_POSITION = Key<RSourcePosition>("org.jetbrains.r.debugger.RDebuggerUtil.RSourcePosition")
  private val ENABLED_SLAVE_BREAKPOINTS = Key<MutableSet<XBreakpoint<*>>>("org.jetbrains.r.debugger.RDebuggerUtil.enabledSlaveBreakpoints")
}
