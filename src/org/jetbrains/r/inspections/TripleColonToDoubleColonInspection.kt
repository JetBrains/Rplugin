/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.RNamespaceAccessExpression
import org.jetbrains.r.psi.api.RVisitor
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStatement

/**
 * base:::print("Hello") -> base::print("Hello")
 */
class TripleColonToDoubleColonInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.triple.colon.to.double.colon.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitNamespaceAccessExpression(o: RNamespaceAccessExpression) {
      val dots = o.namespace.nextSibling
      if (dots.elementType != RElementTypes.R_TRIPLECOLON) return
      val targets = o.identifier?.reference?.multiResolve(false)
                      ?.map { it.element }
                      ?.filterIsInstance<RSkeletonAssignmentStatement>() ?: return
      if (targets.isNotEmpty() && targets.all { it.stub.exported }) {
        myProblemHolder.registerProblem(dots, RBundle.message("inspection.triple.colon.to.double.colon.description"),
                                        ProblemHighlightType.WARNING, TripleColonToDoubleColonQuickFix)
      }
    }
  }

  private object TripleColonToDoubleColonQuickFix : LocalQuickFix {
    override fun getFamilyName() = RBundle.message("inspection.triple.colon.to.double.colon.fix.name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val o = descriptor.psiElement as LeafPsiElement
      o.replace(RElementFactory.createLeafFromText(project, "::"))
    }
  }
}

