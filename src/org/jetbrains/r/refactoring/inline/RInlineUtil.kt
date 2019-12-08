/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.cfg.RControlFlow
import java.util.*

object RInlineUtil {
  fun getLatestDefs(controlFlow: RControlFlow, varName: String, anchor: PsiElement?): List<PsiElement> {
    if (anchor is RAssignmentStatement) return listOf(anchor)

    val par = anchor?.parent
    if (anchor is RIdentifierExpression && par != null && RPsiUtil.getAssignmentByAssignee(anchor) == par) return listOf(par)

    val instructions = controlFlow.instructions
    var anchorInstr = ControlFlowUtil.findInstructionNumberByElement(instructions, anchor)
    if (anchorInstr < 0) return emptyList()
    if (anchor is RIdentifierExpression) {
      val pred = instructions[anchorInstr].allPred()
      if (!pred.isEmpty()) {
        anchorInstr = pred.iterator().next().num()
      }
    }

    return getLatestDefs(varName, instructions, anchorInstr).mapNotNull { it.element }
  }

  private fun getLatestDefs(varName: String, instructions: Array<Instruction>, anchor: Int): Collection<Instruction> {
    val result = LinkedHashSet<Instruction>()
    ControlFlowUtil.iteratePrev(anchor, instructions) { instruction ->
      val element = instruction.element
      if (element is RAssignmentStatement) {
        if (element.name == varName) {
          result.add(instruction)
          return@iteratePrev ControlFlowUtil.Operation.CONTINUE
        }
      }
      ControlFlowUtil.Operation.NEXT
    }
    return result
  }

  fun getPostRefs(controlFlow: RControlFlow, varName: String, anchor: RAssignmentStatement): List<RIdentifierExpression> {
    val result = mutableListOf<RIdentifierExpression>()
    val instructions = controlFlow.instructions
    val exprInstructionNum = controlFlow.getInstructionByElement(anchor)?.num() ?: return emptyList()

    ControlFlowUtil.iterate(exprInstructionNum, instructions, { instruction: Instruction ->
      val element = instruction.element ?: return@iterate ControlFlowUtil.Operation.NEXT
      if (element !is RIdentifierExpression || element.name != varName || RPsiUtil.isNamedArgument(element)) {
        return@iterate ControlFlowUtil.Operation.NEXT
      }

      // a = 3; for (a in 1:5) {}
      val parent = element.parent
      if (parent is RForStatement && PsiTreeUtil.getChildOfType(parent, RIdentifierExpression::class.java) == element) {
        return@iterate ControlFlowUtil.Operation.CONTINUE
      }

      if (RPsiUtil.getAssignmentByAssignee(element) != null) {
        return@iterate ControlFlowUtil.Operation.CONTINUE
      }
      result.add(element)
      ControlFlowUtil.Operation.NEXT
    }, false)

    return result.toList()
  }

  fun collectReturns(project: Project, functionExpression: RFunctionExpression): Set<ReturnResult> {
    val controlFlow = functionExpression.controlFlow
    val instructions = controlFlow.instructions

    val returns = mutableSetOf<ReturnResult>()
    ControlFlowUtil.iteratePrev(instructions.size - 1, instructions) { instruction: Instruction ->
      val element = instruction.element ?: return@iteratePrev ControlFlowUtil.Operation.NEXT

      if (element is RBlockExpression && element.children.isEmpty()) {
        returns.add(ImplicitNullResult(project, element.firstChild))
        return@iteratePrev ControlFlowUtil.Operation.CONTINUE
      }

      if (element is RBlockExpression || element is RIfStatement) return@iteratePrev ControlFlowUtil.Operation.NEXT
      if (element is RParameter) return@iteratePrev ControlFlowUtil.Operation.CONTINUE

      if (element is RLoopStatement) {
        returns.add(ImplicitNullResult(project, element))
        return@iteratePrev ControlFlowUtil.Operation.CONTINUE
      }

      if (RPsiUtil.isReturn(element)) {
        val realArg = (element as RCallExpression).argumentList.expressionList.first()
        returns.add(CorrectReturnResult(project, element, realArg))
        return@iteratePrev ControlFlowUtil.Operation.CONTINUE
      }

      returns.add(CorrectReturnResult(project, element))
      return@iteratePrev ControlFlowUtil.Operation.CONTINUE
    }

    return returns
  }

  inline fun showErrorAndExit(project: Project, editor: Editor?, message: String, returnAction: () -> Unit) {
    CommonRefactoringUtil.showErrorHint(project, editor, message, RBundle.message("inline.assignment.handler.error.title"), null)
    returnAction()
  }

  abstract class ReturnResult(val returnStatement: PsiElement, val returnValue: String) {
    abstract fun doRefactor(resultVariableName: String): PsiElement
    abstract fun getPsiReturnValue(): PsiElement
  }

  class CorrectReturnResult(private val project: Project, psiElement: PsiElement, private val elementInsideReturn: PsiElement? = null)
    : ReturnResult(psiElement, elementInsideReturn?.text ?: psiElement.text) {
    override fun doRefactor(resultVariableName: String): PsiElement {
      return returnStatement.replace(RElementFactory.createRPsiElementFromText(project, "$resultVariableName <- $returnValue"))
    }

    override fun getPsiReturnValue(): PsiElement = elementInsideReturn ?: returnStatement
  }

  class ImplicitNullResult(private val project: Project, lastStatement: PsiElement) : ReturnResult(lastStatement, "NULL") {
    override fun doRefactor(resultVariableName: String): PsiElement {
      val parent = returnStatement.parent
      val element = RElementFactory.createRPsiElementFromText(project, "$resultVariableName <- $returnValue")
      return parent.addAfter(element, returnStatement)
    }

    override fun getPsiReturnValue(): PsiElement = RElementFactory.createRPsiElementFromText(project, returnValue)
  }

  open class RRecursiveElementVisitor : RVisitor(), PsiRecursiveVisitor {
    override fun visitElement(element: PsiElement) {
      element.acceptChildren(this)
    }
  }
}