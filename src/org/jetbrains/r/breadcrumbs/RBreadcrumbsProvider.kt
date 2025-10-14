/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.breadcrumbs

import com.intellij.lang.Language
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.psi.RPsiUtil
import com.intellij.r.psi.psi.api.RAssignmentStatement
import com.intellij.r.psi.psi.api.RFile
import com.intellij.r.psi.psi.api.RFunctionExpression
import com.intellij.r.psi.rmarkdown.RMarkdownLanguage
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFence
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile
import org.jetbrains.r.rmarkdown.RMarkdownPsiUtil

class RBreadcrumbsProvider : BreadcrumbsProvider {
  override fun getLanguages(): Array<Language> {
    return arrayOf(RLanguage.INSTANCE, RMarkdownLanguage)
  }

  override fun acceptElement(element: PsiElement): Boolean {
    return assignmentShouldBeShown(element) ||
           RPsiUtil.isSectionDivider(element) ||
           element is RFile ||
           element is MarkdownFile ||
           element is MarkdownCodeFence
  }

  private fun assignmentShouldBeShown(element: PsiElement) =
    element is RAssignmentStatement &&
    element.assignedValue is RFunctionExpression &&
    PsiTreeUtil.getParentOfType(element, RAssignmentStatement::class.java) == null // check for top-level

  override fun getElementInfo(element: PsiElement): String {
    return when (element) {
      is RAssignmentStatement -> element.name
      is PsiComment -> RPsiUtil.extractNameFromSectionComment(element)
      is RFile -> element.name
      is MarkdownFile -> element.name
      is MarkdownCodeFence -> RMarkdownPsiUtil.getExecutableFenceLabel(element)
      else -> "unknown"
    }
  }

  override fun getElementTooltip(element: PsiElement): String {
    return RBundle.message("breadcrumbs.navigate.tooltip")
  }
}
