/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.roxygen.isNamespaceAccess
import com.intellij.r.psi.roxygen.psi.api.RoxygenIdentifierExpression
import com.intellij.r.psi.roxygen.psi.api.RoxygenParameter
import com.intellij.r.psi.roxygen.psi.api.RoxygenTag

object RoxygenElementFilters {
  val TAG_NAME_FILTER = FilterPattern(TagNameFilter())
  val PARAMETER_FILTER = FilterPattern(ParameterFilter())
  val NAMESPACE_ACCESS_FILTER = FilterPattern(NamespaceAccessExpressionFilter())
  val IDENTIFIER_FILTER = FilterPattern(IdentifierFilter())
}

class TagNameFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val tag = PsiTreeUtil.getParentOfType(context, RoxygenTag::class.java, false) ?: return false
    return PsiTreeUtil.isAncestor(tag.firstChild, context ?: return false, false)
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class ParameterFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    return PsiTreeUtil.getParentOfType(context, RoxygenParameter::class.java, false) != null
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class NamespaceAccessExpressionFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, RoxygenIdentifierExpression::class.java, false)
    return expression is RoxygenIdentifierExpression && expression.isNamespaceAccess()
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}

class IdentifierFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, RoxygenIdentifierExpression::class.java, false)
    return expression is RoxygenIdentifierExpression && !expression.isNamespaceAccess()
  }

  override fun isClassAcceptable(hintClass: Class<*>?) = true
}
