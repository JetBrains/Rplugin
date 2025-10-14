/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.openapi.Disposable
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.rinterop.*
import com.intellij.r.psi.util.tryRegisterDisposable
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.frame.*
import java.util.concurrent.CancellationException

internal class RXVar internal constructor(val rVar: RVar, val stackFrame: RXStackFrame,
                                          private val isChildOfRoot: Boolean = false,
                                          var markChanged: Boolean = false) : XNamedValue(rVar.name) {
  private val listBuilder by lazy {
    val loader = if ((rVar.value as? RValueSimple)?.isS4 == true) {
      rVar.ref.getAttributesRef().createVariableLoader()
    } else {
      rVar.ref.createVariableLoader()
    }
    object : PartialChildrenListBuilder(stackFrame, loader) {
      override fun addContents(result: XValueChildrenList, vars: List<RVar>, offset: Long) {
        if (rVar.value is RValueEnvironment) {
          addEnvironmentContents(result, vars, stackFrame)
        } else {
          addListContents(result, vars, offset)
        }
      }
    }
  }

  var objectSize: Long? = null

  override fun computePresentation(node: XValueNode, place: XValuePlace) {
    RXPresentationUtils.setPresentation(node, this)
  }

  override fun computeChildren(node: XCompositeNode) {
    listBuilder.computeChildren(node)
  }

  override fun getModifier(): XValueModifier? {
    if (!rVar.ref.canSetValue()) return null
    return object : XValueModifier() {
      override fun setValue(expression: XExpression, callback: XModificationCallback) {
        val rInterop = rVar.ref.rInterop
        if (expression.expression.isBlank()) {
          callback.valueModified()
          return
        }
        rVar.ref.setValue(RReference.expressionRef(expression.expression, stackFrame.environment))
          .also {
            stackFrame.tryRegisterDisposable(Disposable { it.cancel() })
          }
          .onSuccess {
            rInterop.invalidateCaches()
            if (it is RValueFunction && isChildOfRoot) {
              stackFrame.expandFunctionGroup = true
              stackFrame.functionToMarkAsChanged = rVar.name
            }
            callback.valueModified()
          }
          .onError {
            if (it is CancellationException) return@onError
            rInterop.invalidateCaches()
            callback.errorOccurred(it.message ?: RBundle.message("rx.presentation.utils.error.value"))
          }
      }
    }
  }

  private fun addListContents(result: XValueChildrenList, vars: List<RVar>, offset: Long) {
    val isVector = rVar.value is RValueSimple
    val rxVars = mutableListOf<RXVar>()
    vars.forEachIndexed { index, it ->
      if (it.name.isEmpty()) {
        val message = if (isVector) {
          RBundle.message("rx.presentation.utils.vector.element.name", offset + index + 1)
        } else {
          RBundle.message("rx.presentation.utils.list.element.name", offset + index + 1)
        }
        rxVars.add(RXVar(it.copy(name = message), stackFrame))
      } else {
        rxVars.add(RXVar(it, stackFrame))
      }
    }
    rxVars.forEach { result.add(it) }
    if (!isVector) setObjectSizes(rxVars, stackFrame)
  }
}
