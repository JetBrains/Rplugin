// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.openapi.Disposable
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rinterop.RReference
import org.jetbrains.r.rinterop.RVar

class RXDebuggerEvaluator(private val stackFrame: RXStackFrame, private var parentDisposable: Disposable? = null) : XDebuggerEvaluator() {
  override fun evaluate(expression: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {
    RReference.expressionRef(expression, stackFrame.loader.obj).copyToPersistentRef(parentDisposable).onSuccess {
      it.getValueInfoAsync().onSuccess { rValue ->
        callback.evaluated(RXVar(RVar(expression, it, rValue), stackFrame)
                             .also { rxVar -> setObjectSizes(listOf(rxVar), stackFrame) })
      }.onError {
        callback.errorOccurred("")
      }
    }.onError {
      if (it is RDebuggerException) {
        callback.errorOccurred(it.message?.trim()?.lines()?.joinToString(" ")?.takeIf { msg -> msg.isNotBlank() } ?: "Error")
      } else {
        callback.errorOccurred("")
      }
    }
  }

  fun registerParentDisposable(disposable: Disposable) {
    parentDisposable = disposable
  }
}
