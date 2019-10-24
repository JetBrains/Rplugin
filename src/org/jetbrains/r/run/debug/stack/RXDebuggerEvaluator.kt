// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import icons.org.jetbrains.r.run.debug.stack.RXVar
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.rinterop.RVar
import org.jetbrains.r.rinterop.RVariableLoader
import java.util.concurrent.ExecutorService

class RXDebuggerEvaluator(private val loader: RVariableLoader, private val executor: ExecutorService) : XDebuggerEvaluator() {
  override fun evaluate(expression: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {
    executor.execute {
      try {
        val resultRef = RRef.expressionRef(expression, loader.obj)
        callback.evaluated(RXVar(RVar(expression, resultRef, resultRef.getValueInfo()), executor))
      } catch (e: RDebuggerException) {
        callback.errorOccurred(e.message?.trim()?.lines()?.joinToString(" ") ?: "Error")
      }
    }
  }
}
