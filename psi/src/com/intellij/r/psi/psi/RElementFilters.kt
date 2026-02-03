/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.filters.AndFilter
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.NotFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.r.psi.classes.r6.context.R6ContextProvider
import com.intellij.r.psi.classes.s4.context.RS4ContextProvider
import com.intellij.r.psi.parsing.RElementTypes
import com.intellij.r.psi.psi.api.RArgumentList
import com.intellij.r.psi.psi.api.RAtExpression
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RIdentifierExpression
import com.intellij.r.psi.psi.api.RInfixOperator
import com.intellij.r.psi.psi.api.RNoCommaTail
import com.intellij.r.psi.psi.api.RStringLiteralExpression

object RElementFilters {
  val IMPORT_FILTER = ImportFilter()
  val MEMBER_ACCESS_FILTER = FilterPattern(MemberFilter())
  val AT_ACCESS_FILTER = FilterPattern(AtFilter())
  val IMPORT_CONTEXT = FilterPattern(IMPORT_FILTER)
  val IDENTIFIER_FILTER = FilterPattern(AndFilter(IdentifierFilter(), NotFilter(IMPORT_FILTER)))
  val OPERATOR_FILTER = FilterPattern(OperatorFilter())
  val NAMESPACE_REFERENCE_FILTER = FilterPattern(RNamespaceAccessExpressionFilter())
  val IDENTIFIER_OR_STRING_FILTER = FilterPattern(IdentifierOrStringFilter())
  val STRING_FILTER = FilterPattern(StringFilter())
  val S4_CONTEXT_FILTER = FilterPattern(S4ContextFilter())
  val R6_CONTEXT_FILTER = FilterPattern(R6ContextFilter())
  val STRING_EXCEPT_OTHER_LIBRARIES_CONTEXT_FILTER = FilterPattern(AndFilter(StringFilter(), NotFilter(S4ContextFilter())))
}

class MemberFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val memberExpression = PsiTreeUtil.getParentOfType(context, com.intellij.r.psi.psi.api.RMemberExpression::class.java, false) ?: return false
    return !(PsiTreeUtil.isAncestor(memberExpression.leftExpr, context ?: return false, false))

  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class AtFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val atExpression = PsiTreeUtil.getParentOfType(context, RAtExpression::class.java, false) ?: return false
    return !(PsiTreeUtil.isAncestor(atExpression.leftExpr, context ?: return false, false))
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class ImportFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, RExpression::class.java, false)
    val parent = expression?.parent
    return (parent is RArgumentList && parent.parent != null && RPsiUtil.isImportStatement(parent.parent))
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class RNamespaceAccessExpressionFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?) : Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, com.intellij.r.psi.psi.api.RExpression::class.java, false)
    return expression is RIdentifierExpression && expression.isNamespaceAccess()
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class IdentifierFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, com.intellij.r.psi.psi.api.RExpression::class.java, false)
    if (expression?.parent is RNoCommaTail) return false
    return expression is RIdentifierExpression && !expression.isNamespaceAccess() && !RPsiUtil.isFieldLikeComponent(expression)
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class OperatorFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    if (context.elementType == RElementTypes.R_INFIX_OP) return true
    val expression = PsiTreeUtil.getParentOfType(context, RInfixOperator::class.java, false)
    val prevSibling = context?.prevSibling
    return expression != null || prevSibling != null && prevSibling.text.startsWith("%")
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class NamedArgumentFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, com.intellij.r.psi.psi.api.RExpression::class.java, false)
    return expression?.parent is RArgumentList
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class IdentifierOrStringFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, RExpression::class.java, false)
    return expression is RIdentifierExpression && !expression.isNamespaceAccess() && !RPsiUtil.isFieldLikeComponent(expression) ||
           expression is RStringLiteralExpression
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class StringFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, RExpression::class.java, false)
    return expression is RStringLiteralExpression
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class S4ContextFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, RExpression::class.java, false) ?: return false
    return RS4ContextProvider.getS4Context(expression) != null
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class R6ContextFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, RExpression::class.java, false) ?: return false
    return R6ContextProvider.getR6Context(expression) != null
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}