// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.extractmethod

import com.intellij.codeInsight.codeFragment.CannotCreateCodeFragmentException
import com.intellij.codeInsight.codeFragment.CodeFragment
import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.cfg.RControlFlow
import org.jetbrains.r.psi.cfg.RControlFlowUtil
import org.jetbrains.r.psi.isAssignee

class RCodeFragment(
  val expressions: List<RExpression>,
  val controlFlowHolder: RControlFlowHolder,
  val controlFlow: RControlFlow,
  val entryPoint: Instruction,
  input: Set<String>,
  output: Set<String>,
  returnInside: Boolean,
  val hasReturnWithValue: Boolean,
  val valueUsed: Boolean
): CodeFragment(input, output, returnInside) {
  companion object {
    fun createCodeFragment(expressions: List<RExpression>): RCodeFragment {
      val info = CodeFragmentInfo(expressions)
      return RCodeFragment(expressions, info.controlFlowHolder, info.controlFlow, info.entryPoint, info.parameters,
                           info.outputVariables, info.hasReturnStatement, info.hasReturnWithValue, info.valueUsed)
    }

    private class CodeFragmentInfo(val expressions: List<RExpression>) {
      val controlFlowHolder = PsiTreeUtil.getParentOfType(expressions[0], RControlFlowHolder::class.java)!!
      val controlFlow = controlFlowHolder.controlFlow
      val entryPoint = expressions.asSequence().mapNotNull { RControlFlowUtil.getEntryPoint(it, controlFlow) }.first()
      val instructionsInside = findInstructionsInside()
      lateinit var outputVariables: Set<String>
      var valueUsed = false
      var hasReturnWithValue = false
      var hasReturnStatement = false
      val parameters: Set<String>

      init {
        if (!validateExits()) {
          throw CannotCreateCodeFragmentException("Cannot perform refactoring when execution flow is interrupted")
        }
        findReturnValues()
        parameters = findParameters()
      }

      fun findInstructionsInside(): Array<Boolean> {
        val instructionsInside = Array(controlFlow.instructions.size) { false }
        val visitor = object : RRecursiveElementVisitor() {
          override fun visitElement(o: PsiElement) {
            super.visitElement(o)
            controlFlow.getInstructionByElement(o)?.let { instructionsInside[it.num()] = true }
          }
        }
        expressions.forEach { it.accept(visitor) }
        return instructionsInside
      }

      fun validateExits(): Boolean {
        if (!validateLoopExits()) return false
        var fail = false
        var haveEdgeToExit = false
        var haveEdgeToNext = false
        val lastInstruction = controlFlow.getInstructionByElement(expressions.last()) ?: return false
        ControlFlowUtil.iterate(entryPoint.num(), controlFlow.instructions, {
          if (!instructionsInside[it.num()]) {
            if (it.element == null) {
              haveEdgeToExit = true
            }
            else {
              haveEdgeToNext = true
            }
            ControlFlowUtil.Operation.CONTINUE
          }
          else {
            if (it != lastInstruction && it.allSucc().any { next -> !instructionsInside[next.num()] && next.element != null }) {
              fail = true
              ControlFlowUtil.Operation.BREAK
            }
            else {
              ControlFlowUtil.Operation.NEXT
            }
          }
        }, false)
        if (fail) return false
        if (haveEdgeToNext && haveEdgeToExit) return false
        return true
      }

      fun validateLoopExits(): Boolean {
        val visitor = object : RRecursiveElementVisitor() {
          var loopDepth = 0
          var valid = true

          override fun visitBreakStatement(o: RBreakStatement) {
            if (loopDepth == 0) valid = false
          }

          override fun visitNextStatement(o: RNextStatement) {
            if (loopDepth == 0) valid = false
          }

          override fun visitForStatement(o: RForStatement) = visitLoop(o)
          override fun visitRepeatStatement(o: RRepeatStatement) = visitLoop(o)
          override fun visitWhileStatement(o: RWhileStatement) = visitLoop(o)

          fun visitLoop(o: RExpression) {
            ++loopDepth
            visitExpression(o)
            --loopDepth
          }
        }
        expressions.forEach { it.accept(visitor) }
        return visitor.valid
      }

      private fun findReturnValues() {
        var valueUsed = isValueUsed(expressions.last())
        var hasReturnWithValue = false
        var hasReturnStatement = false
        val changedVariables = mutableSetOf<String>()
        val lastInstruction = controlFlow.getInstructionByElement(expressions.last())
        var lastReachable = false

        ControlFlowUtil.iterate(entryPoint.num(), controlFlow.instructions, {
          if (it == lastInstruction) lastReachable = true
          if (instructionsInside[it.num()]) {
            val element = it.element
            if (element is RCallExpression && RPsiUtil.isReturn(element)) {
              hasReturnStatement = true
              if (element.argumentList.expressionList.isNotEmpty()) {
                hasReturnWithValue = true
              }
            }
            if (element is RAssignmentStatement && !element.isClosureAssignment) {
              val assignee = element.assignee as? RIdentifierExpression
              if (assignee != null) {
                val name = assignee.name
                if (name !in changedVariables && isVariableUsedOutside(name, it)) {
                  changedVariables.add(name)
                }
              }
            }
            ControlFlowUtil.Operation.NEXT
          }
          else {
            ControlFlowUtil.Operation.CONTINUE
          }
        }, false)
        if (!lastReachable || expressions.last() is RLoopStatement || RPsiUtil.isReturn(expressions.last())) {
          valueUsed = false
        }
        // TODO: support multiple return values
        if ((if (valueUsed) 1 else 0) + (if (hasReturnWithValue) 1 else 0) + changedVariables.size > 1) {
          throw CannotCreateCodeFragmentException("Cannot extract method with multiple return values")
        }
        this.outputVariables = changedVariables
        this.hasReturnStatement = hasReturnStatement
        this.hasReturnWithValue = hasReturnWithValue
        this.valueUsed = valueUsed
      }

      private fun isVariableUsedOutside(name: String, start: Instruction): Boolean {
        val localVariableInfo = controlFlowHolder.getLocalVariableInfo(start.element as RPsiElement) ?: return false
        val variableDefinition = localVariableInfo.variables[name] ?: return false
        var used = false
        ControlFlowUtil.iterate(start.num(), controlFlow.instructions, {
          if (it == start) return@iterate ControlFlowUtil.Operation.NEXT
          val element = it.element ?: return@iterate ControlFlowUtil.Operation.NEXT
          if (!instructionsInside[it.num()] && element in variableDefinition.variableDescription.reads) {
            used = true
            return@iterate ControlFlowUtil.Operation.BREAK
          }
          if (element is RAssignmentStatement && element.assignee!! in variableDefinition.variableDescription.writes) {
            return@iterate ControlFlowUtil.Operation.CONTINUE
          }
          ControlFlowUtil.Operation.NEXT
        }, false)
        return used
      }

      private fun findParameters(): Set<String> {
        val parameters = mutableSetOf<String>()

        val visitor = object : RRecursiveElementVisitor() {
          override fun visitIdentifierExpression(o: RIdentifierExpression) {
            val parent = o.parent
            if (o.isAssignee() || (parent is RForStatement && o == parent.target)) return
            val name = o.name
            if (name in parameters) return
            val instruction = controlFlow.getInstructionByElement(o) ?: return
            val localVariableInfo = controlFlowHolder.getLocalVariableInfo(o as RPsiElement) ?: return
            val variableDefinition = localVariableInfo.variables[name] ?: return

            ControlFlowUtil.iteratePrev(instruction.num(), controlFlow.instructions) {
              val element = it.element
              if (element in variableDefinition.variableDescription.writes) {
                if (instructionsInside[it.num()]) {
                  ControlFlowUtil.Operation.CONTINUE
                } else {
                  parameters.add(name)
                  ControlFlowUtil.Operation.BREAK
                }
              } else {
                ControlFlowUtil.Operation.NEXT
              }
            }
          }
        }
        expressions.forEach { it.accept(visitor) }

        val localVariableInfo = controlFlowHolder.getLocalVariableInfo(entryPoint.element as RPsiElement)!!
        for (name in outputVariables) {
          if (name in parameters) continue
          val variableDefinition = localVariableInfo.variables[name] ?: continue
          ControlFlowUtil.iterate(entryPoint.num(), controlFlow.instructions, {
            val element = it.element
            if (element != null) {
              if (element is RAssignmentStatement && element.assignee!! in variableDefinition.variableDescription.writes) {
                return@iterate ControlFlowUtil.Operation.CONTINUE
              }
              if (element in variableDefinition.variableDescription.reads) {
                parameters.add(name)
                return@iterate ControlFlowUtil.Operation.BREAK
              }
            }
            ControlFlowUtil.Operation.NEXT
          }, false)
        }

        return parameters
      }
    }

    private fun isValueUsed(expr: RExpression): Boolean {
      return when (val parent = expr.parent) {
        is RFile -> false
        is RBlockExpression -> expr == parent.expressionList.lastOrNull() && isValueUsed(parent)
        is RForStatement -> expr == parent.range || expr == parent.target
        is RIfStatement -> expr == parent.condition || isValueUsed(parent)
        is RNamespaceAccessExpression -> isValueUsed(parent)
        is RParenthesizedExpression -> isValueUsed(parent)
        is RRepeatStatement -> false
        is RWhileStatement -> expr == parent.condition
        else -> true
      }
    }
  }
}
