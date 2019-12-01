// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import icons.org.jetbrains.r.run.debug.stack.RXVar
import org.jetbrains.concurrency.isPending
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.rinterop.RVar
import java.util.concurrent.ExecutorService

class RXDebuggerEvaluator(
  private val environment: RRef, private val executor: ExecutorService, private var parentDisposable: Disposable? = null)
  : XDebuggerEvaluator() {
  override fun evaluate(expression: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {
    val resultPromise = RRef.expressionRef(expression, environment).copyToPersistentRef(parentDisposable)
    parentDisposable?.let {
      Disposer.register(it, Disposable {
        if (resultPromise.isPending) resultPromise.cancel()
      })
    }
    resultPromise.onSuccess {
      callback.evaluated(RXVar(RVar(expression, it, it.getValueInfo()), executor))
    }.onError {
      if (it is RDebuggerException) {
        callback.errorOccurred(it.message?.trim()?.lines()?.joinToString(" ")?.takeIf { it.isNotEmpty() } ?: "Error")
      } else {
        callback.errorOccurred("")
      }
    }
  }

  fun registerParentDisposable(disposable: Disposable) {
    parentDisposable = disposable
  }
}
