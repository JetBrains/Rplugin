/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RFunctionExpression
import com.intellij.r.psi.roxygen.psi.api.RoxygenIdentifierExpression
import com.intellij.r.psi.roxygen.psi.api.RoxygenNamespaceAccessExpression

fun RoxygenIdentifierExpression.isNamespaceAccess(): Boolean {
  return parent?.let { it is RoxygenNamespaceAccessExpression && it.identifier == this } == true
}

object RoxygenUtil {
  const val DOCUMENTATION_COMMENT_PREFIX = "#'"

  fun findHostComment(roxygenPsiElement: PsiElement): PsiComment? {
    val langMgr = InjectedLanguageManager.getInstance(roxygenPsiElement.project)
    val host = langMgr.getInjectionHost(roxygenPsiElement)
    return host as? PsiComment
  }

  private fun findAssociatedExpression(roxygenPsiElement: PsiElement): RExpression? {
    val psiComment = findHostComment(roxygenPsiElement)
    if (psiComment is PsiDocCommentBase) {
      val owner = psiComment.owner
      return owner as? RExpression
    }
    return null
  }

  fun findAssociatedFunction(roxygenPsiElement: PsiElement): RFunctionExpression? {
    return when (val associatedExpression = findAssociatedExpression(roxygenPsiElement)) {
      is RAssignmentStatement -> associatedExpression.assignedValue as? RFunctionExpression
      is RFunctionExpression -> associatedExpression
      else -> null
    }
  }
}