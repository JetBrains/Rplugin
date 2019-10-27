/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiPolyVariantReference
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*

/**
 * paste('a', 'b', sep='') -> paste0('a', 'b')
 */
class PasteToPaste0Inspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.paste2paste0.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private fun isEmptySepArgument(arg: RExpression): Boolean {
    val name = RPsiUtil.getArgumentName(arg) ?: return false
    val value = (arg as RAssignmentStatement).assignedValue?.text ?: return false
    return name == "sep" && (value == "''" || value == "\"\"")
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitCallExpression(o: RCallExpression) {
      val expression = o.expression
      val (name, namespaceName, reference) = when (expression) {
        is RIdentifierExpression -> Triple(expression.name, "", expression.reference)
        is RNamespaceAccessExpression -> Triple(expression.identifier?.name, expression.namespaceName, expression.identifier?.reference)
        else -> return
      }

      if (namespaceName.isNotEmpty() && namespaceName != "base") return
      if (name != "paste") return
      if (reference is PsiPolyVariantReference) {
        val refs = reference.multiResolve(false).mapNotNull { it.element }
        if (refs.all { !RPsiUtil.isLibraryElement(it) }) return
      }
      else {
        val ref = reference?.resolve()
        if (ref != null && !RPsiUtil.isLibraryElement(ref)) return
      }
      if (o.argumentList.expressionList.firstOrNull { isEmptySepArgument(it) } != null) {
        myProblemHolder.registerProblem(o, RBundle.message("inspection.paste2paste0.description"),
                                        ProblemHighlightType.WEAK_WARNING, PasteQuickFix(namespaceName.isNotEmpty()))
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

