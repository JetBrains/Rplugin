/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.run.debug.stack

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.frame.*
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.rinterop.RValueEnvironment
import org.jetbrains.r.rinterop.RValueSimple
import org.jetbrains.r.rinterop.RVar
import org.jetbrains.r.run.debug.stack.RXPresentationUtils
import org.jetbrains.r.run.debug.stack.RXStackFrame
import org.jetbrains.r.run.debug.stack.addEnvironmentContents
import java.util.concurrent.CancellationException
import kotlin.math.min

internal class RXVar internal constructor(private val rVar: RVar, private val stackFrame: RXStackFrame) : XNamedValue(rVar.name) {
  private var offset = 0
  private val loader by lazy { rVar.ref.createVariableLoader() }

  override fun computePresentation(node: XValueNode, place: XValuePlace) {
    RXPresentationUtils.setPresentation(rVar, node, stackFrame.executor)
  }

  override fun computeChildren(node: XCompositeNode) {
    stackFrame.executor.execute {
      try {
        val result = XValueChildrenList()
        val endOffset = offset + MAX_ITEMS
        val (vars, totalCount) = loader.loadVariablesPartially(offset, endOffset)
        if (rVar.value is RValueEnvironment) {
          addEnvironmentContents(result, vars, stackFrame)
        } else {
          addListContents(result, vars)
        }
        node.addChildren(result, true)
        offset = min(endOffset, totalCount)
        if (offset != totalCount) {
          node.tooManyChildren(totalCount - offset)
        }
      } catch (e: RDebuggerException) {
        node.setErrorMessage(e.message.orEmpty())
      }
    }
  }

  override fun getModifier(): XValueModifier? {
    if (!rVar.ref.canSetValue()) return null
    return object : XValueModifier() {
      override fun setValue(expression: XExpression, callback: XModificationCallback) {
        if (expression.expression.isBlank()) {
          callback.valueModified()
          return
        }
        val rInterop = rVar.ref.rInterop
        rVar.ref.setValue(RRef.expressionRef(expression.expression, stackFrame.environment))
          .also {
            Disposer.register(stackFrame, Disposable { it.cancel() })
          }
          .onSuccess {
            callback.valueModified()
            rInterop.invalidateCaches()
          }
          .onError {
            if (it is CancellationException) return@onError
            callback.errorOccurred(it.message ?: "Error")
            rInterop.invalidateCaches()
          }
      }
    }
  }

  private fun addListContents(result: XValueChildrenList, vars: List<RVar>) {
    val isVector = rVar.value is RValueSimple
    vars.forEachIndexed { index, it ->
      if (it.name.isEmpty()) {
        val message = if (isVector) "rx.presentation.utils.vector.element.name" else "rx.presentation.utils.list.element.name"
        result.add(RXVar(it.copy(name = RBundle.message(message, offset + index + 1)), stackFrame))
      } else {
        result.add(RXVar(it, stackFrame))
      }
    }
  }

  companion object {
    private const val MAX_ITEMS = 100
  }
}
