/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.api.*

/**
 * Find boolean expressions that can be simplified (e.g. (x && TRUE), (!!y))
 */
class BooleanExpressionSimplifier : RInspection() {
  override fun getDisplayName() = "Boolean expression can be simplified"

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession
  ): PsiElementVisitor {
    return Visitor(holder)
  }

  private inner class Visitor(private val myProblemHolder: ProblemsHolder) : RVisitor() {
    override fun visitExpression(expr: RExpression) {
      if (!isBooleanOperator(expr)) return
      if (isBooleanOperator(PsiTreeUtil.skipParentsOfType(expr, RParenthesizedExpression::class.java) as? RExpression)) return
      class ReplaceHappened : Throwable()
      try {
        simplify(expr, { _, _ -> throw ReplaceHappened() }, { throw ReplaceHappened() })
      } catch (_: ReplaceHappened) {
        myProblemHolder.registerProblem(expr, "Boolean expression can be simplified", myQuickFix)
      }
    }
  }

  private class MyQuickFix : LocalQuickFix {
    override fun getFamilyName() = "Simplify"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      var expr = descriptor.psiElement as RExpression

      val replace = { from: RExpression, to: RExpression ->
        val result = from.replace(to) as RExpression
        if (expr === from) {
          expr = result
        }
        result
      }

      val genFromText = { text: String ->
        RElementFactory.createRPsiElementFromText(project, text) as RExpression
      }

      simplify(expr, replace, genFromText)
      fixParenthesisRecursive(expr, replace, genFromText)
    }
  }

  private val myQuickFix = MyQuickFix()
}

private val OPERATOR_PRIORITY = mapOf(
  Pair("||", 0), Pair("|", 0),
  Pair("&&", 1), Pair("&", 1),
  Pair("!", 2)
)

private fun isBooleanOperator(operator: RExpression?): Boolean {
  if (operator !is ROperatorExpression) {
    return false
  }
  val name = operator.operator?.name ?: return false
  return OPERATOR_PRIORITY.containsKey(name)
}


private enum class RBooleanValue {
  NA {
    override fun toRLiteral() = "NA"
    override fun not() = NA

  },

  FALSE {
    override fun toRLiteral() = "FALSE"
    override fun not() = TRUE
  },

  TRUE {
    override fun toRLiteral() = "TRUE"
    override fun not() = FALSE
  };

  abstract fun toRLiteral(): String
  abstract fun not(): RBooleanValue
}

private data class SimplifyResult(val const: RBooleanValue?, val canBeDeleted: Boolean)

private val NULL_RESULT = SimplifyResult(null, true)

private fun checkCanBeDeleted(expr: RPsiElement): Boolean {
  return when (expr) {
    is RAssignmentStatement -> false
    is RBreakStatement -> false
    is RCallExpression -> false
    is RLoopStatement -> false
    is RNextStatement -> false
    is RFunctionExpression -> true
    else -> expr.children.all { it !is RExpression || checkCanBeDeleted(it) }
  }
}

private fun simplify(
  expr: RExpression,
  replace: (RExpression, RExpression) -> RExpression,
  genFromText: (String) -> RExpression
): SimplifyResult {
  return when (expr) {
    is RBooleanLiteral -> SimplifyResult(if (expr.isTrue) RBooleanValue.TRUE else RBooleanValue.FALSE, true)
    is RNaLiteral -> SimplifyResult(RBooleanValue.NA, true)
    is RParenthesizedExpression -> simplify(expr.expression ?: return NULL_RESULT, replace, genFromText)

    is ROperatorExpression -> {
      if (expr.isBinary) {
        simplifyBinaryOperator(expr, replace, genFromText)
      } else {
        simplifyUnaryOperator(expr, replace, genFromText)
      }
    }
    else -> SimplifyResult(null, checkCanBeDeleted(expr))
  }
}

private fun simplifyBinaryOperator(
  expr: ROperatorExpression,
  replace: (RExpression, RExpression) -> RExpression,
  genFromText: (String) -> RExpression
): SimplifyResult {
  val name = expr.operator?.name
  val c1 = when (expr.operator?.name) {
    "&&", "&" -> RBooleanValue.TRUE
    "||", "|" -> RBooleanValue.FALSE
    else -> return SimplifyResult(null, checkCanBeDeleted(expr))
  }
  val c0 = c1.not()
  val lazy = name?.length == 2

  val lhsResult = simplify(expr.leftExpr ?: return NULL_RESULT, replace, genFromText)
  val rhsResult = simplify(expr.rightExpr ?: return NULL_RESULT, replace, genFromText)
  val lhs = expr.leftExpr ?: return NULL_RESULT
  val rhs = expr.rightExpr ?: return NULL_RESULT

  fun tryReplace(to: RExpression, toResult: SimplifyResult, otherResult: SimplifyResult?): SimplifyResult? {
    return if (otherResult?.canBeDeleted != false) {
      replace(expr, to)
      toResult
    } else null
  }

  if (lhsResult.const == c0) return tryReplace(lhs, lhsResult, if (lazy) null else rhsResult) ?: SimplifyResult(c0, false)
  if (lhsResult.const == c1) return tryReplace(rhs, rhsResult, lhsResult) ?: SimplifyResult(rhsResult.const, false)
  if (rhsResult.const == c0) return tryReplace(rhs, rhsResult, lhsResult) ?: SimplifyResult(c0, false)
  if (rhsResult.const == c1) return tryReplace(lhs, lhsResult, rhsResult) ?: SimplifyResult(lhsResult.const, false)

  if (lhsResult.const == RBooleanValue.NA && rhsResult.const == RBooleanValue.NA) {
    if (lhsResult.canBeDeleted) {
      replace(expr, rhs)
      return rhsResult
    }
    if (rhsResult.canBeDeleted) {
      replace(expr, lhs)
      return lhsResult
    }
    return SimplifyResult(RBooleanValue.NA, false)
  }
  return SimplifyResult(null, lhsResult.canBeDeleted && rhsResult.canBeDeleted)
}

private fun simplifyUnaryOperator(
  expr: ROperatorExpression,
  replace: (RExpression, RExpression) -> RExpression,
  genFromText: (String) -> RExpression
): SimplifyResult {
  if (expr.operator?.name == "!") {
    val opResult = simplify(expr.expr ?: return NULL_RESULT, replace, genFromText)
    if (opResult.const != null) {
      if (opResult.canBeDeleted) {
        replace(expr, genFromText(opResult.const.not().toRLiteral()))
      }
    }
    return SimplifyResult(opResult.const?.not(), opResult.canBeDeleted)
  }
  return SimplifyResult(null, checkCanBeDeleted(expr))
}

fun skipParenthesized(expr: RExpression): RExpression? {
  return if (expr is RParenthesizedExpression)
    skipParenthesized(expr.expression ?: return null)
  else
    expr
}

private fun fixParenthesisRecursive(
  expr: RExpression,
  replace: (RExpression, RExpression) -> RExpression,
  genFromText: (String) -> RExpression
) {
  when (expr) {
    is RParenthesizedExpression -> {
      fixParenthesisRecursive(expr.expression ?: return, replace, genFromText)
      val op = expr.expression ?: return
      if (op is RBooleanLiteral || op is RNaLiteral ||
          op is RNumericLiteralExpression || op is RStringLiteralExpression ||
          op is RIdentifierExpression || op is RParenthesizedExpression) {
        replace(expr, op)
      }
    }

    is ROperatorExpression -> {
      if (expr.isBinary) {
        fixParenthesisRecursive(expr.leftExpr ?: return, replace, genFromText)
        fixParenthesisRecursive(expr.rightExpr ?: return, replace, genFromText)
      } else {
        fixParenthesisRecursive(expr.expr ?: return, replace, genFromText)
      }
      fixOperatorParentheses(expr, replace, genFromText)
    }
  }
}

private fun fixOperatorParentheses(
  expr: ROperatorExpression,
  replace: (RExpression, RExpression) -> RExpression,
  genFromText: (String) -> RExpression
) {
  if (expr.isBinary) {
    processChild(expr, expr.leftExpr, replace, genFromText)
    processChild(expr, expr.rightExpr, replace, genFromText, true)
  } else {
    processChild(expr, expr.expr, replace, genFromText)
  }
}

private fun processChild(
  expr: ROperatorExpression,
  exprChildParenthesized: RExpression?,
  replace: (RExpression, RExpression) -> RExpression,
  genFromText: (String) -> RExpression,
  isRight: Boolean = false
) {
  if (exprChildParenthesized !is RParenthesizedExpression) {
    return
  }
  val exprChild = exprChildParenthesized.expression as? ROperatorExpression ?: return
  val operator = expr.operator?.name ?: return
  val priority = OPERATOR_PRIORITY[operator] ?: return
  val operatorChild = exprChild.operator?.name ?: return
  val priorityChild = OPERATOR_PRIORITY[operatorChild] ?: return

  if (priorityChild > priority) {
    replace(exprChildParenthesized, exprChild)
    return
  }
  if (priorityChild < priority) {
    return
  }
  if (!isRight || (operator.length == 2 && operatorChild.length == 2)) {
    if (isRight) {
      // Tree rotation: exprA && [exprB && exprC] -> [exprA && exprB] && exprC
      val exprA = expr.leftExpr ?: return
      val exprB = exprChild.leftExpr ?: return
      val exprC = exprChild.rightExpr ?: return
      val replacementText = "a $operator b $operatorChild c"
      val replacement = genFromText(replacementText) as? ROperatorExpression ?: return
      val replacementLeft = replacement.leftExpr as? ROperatorExpression ?: return
      replace(replacementLeft.leftExpr ?: return, exprA)
      replace(replacementLeft.rightExpr ?: return, exprB)
      replace(replacement.rightExpr ?: return, exprC)
      replace(expr, replacement)
    } else {
      replace(exprChildParenthesized, exprChild)
    }
  }
}
