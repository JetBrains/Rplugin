/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.parsing.RElementTypes
import com.intellij.r.psi.psi.RPsiUtil
import com.intellij.r.psi.psi.api.RVisitor

/**
 * c(); -> c()
 */
class RedundantSemicolonInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.redundant.semicolon.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitElement(element: PsiElement) {
      if (element.elementType != RElementTypes.R_SEMI) return
      var nextSibling = element.nextSibling
      while (nextSibling is PsiWhiteSpace && !RPsiUtil.isWhitespaceWithNL(nextSibling) || nextSibling is PsiComment) {
        nextSibling = nextSibling.nextSibling
      }
      if (nextSibling != null && !RPsiUtil.isWhitespaceWithNL(nextSibling)) return
      myProblemHolder.registerProblem(element, RBundle.message("inspection.redundant.semicolon.description"),
                                      ProblemHighlightType.LIKE_UNUSED_SYMBOL, RedundantSemicolonQuickFix)
    }
  }

  private object RedundantSemicolonQuickFix : LocalQuickFix {
    override fun getFamilyName() = RBundle.message("inspection.redundant.semicolon.fix.name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val semi = descriptor.psiElement
      semi.delete()
    }
  }
}

