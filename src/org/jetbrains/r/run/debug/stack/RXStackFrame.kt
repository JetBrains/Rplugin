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
import org.jetbrains.r.rinterop.RValueFunction
import org.jetbrains.r.rinterop.RVar
import org.jetbrains.r.rinterop.RVariableLoader
import java.util.concurrent.ExecutorService

class RXStackFrame(val functionName: String,
                   private val position: XSourcePosition?,
                   val loader: RVariableLoader,
                   val grayAttributes: Boolean,
                   val showHiddenVariables: Boolean = false,
                   private val equalityObject: Any? = null) : XStackFrame(), Disposable {
  val executor = loader.rInterop.executor
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
    executor.execute {
      try {
        val result = XValueChildrenList()
        addEnvironmentsGroup(result)
        addEnvironmentContents(result, loader.variables, this, true)
        node.addChildren(result, true)
      }
      catch (e: RDebuggerException) {
        node.setErrorMessage(e.message.orEmpty())
      }
    }
  }

  private fun addEnvironmentsGroup(result: XValueChildrenList) {
    val environments = object : XValueGroup(RBundle.message("variable.view.parent.environments")) {
      override fun computeChildren(node: XCompositeNode) {
        executor.execute {
          try {
            val children = XValueChildrenList()
            for (env in loader.parentEnvironments) {
              val environment = RXEnvironment(env.name, env.ref.createVariableLoader(), executor, showHiddenVariables)
              children.add(environment)
            }
            node.addChildren(children, true)
          }
          catch (e: RDebuggerException) {
            node.setErrorMessage(e.message.orEmpty())
          }
        }
      }
    }
    result.addTopGroup(environments)
  }

  override fun dispose() {
  }

  private inner class RXEnvironment internal constructor(name: String, private val loader: RVariableLoader,
                                                         private val executor: ExecutorService,
                                                         private val showHiddenVariables: Boolean)
    : XNamedValue(name.takeIf { it.isNotEmpty() } ?: RBundle.message("rx.presentation.utils.environment.unnamed")) {

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
      RXPresentationUtils.setEnvironmentPresentation(node)
    }

    override fun computeChildren(node: XCompositeNode) {
      executor.execute {
        try {
          val result = XValueChildrenList()
          addEnvironmentContents(result, loader.variables, this@RXStackFrame)
          node.addChildren(result, true)
        } catch (e: RDebuggerException) {
          node.setErrorMessage(e.message.orEmpty())
        }
      }
    }

    override fun calculateEvaluationExpression(): Promise<XExpression> {
      return rejectedPromise()
    }
  }
}

internal fun addEnvironmentContents(result: XValueChildrenList, vars: List<RVar>, stackFrame: RXStackFrame,
                                    hideFunctions: Boolean = false) {
  val filteredVars = if (stackFrame.showHiddenVariables) {
    vars
  } else {
    vars.filter { !it.name.startsWith('.') || it.name == "..." }
  }
  if (hideFunctions) {
    val functions = filteredVars.filter { it.value is RValueFunction }
    if (functions.isNotEmpty()) {
      result.addTopGroup(object : XValueGroup(RBundle.message("variable.view.functions")) {
        override fun computeChildren(node: XCompositeNode) {
          val children = XValueChildrenList()
          functions.forEach { children.add(RXVar(it, stackFrame)) }
          node.addChildren(children, true)
        }
      })
    }
    filteredVars.filter { it.value !is RValueFunction }.forEach {
      result.add(RXVar(it, stackFrame))
    }
  } else {
    filteredVars.forEach { result.add(RXVar(it, stackFrame)) }
  }
}
