/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.ROperator
import com.intellij.r.psi.psi.api.RVisitor

class ClosureAssignmentInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.closure.assignment.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitAssignmentStatement(o: RAssignmentStatement) {
      if (!o.isClosureAssignment) return
      val operator = PsiTreeUtil.findChildOfType(o, ROperator::class.java)!!
      myProblemHolder.registerProblem(operator,
                                      RBundle.message("inspection.closure.assignment.description", operator.text),
                                      ProblemHighlightType.WEAK_WARNING)
    }
  }
}

