/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.RBundle
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isFunctionFromLibrary

/**
 * 1:length(a) -> seq_along(a)
 *
 * 1:nrow(a) -> seq_len(nrow(a))
 */
class UnsafeSequenceInspection : RInspection() {
  override fun getDisplayName() = RBundle.message("inspection.unsafe.sequence.name")

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitOperatorExpression(o: ROperatorExpression) {
      if (o.operator !is RColonOperator) return
      val leftExpr = o.leftExpr ?: return
      if (leftExpr !is RNumericLiteralExpression || leftExpr.text != "1") return

      val rightExpr = o.rightExpr as? RCallExpression ?: return
      if (rightExpr.argumentList.expressionList.size != 1) return
      for (gen in RSequenceGenerator.values()) {
        if (gen.callCanBeReplaced(rightExpr)) {
          myProblemHolder.registerProblem(o, RBundle.message("inspection.unsafe.sequence.description"),
                                          ProblemHighlightType.WARNING, UnsafeSequenceQuickFix(gen))
          break
        }
      }
    }
  }

  private enum class RSequenceGenerator(val functionName: String, protected val replaceableFunctions: List<String>) {
    SEQ_ALONG("seq_along", listOf("length")) {
      override fun replaceCall(project: Project, call: RCallExpression): RPsiElement {
        val arg = call.argumentList.expressionList.first()
        return RElementFactory.createFuncallFromText(project, "$functionName(${arg.text})")
      }
    },
    SEQ_LEN("seq_len", listOf("nrow", "ncol", "NROW", "NCOL")) {
      override fun replaceCall(project: Project, call: RCallExpression): RPsiElement {
        return RElementFactory.createFuncallFromText(project, "$functionName(${call.text})")
      }
    };

    fun callCanBeReplaced(call: RCallExpression): Boolean {
      return replaceableFunctions.any { call.isFunctionFromLibrary(it, "base") }
    }

    abstract fun replaceCall(project: Project, call: RCallExpression): RPsiElement
  }

  private class UnsafeSequenceQuickFix(private val rGenerator: RSequenceGenerator) : LocalQuickFix {
    override fun getFamilyName() = RBundle.message("inspection.unsafe.sequence.fix.name", rGenerator.functionName)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val element = descriptor.psiElement as ROperatorExpression
      element.replace(rGenerator.replaceCall(project, element.rightExpr as RCallExpression))
    }
  }
}

