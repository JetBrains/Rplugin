/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring

import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*

object RRefactoringUtil {
  fun getRScope(local: PsiElement): RControlFlowHolder {
    var context: RControlFlowHolder? = PsiTreeUtil.getParentOfType(local, RFunctionExpression::class.java)
    if (context == null) {
      context = local.containingFile as RFile
    }
    return context
  }

  fun collectUsedNames(scope: PsiElement?): Collection<String> {
    return collectAssignments(scope).map { it.name }
  }

  fun collectAssignments(scope: PsiElement?): Collection<RAssignmentStatement> {
    if (scope == null) return emptyList()
    val assignments = mutableSetOf<RAssignmentStatement>()

    scope.acceptChildren(object : RVisitor(), PsiRecursiveVisitor {
      override fun visitElement(element: PsiElement) {
        if (element !is RFunctionExpression)
          element.acceptChildren(this)
      }

      override fun visitAssignmentStatement(o: RAssignmentStatement) {
        assignments.add(o)
      }
    })
    return assignments
  }

  fun getUniqueName(baseName: String, unavailableNames: MutableSet<String>, isFunctionName: Boolean = false): String {
    if (baseName !in unavailableNames) {
      unavailableNames.add(baseName)
      return baseName
    }

    var i = 1
    val name = if (isFunctionName && baseName.contains(".")) {
      while (baseName.replaceFirst(".", "$i.") in unavailableNames) {
        ++i
      }
      baseName.replaceFirst(".", "$i.")
    }
    else {
      while ("$baseName$i" in unavailableNames) {
        ++i
      }
      "$baseName$i"
    }
    unavailableNames.add(name)
    return name
  }

  fun collectReturns(project: Project, functionExpression: RFunctionExpression): Set<ReturnResult> {
    val controlFlow = functionExpression.controlFlow
    val instructions = controlFlow.instructions

    val returns = mutableSetOf<ReturnResult>()
    ControlFlowUtil.iteratePrev(instructions.size - 1, instructions) { instruction: Instruction ->
      val element = instruction.element ?: return@iteratePrev ControlFlowUtil.Operation.NEXT
      if (element is RFunctionExpression) return@iteratePrev ControlFlowUtil.Operation.CONTINUE

      if (element is RBlockExpression && element.children.isEmpty()) {
        returns.add(ImplicitNullResult(project, element.firstChild))
        return@iteratePrev ControlFlowUtil.Operation.CONTINUE
      }

      val parent = element.parent
      if (parent is RIfStatement && parent.condition == element) return@iteratePrev ControlFlowUtil.Operation.CONTINUE

      if (element is RBlockExpression) return@iteratePrev ControlFlowUtil.Operation.NEXT
      if (element is RIfStatement) {
        if (element.elseBody == null) returns.add(ImplicitNullResult(project, element))
        return@iteratePrev ControlFlowUtil.Operation.NEXT
      }

      if (element is RParameter) return@iteratePrev ControlFlowUtil.Operation.CONTINUE

      if (element is RLoopStatement) {
        returns.add(ImplicitNullResult(project, element))
        return@iteratePrev ControlFlowUtil.Operation.CONTINUE
      }

      if (RPsiUtil.isReturn(element)) {
        val expressionList = (element as RCallExpression).argumentList.expressionList
        returns.add(if (expressionList.isNotEmpty()) CorrectReturnResult(project, element, expressionList.first())
                                                else ImplicitNullResult(project, element))
        return@iteratePrev ControlFlowUtil.Operation.CONTINUE
      }

      returns.add(CorrectReturnResult(project, element))
      return@iteratePrev ControlFlowUtil.Operation.CONTINUE
    }

    return returns
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
}