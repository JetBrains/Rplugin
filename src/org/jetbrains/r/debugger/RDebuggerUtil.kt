// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
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

    val listener = object : XBreakpointListener<XLineBreakpoint<XBreakpointProperties<*>>> {
      override fun breakpointAdded(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        if (RSourceFileManager.isInvalid(breakpoint.fileUrl)) {
          runWriteAction { breakpointManager.removeBreakpoint(breakpoint) }
          return
        }
        rInterop.executeTask { updateBreakpoint(rInterop, breakpoint) }
      }
      override fun breakpointRemoved(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        if (RSourceFileManager.isInvalid(breakpoint.fileUrl)) return
        rInterop.executeTask { updateBreakpoint(rInterop, breakpoint, true) }
      }
      override fun breakpointChanged(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        rInterop.executeTask { updateBreakpoint(rInterop, breakpoint) }
      }
    }
    breakpointManager.addBreakpointListener(breakpointType, listener, parentDisposable ?: rInterop)
    breakpointManager.getBreakpoints(breakpointType).forEach { listener.breakpointAdded(it) }
    rInterop.project.messageBus.connect(parentDisposable ?: rInterop)
      .subscribe(XDependentBreakpointListener.TOPIC, object : XDependentBreakpointListener {
        override fun dependencyCleared(breakpoint: XBreakpoint<*>?) {
          if (breakpoint?.type !is RLineBreakpointType) return
          rInterop.executeTask { updateBreakpoint(rInterop, breakpoint) }
        }

        override fun dependencySet(slave: XBreakpoint<*>, master: XBreakpoint<*>) {
          if (master.type is RLineBreakpointType) {
            rInterop.executeTask { updateBreakpoint(rInterop, master) }
          }
          if (slave.type is RLineBreakpointType) {
            rInterop.executeTask { updateBreakpoint(rInterop, slave) }
          }
        }
      })
  }

  private fun updateBreakpoint(rInterop: RInterop, breakpoint: XBreakpoint<*>, remove: Boolean = false) {
    val breakpointInfo = getRInteropBreakpointInfo(rInterop)
    var shouldSuspendInRWrapper = false
    var isEnabled = false
    var isDependent = false
    var evaluateAndLog: String? = null
    var condition: String? = null
    val sourcePosition = runReadAction {
      val breakpointManager = XDebuggerManager.getInstance(rInterop.project).breakpointManager
      val dependentBreakpointManager = (breakpointManager as? XBreakpointManagerImpl)?.dependentBreakpointManager
      isDependent = dependentBreakpointManager?.getMasterBreakpoint(breakpoint)?.type is RLineBreakpointType
      shouldSuspendInRWrapper = breakpoint.suspendPolicy != SuspendPolicy.NONE || breakpoint.isLogStack || breakpoint.isLogMessage ||
                                isDependent || (breakpoint as? XLineBreakpoint<*>)?.isTemporary == true ||
                                dependentBreakpointManager?.getSlaveBreakpoints(breakpoint).orEmpty().any { it.type is RLineBreakpointType }
      isEnabled = breakpoint.isEnabled
      condition = breakpoint.conditionExpression?.expression
      evaluateAndLog = breakpoint.logExpressionObject?.expression
      breakpoint.sourcePosition
    } ?: return
    val position = RSourcePosition(sourcePosition.file, sourcePosition.line)
    if (remove) {
      breakpointInfo.remove(breakpoint)?.uploadedAs?.let {
        rInterop.debugRemoveBreakpoint(it.file, it.line)
      }
      return
    }
    val info = breakpointInfo.getOrPut(breakpoint) { RInteropBreakpointInfo() }
    info.uploadedAs?.let {
      rInterop.debugRemoveBreakpoint(it.file, it.line)
      info.uploadedAs = null
    }
    if (isEnabled && !(isDependent && !info.slaveBreakpointEnabled)) {
      rInterop.debugAddBreakpoint(
        position.file, position.line,
        suspend = shouldSuspendInRWrapper,
        evaluateAndLog = evaluateAndLog,
        condition = condition)
      info.uploadedAs = position
    }
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
    val breakpointInfo = getRInteropBreakpointInfo(rInterop)
    val breakpoint = runReadAction {
      val breakpoint = frame?.position?.let { position ->
        breakpointManager.findBreakpointAtLine(breakpointType, position.file, position.line)
      }
      breakpoint?.also {
        masterBreakpoint = dependentBreakpointManager?.getMasterBreakpoint(it)?.takeIf { master -> master.type is RLineBreakpointType }
        slaveBreakpoints = dependentBreakpointManager?.getSlaveBreakpoints(it).orEmpty()
          .filter { slave -> slave.type is RLineBreakpointType }
        logMessage = it.isLogMessage
        logStack = it.isLogStack
        suspend = it.suspendPolicy != SuspendPolicy.NONE
        leaveEnabled = dependentBreakpointManager?.isLeaveEnabled(it) ?: false
      }
    }
    if (breakpoint == null) return false
    if (masterBreakpoint != null && breakpointInfo[breakpoint]?.slaveBreakpointEnabled != true) return false

    if (masterBreakpoint != null && !leaveEnabled) {
      breakpointInfo[breakpoint]?.let { it.slaveBreakpointEnabled = false }
      updateBreakpoint(rInterop, breakpoint)
    }
    slaveBreakpoints.forEach {
      breakpointInfo[it]?.let { info -> info.slaveBreakpointEnabled = true }
      updateBreakpoint(rInterop, it)
    }
    if (breakpoint.isTemporary) {
      invokeAndWaitIfNeeded {
        runWriteAction {
          breakpointManager.removeBreakpoint(breakpoint)
        }
      }
    }

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

  private fun getRInteropBreakpointInfo(rInterop: RInterop): MutableMap<XBreakpoint<*>, RInteropBreakpointInfo> {
    return rInterop.getUserData(RINTEROP_BREAKPOINT_INFO) ?: mutableMapOf<XBreakpoint<*>, RInteropBreakpointInfo>().also {
      rInterop.putUserData(RINTEROP_BREAKPOINT_INFO, it)
    }
  }

  data class RInteropBreakpointInfo(var uploadedAs: RSourcePosition? = null, var slaveBreakpointEnabled: Boolean = false)

  private val RINTEROP_BREAKPOINT_INFO = Key<MutableMap<XBreakpoint<*>, RInteropBreakpointInfo>>(
    "org.jetbrains.r.debugger.RDebuggerUtil.rInteropBreakpointInfo")
}
