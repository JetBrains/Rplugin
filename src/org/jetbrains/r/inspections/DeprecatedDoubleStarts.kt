/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RExpOperator
import org.jetbrains.r.psi.api.RVisitor

/**
 * <code>x ** y</code> is a deprecated form, use <code>x ^ y</code>
 */
class DeprecatedDoubleStarts : RInspection() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitExpOperator(expOperator: RExpOperator) {
      if (expOperator.text == "**") {
        myProblemHolder.registerProblem(expOperator,
                                        RBundle.message("inspection.deprecated.double.starts.description"),
                                        ProblemHighlightType.WARNING, DoubleStartsQuickFix)
      }
    }
  }

  private object DoubleStartsQuickFix : LocalQuickFix {
    override fun getFamilyName() = RBundle.message("inspection.deprecated.double.starts.fix.name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      descriptor.psiElement.replace(RElementFactory.createLeafFromText(project, "^"))
    }
  }
}

