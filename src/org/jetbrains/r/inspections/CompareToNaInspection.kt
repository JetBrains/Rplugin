/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.*

/**
 * Flag (x == NA), (x != NA)
 * Replace to (is.na(x)), (!is.na(x))
 */
class CompareToNaInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.compareToNa.name")

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession
  ): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitOperatorExpression(o: ROperatorExpression) {
      val operator = o.operator?.name
      if (operator != "==" && operator != "!=") return
      val lhs = o.leftExpr ?: return
      val rhs = o.rightExpr ?: return
      if (lhs is RNaLiteral || rhs is RNaLiteral) {
        myProblemHolder.registerProblem(o, "Checking for NA should be done using is.na", ProblemHighlightType.WARNING, myQuickFix)
      }
    }
  }

  private class MyQuickFix : LocalQuickFix {
    override fun getFamilyName() = "Replace with is.na"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val o = descriptor.psiElement as ROperatorExpression
      val operator = o.operator?.name ?: return
      val lhs = o.leftExpr ?: return
      val rhs = o.rightExpr ?: return
      val expr = if (lhs is RNaLiteral) rhs else if (rhs is RNaLiteral) lhs else return

      val replacement: RExpression
      if (operator == "==") {
        replacement = RElementFactory.createRPsiElementFromText(project, "is.na(x)") as RCallExpression
        replacement.argumentList.expressionList[0].replace(expr)
      } else {
        replacement = RElementFactory.createRPsiElementFromText(project, "!is.na(x)") as ROperatorExpression
        (replacement.expr as RCallExpression).argumentList.expressionList[0].replace(expr)
      }
      o.replace(replacement)
    }
  }

  private val myQuickFix = MyQuickFix()
}

