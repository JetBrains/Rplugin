// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.google.protobuf.Empty
import com.google.protobuf.Int32Value
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RPluginCoroutineScope
import com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest
import com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest
import com.intellij.r.psi.rinterop.ExecuteCodeRequest
import com.intellij.r.psi.rinterop.SourcePosition
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.*
import com.intellij.xdebugger.impl.breakpoints.XBreakpointManagerImpl
import com.intellij.xdebugger.impl.breakpoints.XDependentBreakpointListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.r.rinterop.RInteropAsyncEventsListener
import org.jetbrains.r.rinterop.RInteropImpl
import org.jetbrains.r.rinterop.RSourceFileManager
import org.jetbrains.r.run.debug.RLineBreakpointType
import kotlin.math.max
import kotlin.math.min

object RDebuggerUtil {
  fun createBreakpointListener(rInterop: RInteropImpl, parentDisposable: Disposable? = rInterop) {
    val breakpointManager = XDebuggerManager.getInstance(rInterop.project).breakpointManager
    val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
    val dependentBreakpointManager = (breakpointManager as? XBreakpointManagerImpl)?.dependentBreakpointManager

    var currentId = 0
    val breakpointToId = mutableMapOf<XBreakpoint<*>, Int>()
    val breakpointsById = mutableMapOf<Int, XBreakpoint<*>>()

    val listener = object : XBreakpointListener<XLineBreakpoint<XBreakpointProperties<*>>> {
      override fun breakpointAdded(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        if (RSourceFileManager.isInvalid(breakpoint.fileUrl)) {
          breakpointManager.removeBreakpoint(breakpoint)
          return
        }
        val position = breakpoint.sourcePosition ?: return
        val id = breakpointToId.getOrPut(breakpoint) {
          (++currentId).also { breakpointsById[it] = breakpoint }
        }
        val request = DebugAddOrModifyBreakpointRequest.newBuilder()
          .setId(id)
          .setPosition(SourcePosition.newBuilder().setFileId(rInterop.sourceFileManager.getFileId(position.file)).setLine(position.line))
          .setEnabled(breakpoint.isEnabled)
          .setSuspend(breakpoint.suspendPolicy != SuspendPolicy.NONE)
          .setCondition(breakpoint.conditionExpression?.expression.orEmpty())
          .setEvaluateAndLog(breakpoint.logExpressionObject?.expression.orEmpty())
          .setHitMessage(breakpoint.isLogMessage)
          .setPrintStack(breakpoint.isLogStack)
          .setRemoveAfterHit(breakpoint.isTemporary)
          .build()
        rInterop.executeTask {
          rInterop.execute(rInterop.asyncStub::debugAddOrModifyBreakpoint, request)
        }
      }
      override fun breakpointRemoved(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        val id = breakpointToId.remove(breakpoint) ?: return
        breakpointsById.remove(id)
        rInterop.executeTask {
          rInterop.execute(rInterop.asyncStub::debugRemoveBreakpoint, Int32Value.of(id))
        }
      }
      override fun breakpointChanged(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        breakpointAdded(breakpoint)
      }
    }
    val dependantListener = object : XDependentBreakpointListener {
      override fun dependencyCleared(breakpoint: XBreakpoint<*>?) {
        val id = breakpointToId[breakpoint] ?: return
        rInterop.executeTask {
          rInterop.execute(rInterop.asyncStub::debugSetMasterBreakpoint, DebugSetMasterBreakpointRequest.newBuilder()
            .setBreakpointId(id)
            .setNone(Empty.getDefaultInstance())
            .build())
        }
      }

      override fun dependencySet(slave: XBreakpoint<*>, master: XBreakpoint<*>) {
        val masterId= breakpointToId[master] ?: return dependencyCleared(slave)
        val slaveId = breakpointToId[slave] ?: return
        val leaveEnabled = dependentBreakpointManager?.isLeaveEnabled(slave) ?: false
        rInterop.executeTask {
          rInterop.execute(rInterop.asyncStub::debugSetMasterBreakpoint, DebugSetMasterBreakpointRequest.newBuilder()
            .setBreakpointId(slaveId)
            .setMasterId(masterId)
            .setLeaveEnabled(leaveEnabled)
            .build())
        }
      }
    }

    breakpointManager.getBreakpoints(breakpointType).forEach { listener.breakpointAdded(it) }
    breakpointManager.getBreakpoints(breakpointType).forEach { breakpoint ->
      dependentBreakpointManager?.getMasterBreakpoint(breakpoint)?.let { dependantListener.dependencySet(breakpoint, it) }
    }

    breakpointManager.addBreakpointListener(breakpointType, listener, parentDisposable ?: rInterop)
    rInterop.project.messageBus.connect(parentDisposable ?: rInterop)
      .subscribe(XDependentBreakpointListener.TOPIC, dependantListener)

    rInterop.addAsyncEventsListener(object : RInteropAsyncEventsListener {
      override fun onRemoveBreakpointByIdRequest(id: Int) {
        RPluginCoroutineScope.getScope(rInterop.project).launch(Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()) {
          val breakpoint = breakpointsById.remove(id) ?: return@launch
          breakpointToId.remove(breakpoint)
          breakpointManager.removeBreakpoint(breakpoint)
        }
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
}
