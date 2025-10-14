/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.psi.RElementFactory
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.ROperator
import com.intellij.r.psi.psi.api.RVisitor

/**
 * a = 3 -> a <- 3
 */
class AssignmentInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.assignment.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitAssignmentStatement(o: RAssignmentStatement) {
      if (!o.isEqual) return
      myProblemHolder.registerProblem(o.children.first { it is ROperator },
                                      RBundle.message("inspection.assignment.description"),
                                      ProblemHighlightType.WARNING, AssignmentQuickFix)
    }
  }

  private object AssignmentQuickFix : LocalQuickFix {
    override fun getFamilyName() = RBundle.message("inspection.assignment.fix.name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val operator = (descriptor.psiElement as ROperator).firstChild
      operator.replace(RElementFactory.createLeafFromText(project, "<-"))
    }
  }
}

