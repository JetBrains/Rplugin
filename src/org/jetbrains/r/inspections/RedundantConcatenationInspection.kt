/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RNamedArgument
import org.jetbrains.r.psi.api.RVisitor
import org.jetbrains.r.psi.isFunctionFromLibrary

/**
 * c() -> NULL
 *
 * c(x) -> x
 */
class RedundantConcatenationInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.redundant.concatenation.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitCallExpression(o: RCallExpression) {
      if (!o.isFunctionFromLibrary("c", "base")) return
      if (o.argumentList.expressionList.size > 1) return
      if (o.argumentList.expressionList.any { it is RNamedArgument || it.text == "..." }) return
      myProblemHolder.registerProblem(o, RBundle.message("inspection.redundant.concatenation.description"),
                                      ProblemHighlightType.WEAK_WARNING, RedundantConcatenationQuickFix)
    }
  }

  private object RedundantConcatenationQuickFix : LocalQuickFix {
    override fun getFamilyName() = RBundle.message("inspection.redundant.concatenation.fix.name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val element = descriptor.psiElement as RCallExpression
      val replacement = element.argumentList.expressionList.firstOrNull()
      if (replacement == null) {
        element.replace(RElementFactory.createRPsiElementFromText(project, "NULL"))
      } else {
        element.replace(replacement)
      }
    }
  }
}

