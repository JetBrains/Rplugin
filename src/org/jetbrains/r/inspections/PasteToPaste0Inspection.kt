/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.psi.RElementFactory
import com.intellij.r.psi.psi.api.RCallExpression
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RNamedArgument
import com.intellij.r.psi.psi.api.RNamespaceAccessExpression
import com.intellij.r.psi.psi.api.RVisitor
import com.intellij.r.psi.psi.isFunctionFromLibrary

/**
 * paste('a', 'b', sep='') -> paste0('a', 'b')
 */
class PasteToPaste0Inspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.paste2paste0.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private fun isEmptySepArgument(arg: RExpression): Boolean {
    val name = (arg as? RNamedArgument)?.name ?: return false
    val value = arg.assignedValue?.text ?: return false
    return name == "sep" && (value == "''" || value == "\"\"")
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitCallExpression(o: RCallExpression) {
      if (!o.isFunctionFromLibrary("paste", "base")) return
      if (o.argumentList.expressionList.firstOrNull { isEmptySepArgument(it) } != null) {
        myProblemHolder.registerProblem(o, RBundle.message("inspection.paste2paste0.description"),
                                        ProblemHighlightType.WEAK_WARNING, PasteQuickFix(o.expression is RNamespaceAccessExpression))
      }
    }
  }

  private inner class PasteQuickFix(private val withNamespaceAccess: Boolean) : LocalQuickFix {
    override fun getFamilyName() = RBundle.message("inspection.paste2paste0.fix.name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val o = descriptor.psiElement as RCallExpression
      val args = o.argumentList.expressionList.filter { !isEmptySepArgument(it) }
      val replacement = RElementFactory
        .createRPsiElementFromText(project, (if (withNamespaceAccess) "base::" else "") +
                                            "paste0(${args.joinToString(separator = ", ", transform = { it.text })})")
      o.replace(replacement)
    }
  }
}

