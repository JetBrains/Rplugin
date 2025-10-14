// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.openapi.Disposable
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.debugger.exception.RDebuggerException
import com.intellij.r.psi.rinterop.RReference
import com.intellij.r.psi.rinterop.RVar
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator

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
        callback.errorOccurred(it.message?.trim()?.lines()?.joinToString(" ")?.takeIf { msg -> msg.isNotBlank() } ?: RBundle.message("rx.presentation.utils.error.value"))
      } else {
        callback.errorOccurred("")
      }
    }
  }

  fun registerParentDisposable(disposable: Disposable) {
    parentDisposable = disposable
  }
}
