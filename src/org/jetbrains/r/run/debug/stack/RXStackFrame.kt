// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.*
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.r.RBundle
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rinterop.*
import java.util.concurrent.ExecutorService

class RXStackFrame(val functionName: String,
                   private val position: XSourcePosition?,
                   val loader: RVariableLoader,
                   val grayAttributes: Boolean,
                   val variableViewSettings: RXVariableViewSettings,
                   private val equalityObject: Any? = null) : XStackFrame(), Disposable {
  private val evaluator = RXDebuggerEvaluator(this, this)
  val environment get() = loader.obj

  override fun getEqualityObject() = equalityObject

  override fun getEvaluator() = evaluator

  override fun getSourcePosition() = position

  override fun customizePresentation(component: ColoredTextContainer) {
    val attributes = if (grayAttributes) SimpleTextAttributes.GRAYED_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES
    if (position == null) {
      component.append(functionName, attributes)
    } else {
      component.append("$functionName, ${position.file.name}:${position.line + 1}", attributes)
    }
    component.setIcon(AllIcons.Debugger.Frame)
  }

  override fun computeChildren(node: XCompositeNode) {
    loader.variablesAsync.getAsync().then {
      val result = XValueChildrenList()
      addEnvironmentsGroup(result)
      addEnvironmentContents(result, it, this, true)
      node.addChildren(result, true)
    }.onError {
      node.setErrorMessage((it as? RDebuggerException)?.message.orEmpty())
    }
  }

  private fun addEnvironmentsGroup(result: XValueChildrenList) {
    val environments = object : XValueGroup(RBundle.message("variable.view.parent.environments")) {
      override fun computeChildren(node: XCompositeNode) {
        loader.parentEnvironments.getAsync().then {
          val children = XValueChildrenList()
          for (env in it) {
            val environment = RXEnvironment(env.name, env.ref.createVariableLoader())
            children.add(environment)
          }
          node.addChildren(children, true)
        }.onError {
          node.setErrorMessage((it as? RDebuggerException)?.message.orEmpty())
        }
      }
    }
    result.addTopGroup(environments)
  }

  override fun dispose() {
  }

  private inner class RXEnvironment internal constructor(name: String, private val loader: RVariableLoader)
    : XNamedValue(name.takeIf { it.isNotEmpty() } ?: RBundle.message("rx.presentation.utils.environment.unnamed")) {

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
      RXPresentationUtils.setEnvironmentPresentation(node)
    }

    override fun computeChildren(node: XCompositeNode) {
      loader.variablesAsync.getAsync().then {
        val result = XValueChildrenList()
        addEnvironmentContents(result, it, this@RXStackFrame)
        node.addChildren(result, true)
      }.onError {
        node.setErrorMessage((it as? RDebuggerException)?.message.orEmpty())
      }
  }

  override fun calculateEvaluationExpression(): Promise<XExpression> {
      return rejectedPromise()
    }
  }
}

internal fun addEnvironmentContents(result: XValueChildrenList, vars: List<RVar>, stackFrame: RXStackFrame,
                                    hideFunctions: Boolean = false) {
  val filteredVars = if (stackFrame.variableViewSettings.showHiddenVariables) {
    vars
  } else {
    vars.filter { !it.name.startsWith('.') || it.name == "..." }
  }
  val rxVars = filteredVars.map { RXVar(it, stackFrame) }
  if (hideFunctions) {
    val functions = rxVars.filter { it.rVar.value is RValueFunction }
    if (functions.isNotEmpty()) {
      result.addTopGroup(object : XValueGroup(RBundle.message("variable.view.functions")) {
        override fun computeChildren(node: XCompositeNode) {
          val children = XValueChildrenList()
          functions.forEach { children.add(it) }
          node.addChildren(children, true)
        }
      })
    }
    rxVars.filter { it.rVar.value !is RValueFunction }.forEach { result.add(it) }
  } else {
    rxVars.forEach { result.add(it) }
  }
  setObjectSizes(rxVars, stackFrame)
}

internal fun setObjectSizes(rxVars: List<RXVar>, stackFrame: RXStackFrame) {
  if (!stackFrame.variableViewSettings.showSize) return
  val filtered = rxVars.filter { it.rVar.value !is RValueError && it.rVar.value !is RValueUnevaluated }
  if (filtered.isEmpty()) return
  val sizes = stackFrame.loader.rInterop.getObjectSizes(filtered.map { it.rVar.ref })
  filtered.zip(sizes).forEach { (rxVar, size) -> rxVar.objectSize = size.takeIf { it >= 0 } }
}

