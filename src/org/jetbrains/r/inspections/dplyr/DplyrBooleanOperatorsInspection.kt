/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections.dplyr

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.inspections.RInspection
import org.jetbrains.r.psi.RDplyrAnalyzer
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.ROperator
import org.jetbrains.r.psi.api.ROperatorExpression
import org.jetbrains.r.psi.api.RVisitor

/**
 * Flag &&, || inside of dplyr expressions.
 * &, | should be used instead.
 */
class DplyrBooleanOperatorsInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.dplyrBooleanOperator.name")

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession
  ): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitOperator(o: ROperator) {
      val name = o.name
      if (name != "&&" && name != "||") return
      val runtimeInfo = o.containingFile.runtimeInfo
      val dplyrContextInfo = RDplyrAnalyzer.getContextInfo(o, runtimeInfo) ?: return
      if (dplyrContextInfo.currentArgumentIsTable) return
      myProblemHolder.registerProblem(o, RBundle.message("inspection.message.non.vector.logical.operator.used.in.dplyr.expression"),
                                      ProblemHighlightType.WARNING, MyQuickFix(name))
    }
  }

  private class MyQuickFix(val operatorName: String) : LocalQuickFix {
    override fun getFamilyName() = CommonQuickFixBundle.message("fix.replace.x.with.y", operatorName, operatorName[0])

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val o = descriptor.psiElement as ROperator
      val replacement = RElementFactory.createRPsiElementFromText(project, "x ${operatorName.substring(0, 1)} y")
      o.replace((replacement as ROperatorExpression).operator!!)
    }
  }
}

