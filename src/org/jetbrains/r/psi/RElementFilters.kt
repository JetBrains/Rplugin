/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.filters.AndFilter
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.NotFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.psi.api.RArgumentList
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RIdentifierExpression
import org.jetbrains.r.psi.api.RStringLiteralExpression

object RElementFilters {
  val IMPORT_FILTER = ImportFilter()
  val MEMBER_ACCESS_FILTER = FilterPattern(MemberFilter())
  val IMPORT_CONTEXT = FilterPattern(IMPORT_FILTER)
  val IDENTIFIER_FILTER = FilterPattern(AndFilter(IdentifierFilter(), NotFilter(IMPORT_FILTER)))
  val NAMESPACE_REFERENCE_FILTER = FilterPattern(RNamespaceAccessExpressionFilter())
  val IDENTIFIER_OR_STRING_FILTER = FilterPattern(IdentifierOrStringFilter())
  val STRING_FILTER = FilterPattern(StringFilter())
}

class MemberFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val memberExpression = PsiTreeUtil.getParentOfType(context, org.jetbrains.r.psi.api.RMemberExpression::class.java, false) ?: return false
    return !(PsiTreeUtil.isAncestor(memberExpression.leftExpr, context ?: return false, false))

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
    val expression = PsiTreeUtil.getParentOfType(context, org.jetbrains.r.psi.api.RExpression::class.java, false)
    return expression is RIdentifierExpression && expression.isNamespaceAccess()
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class IdentifierFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, org.jetbrains.r.psi.api.RExpression::class.java, false)
    if (expression?.parent is RNoCommaTail) return false
    return expression is RIdentifierExpression && !expression.isNamespaceAccess() && !RPsiUtil.isFieldLikeComponent(expression)
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class NamedArgumentFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, org.jetbrains.r.psi.api.RExpression::class.java, false)
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
