/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.r.psi.psi.api.*

object RPrecedenceUtil {
  private const val LITERAL_PRECEDENCE = 0
  private const val NAMESPACE_ACCESS_PRECEDENCE = 1
  private const val NAMESPACE_MEMBER_PRECEDENCE = 2
  private const val INDEXING_PRECEDENCE = 3
  // ^                4
  // unary - +        5
  // :                6
  private const val SPECIAL_OPERATOR_PRECEDENCE = 7
  // * /              8
  // + -              9
  // < > <= >= == !=  10
  // !                11
  // & &&             12
  // | ||             13
  private const val TILDE_PRECEDENCE = 14
  private const val RIGHT_ASSIGNMENT_PRECEDENCE = 15
  private const val LEFT_ASSIGNMENT_PRECEDENCE = 16
  private const val EQUAL_ASSIGNMENT_PRECEDENCE = 17

  private val BINARY_OPERATOR_PRECEDENCE = mapOf(
    Pair("@", NAMESPACE_MEMBER_PRECEDENCE),
    Pair("^", 4),
    Pair(":", 6),
    Pair("*", 8), Pair("/", 8),
    Pair("+", 9), Pair("-", 9),
    Pair("<", 10), Pair(">", 10), Pair("<=", 10), Pair(">=", 10), Pair("==", 10), Pair("!=", 10),
    Pair("&", 12), Pair("&&", 12),
    Pair("|", 13), Pair("||", 13)
  )
  private val UNARY_OPERATOR_PRECEDENCE = mapOf(
    Pair("+", 5), Pair("-", 5),
    Pair("!", 11)
  )

  private fun getPrecedence(expr: PsiElement): Int {
    return when (expr) {
      is RNamespaceAccessExpression -> NAMESPACE_ACCESS_PRECEDENCE
      is RMemberExpression -> NAMESPACE_MEMBER_PRECEDENCE
      is RSubscriptionExpression -> INDEXING_PRECEDENCE
      is RTildeExpression -> TILDE_PRECEDENCE
      is RAssignmentStatement -> {
        when {
          expr.isRight -> RIGHT_ASSIGNMENT_PRECEDENCE
          expr.isLeft -> LEFT_ASSIGNMENT_PRECEDENCE
          else -> EQUAL_ASSIGNMENT_PRECEDENCE
        }
      }
      is ROperatorExpression -> {
        val name = expr.operator?.name ?: return -1
        if (expr.isBinary) {
          BINARY_OPERATOR_PRECEDENCE[name] ?: SPECIAL_OPERATOR_PRECEDENCE
        } else {
          UNARY_OPERATOR_PRECEDENCE[name] ?: SPECIAL_OPERATOR_PRECEDENCE
        }
      }
      else -> LITERAL_PRECEDENCE
    }
  }

  fun needParenthesisToParent(expr: PsiElement): Boolean {
    val precedence = getPrecedence(expr)
    val parent = expr.parent
    val parentPrecedence = getPrecedence(parent)
    if (parentPrecedence == LITERAL_PRECEDENCE) return false
    if (parentPrecedence == INDEXING_PRECEDENCE && expr != (parent as RSubscriptionExpression).expressionList[0]) return false
    if (parentPrecedence > precedence) return false
    if (parentPrecedence == precedence) {
      return when (parent) {
        is ROperatorExpression -> expr == parent.rightExpr
        is RNamespaceAccessExpression -> expr == parent.identifier
        is RMemberExpression -> true
        is RSubscriptionExpression -> true
        is RTildeExpression -> expr != parent.expressionList[0]
        is RAssignmentStatement -> expr == if (parent.isRight) parent.assignee else parent.assignedValue
        else -> false
      }
    }
    return true
  }

  fun wrapToParenthesis(expr: PsiElement, project: Project): RParenthesizedExpression {
    val replacement = RElementFactory.createRPsiElementFromText(project, "(x)") as RParenthesizedExpression
    replacement.expression!!.replace(expr.copy())
    return expr.replace(replacement) as RParenthesizedExpression
  }

  fun wrapToParenthesisIfNeeded(expr: RExpression, project: Project): RExpression {
    return if (needParenthesisToParent(expr)) wrapToParenthesis(expr, project) else expr
  }
}