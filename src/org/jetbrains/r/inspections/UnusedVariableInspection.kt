// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*

/**
 * Flag unused variables. We never flag functions calls (even when not being assigned) because
 * of potential side effects.
 */
class UnusedVariableInspection : org.jetbrains.r.inspections.RInspection() {

  override fun getDisplayName(): String {
    return RBundle.message("inspection.unusedVariable.name")
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : org.jetbrains.r.psi.api.RVisitor() {

    override fun visitAssignmentStatement(element: RAssignmentStatement) {
      val assignee = element.assignee ?: return

      if (RPsiUtil.isReturnValue(element)) {
        return
      }
      if (isInplaceAssignment(assignee)) {
        return
      }
      // not supported
      if (element.isClosureAssignment) {
        return
      }

      val function = PsiTreeUtil.getParentOfType(element, RFunctionExpression::class.java) ?: return
      val name = assignee.name
      val localVariableInfo = function.getLocalVariableInfo(element)
      val variableDefinition = localVariableInfo?.variables?.get(name) ?: return
      val controlFlow = function.controlFlow
      var found = false
      var rewritten = false

      ControlFlowUtil.iterate(controlFlow.getInstructionByElement(element)?.num()!!, controlFlow.instructions, { instruction ->
        val currentElement = instruction.element ?: return@iterate ControlFlowUtil.Operation.NEXT
        if (currentElement == element) {
          return@iterate ControlFlowUtil.Operation.NEXT
        }
        if (variableDefinition.variableDescription.reads.contains(currentElement)) {
          found = true
          return@iterate ControlFlowUtil.Operation.BREAK
        }
        val writes = variableDefinition.variableDescription.writes
        if (writes.contains(currentElement) && currentElement.parent is RForStatement ||
            currentElement is RAssignmentStatement && currentElement.assignee?.let { writes.contains(it) } == true ) {
          rewritten = true
          return@iterate ControlFlowUtil.Operation.CONTINUE
        }
        if (currentElement is RFunctionExpression &&
            currentElement.localAnalysisResult.closure.contains(variableDefinition.variableDescription)) {
          found = true
          return@iterate ControlFlowUtil.Operation.BREAK
        }
        return@iterate ControlFlowUtil.Operation.NEXT
      }, false)

      if (!found) {
        val message = if (!rewritten) "Variable '" + assignee.text + "' is never used"
                                 else "Variable '" + assignee.text + "' is assigned but never accessed"
        myProblemHolder.registerProblem(assignee,
                                        message,
                                        ProblemHighlightType.LIKE_UNUSED_SYMBOL)
      }
    }


    private fun isInplaceAssignment(assignee: PsiElement): Boolean {
      if (assignee is RSubscriptionExpression) return true

      if (assignee !is RCallExpression) return false

      // Check if we can resolve it into a accessor setter

      // See https://cran.r-project.org/doc/manuals/r-release/R-lang.html#Attributes
      // See https://cran.r-project.org/doc/manuals/r-release/R-lang.html#Function-calls

      // check if it can be resolved it into an accessor function
      val reference = assignee.expression.reference ?: return false
      val accessorMethodName = "`" + reference.canonicalText + "<-`()"

      val accessorResolvant = org.jetbrains.r.psi.RElementFactory
        .createFuncallFromText(assignee.getProject(), accessorMethodName)
        .expression.reference?.resolve()

      return accessorResolvant != null
    }
  }
}
