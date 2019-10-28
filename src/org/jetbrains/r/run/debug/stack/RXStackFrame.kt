// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.debug.stack

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.*
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.run.debug.stack.RXVar
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rinterop.RValueFunction
import org.jetbrains.r.rinterop.RVar
import org.jetbrains.r.rinterop.RVariableLoader
import java.util.concurrent.ExecutorService

class RXStackFrame(val functionName: String,
                   private val position: XSourcePosition?,
                   private val loader: RVariableLoader,
                   private val executor: ExecutorService,
                   private val grayAttributes: Boolean,
                   private val equalityObject: Any? = null) : XStackFrame() {
  private val evaluator = RXDebuggerEvaluator(loader, executor)

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
        addEnvironmentContents(result, loader.variables, executor, true)
        node.addChildren(result, true)
      } catch (e: RDebuggerException) {
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
              val environment = RXEnvironment(env.name, env.ref.createVariableLoader(), executor)
              children.add(environment)
            }
            node.addChildren(children, true)
          } catch (e: RDebuggerException) {
            node.setErrorMessage(e.message.orEmpty())
          }
        }
      }
    }
    result.addTopGroup(environments)
  }
}

private class RXEnvironment internal constructor(name: String, private val loader: RVariableLoader, private val executor: ExecutorService)
  : XNamedValue(name.takeIf { it.isNotEmpty() } ?: RBundle.message("rx.presentation.utils.environment.unnamed")) {

  override fun computePresentation(node: XValueNode, place: XValuePlace) {
    RXPresentationUtils.setEnvironmentPresentation(node)
  }

  override fun computeChildren(node: XCompositeNode) {
    executor.execute {
      try {
        val result = XValueChildrenList()
        addEnvironmentContents(result, loader.variables, executor)
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

internal fun addEnvironmentContents(result: XValueChildrenList, vars: List<RVar>, executor: ExecutorService,
                                    hideFunctions: Boolean = false) {
  if (hideFunctions) {
    val functions = vars.filter { it.value is RValueFunction }
    if (functions.isNotEmpty()) {
      result.addTopGroup(object : XValueGroup(RBundle.message("variable.view.functions")) {
        override fun computeChildren(node: XCompositeNode) {
          val children = XValueChildrenList()
          functions.forEach { children.add(RXVar(it, executor)) }
          node.addChildren(children, true)
        }
      })
    }
    vars.filter { it.value !is RValueFunction }.forEach { result.add(RXVar(it, executor)) }
  } else {
    vars.forEach { result.add(RXVar(it, executor)) }
  }
}
