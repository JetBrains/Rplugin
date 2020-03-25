// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.openapi.Disposable
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.rinterop.RVar

class RXDebuggerEvaluator(private val stackFrame: RXStackFrame, private var parentDisposable: Disposable? = null) : XDebuggerEvaluator() {
  override fun evaluate(expression: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {
    val resultPromise = RRef.expressionRef(expression, stackFrame.loader.obj).copyToPersistentRef(parentDisposable)
    resultPromise.onSuccess {
      callback.evaluated(RXVar(RVar(expression, it, it.getValueInfo()), stackFrame).also { setObjectSizes(listOf(it), stackFrame) })
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
