/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.cfg

import com.intellij.codeInsight.controlflow.ControlFlowBuilder
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.util.SmartList
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*

internal fun buildControlFlow(controlFlowHolder: RControlFlowHolder): RControlFlow {
  return RControlFlow(RControlFlowBuilder().let { it.builder.build (it, controlFlowHolder) }.instructions)
}

private class RControlFlowBuilder: RRecursiveElementVisitor() {
  val builder = ControlFlowBuilder()

  override fun visitPsiElement(element: RPsiElement) {
    element.acceptChildren(this)
  }

  override fun visitArgumentList(argumentList: RArgumentList) {
    argumentList.expressionList.forEach {
      it.accept(this)
    }
    builder.startNode(argumentList)
  }

  override fun visitAssignmentStatement(rAssignmentStatement: RAssignmentStatement) {
    rAssignmentStatement.assignedValue?.accept(this)
    rAssignmentStatement.assignee?.accept(this)
    builder.startNode(rAssignmentStatement)
  }

  override fun visitBlockExpression(block: RBlockExpression) {
    super.visitBlockExpression(block)
    builder.startNode(block)
  }

  override fun visitBooleanLiteral(o: RBooleanLiteral) {
    builder.startNode(o)
  }

  override fun visitBoundaryLiteral(o: RBoundaryLiteral) {
    builder.startNode(o)
  }

  override fun visitBreakStatement(o: RBreakStatement) {
    builder.startNode(o)
    o.loop?.let {
      builder.addPendingEdge(it, builder.prevInstruction)
      builder.flowAbrupted()
    }
  }

  override fun visitCallExpression(o: RCallExpression) {
    if (RPsiUtil.isReturn(o)) {
      o.argumentList.accept(this)
      builder.startNode(o)
      builder.addPendingEdge(null, builder.prevInstruction)
      builder.flowAbrupted()
    } else {
      o.expression.accept(this)
      o.argumentList.accept(this)
      builder.startNode(o)
    }
  }

  override fun visitEmptyExpression(o: REmptyExpression) {
    builder.startNode(o)
  }

  override fun visitForStatement(forStatement: RForStatement) {
    forStatement.range?.accept(this)
    forStatement.target?.accept(this)
    val entry = builder.prevInstruction
    forStatement.body?.accept(this)
    val last = builder.prevInstruction
    val exit = builder.startNode(forStatement)
    handleLoop(entry, last, exit, forStatement)
  }

  override fun visitFunctionExpression(o: RFunctionExpression) {
    builder.startNode(o)
  }

  override fun visitIdentifierExpression(o: RIdentifierExpression) {
    builder.startNode(o)
  }

  override fun visitIfStatement(o: RIfStatement) {
    o.condition?.accept(this)
    val condition = builder.prevInstruction
    o.ifBody?.accept(this)
    val ifBody = if (o.ifBody != null) builder.prevInstruction else null
    builder.prevInstruction = condition
    o.elseBody?.accept(this)
    val end = builder.startNode(o)
    if (ifBody != null) {
      builder.addEdge(ifBody, end)
    }
  }

  override fun visitHelpExpression(help: RHelpExpression) {
    super.visitHelpExpression(help)
    builder.startNode(help)
  }

  override fun visitMemberExpression(o: RMemberExpression) {
    super.visitMemberExpression(o)
    builder.startNode(o)
  }

  override fun visitNaLiteral(o: RNaLiteral) {
    builder.startNode(o)
  }

  override fun visitNamespaceAccessExpression(o: RNamespaceAccessExpression) {
    super.visitNamespaceAccessExpression(o)
    builder.startNode(o)
  }

  override fun visitNextStatement(o: RNextStatement) {
    builder.startNode(o)
    o.loop?.let {
      builder.addPendingEdge(it, builder.prevInstruction)
      builder.flowAbrupted()
    }
  }

  override fun visitNullLiteral(o: RNullLiteral) {
    builder.startNode(o)
  }

  override fun visitNumericLiteralExpression(o: RNumericLiteralExpression) {
    builder.startNode(o)
  }

  override fun visitOperatorExpression(o: ROperatorExpression) {
    if (o.isBinary) {
      o.leftExpr?.accept(this)
      o.rightExpr?.accept(this)
    }
    else {
      o.expr?.accept(this)
    }
    builder.startNode(o)
  }

  override fun visitParameter(o: RParameter) {
    o.defaultValue?.accept(this)
    o.variable?.accept(this)
    builder.startNode(o)
  }

  @Suppress("Duplicates")
  override fun visitRepeatStatement(repeatStatement: RRepeatStatement) {
    val entry = builder.prevInstruction
    val oldSuccessors = SmartList(entry.allSucc())
    repeatStatement.body?.accept(this)
    val last = builder.prevInstruction
    val exit = builder.startNode(repeatStatement)
    handleLoop(entry.allSucc().first { !oldSuccessors.contains(it) }, last, exit, repeatStatement)
  }

  override fun visitStringLiteralExpression(o: RStringLiteralExpression) {
    builder.startNode(o)
  }

  override fun visitSubscriptionExpression(o: RSubscriptionExpression) {
    for (expression in o.expressionList) {
      expression.accept(this)
    }
    builder.startNode(o)
  }

  override fun visitTildeExpression(o: RTildeExpression) {
    super.visitTildeExpression(o)
    builder.startNode(o)
  }

  override fun visitUnaryTildeExpression(o: RUnaryTildeExpression) {
    super.visitUnaryTildeExpression(o)
    builder.startNode(o)
  }

  @Suppress("Duplicates")
  override fun visitWhileStatement(whileStatement: RWhileStatement) {
    val entry = builder.prevInstruction
    val oldSuccessors = SmartList(entry.allSucc())
    whileStatement.condition?.accept(this)
    whileStatement.body?.accept(this)
    val last = builder.prevInstruction
    val exit = builder.startNode(whileStatement)
    handleLoop(entry.allSucc().first { !oldSuccessors.contains(it) }, last, exit, whileStatement)
  }

  private fun handleLoop(entry: Instruction, last: Instruction, exit: Instruction, loop: RLoopStatement) {
    builder.processPending { pendingScope, instruction ->
      if (pendingScope == loop) {
        when (instruction.element) {
          is RNextStatement -> builder.addEdge(instruction, entry)
          is RBreakStatement -> builder.addEdge(instruction, exit)
        }
      }
      else {
        builder.addPendingEdge(pendingScope, instruction)
      }
    }
    builder.addEdge(last, entry)
    if (exit.element !is RRepeatStatement) {
      builder.addEdge(entry, exit)
    }
  }
}