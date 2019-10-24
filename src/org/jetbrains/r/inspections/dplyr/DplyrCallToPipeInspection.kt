/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections.dplyr

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.inspections.RInspection
import org.jetbrains.r.psi.RDplyrUtil
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPrecedenceUtil
import org.jetbrains.r.psi.api.*

/**
 * Replace calls of dplyr functions with pipe
 * E.g. summarise(group_by(table, year), s = sum(x)) -> table %>% group_by(year) %>% summarise(s = sum(x))
 */
class DplyrCallToPipeInspection : RInspection() {
  override fun getDisplayName() = "Replace dplyr calls with pipe"

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession
  ): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitCallExpression(o: RCallExpression) {
      val depth = getDepth(o)
      if (depth < MINIMAL_DEPTH_FOR_WARNING || RDplyrUtil.isPipeCall(o)) return

      val parent = o.parent
      if (parent is RArgumentList && o == parent.expressionList[0]) {
        val parentCall = parent.parent as RCallExpression
        if (isDplyrCallWithTable(parentCall) && !RDplyrUtil.isPipeCall(parentCall)) {
          return
        }
      }

      myProblemHolder.registerProblem(o, "Dplyr function calls can be replaced with pipe",
                                      ProblemHighlightType.WEAK_WARNING, MyQuickFix)
    }

    private fun getDepth(o: RCallExpression): Int {
      if (!isDplyrCallWithTable(o)) return 0
      val table = (o.argumentList.expressionList.getOrNull(0) ?: return 0) as? RCallExpression ?: return 1
      return 1 + getDepth(table)
    }
  }

  private object MyQuickFix : LocalQuickFix {
    override fun getFamilyName() = "Replace function calls with pipe"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      var expr = descriptor.psiElement as RCallExpression

      while (true) {
        val args = expr.argumentList.expressionList
        if (args.size == 0) {
          break
        }
        val replacementText = "t ${RDplyrUtil.PIPE_OPERATOR} f(${generateSequence { "x" }.take(args.size - 1).joinToString(", ")})"
        val replacement = RElementFactory.createRPsiElementFromText(project, replacementText) as ROperatorExpression
        replacement.leftExpr!!.replace(args[0])
        val replacementCall = replacement.rightExpr as RCallExpression
        replacementCall.expression.replace(expr.expression)
        val replacementArgs = replacementCall.argumentList.expressionList
        for (i in 0 until replacementArgs.size) {
          replacementArgs[i].replace(args[i + 1])
        }

        var replacedExpr = expr.replace(replacement)
        if (RPrecedenceUtil.needParenthesisToParent(replacedExpr)) {
          replacedExpr = RPrecedenceUtil.wrapToParenthesis(replacedExpr, project).expression
        }
        val newExpr = (replacedExpr as ROperatorExpression).leftExpr ?: break
        if (!isDplyrCallWithTable(newExpr)) break
        expr = newExpr as RCallExpression
      }

      if (RPrecedenceUtil.needParenthesisToParent(expr)) {
        RPrecedenceUtil.wrapToParenthesis(expr, project).expression
      }
    }
  }

  companion object {
    private const val MINIMAL_DEPTH_FOR_WARNING = 2

    private fun isDplyrCallWithTable(expr: RExpression): Boolean {
      if (expr !is RCallExpression) return false
      val call = RDplyrUtil.getCallInfo(expr) ?: return false
      val runtimeInfo = expr.containingFile.runtimeInfo
      return call.function.haveTableArguments(call.psiCall, call.arguments, runtimeInfo) && call.function.tableArguments != 0
    }
  }
}

