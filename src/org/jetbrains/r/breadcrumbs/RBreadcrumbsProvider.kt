/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.breadcrumbs

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import icons.org.jetbrains.r.RBundle
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile
import org.jetbrains.r.RLanguage
import org.jetbrains.r.psi.api.RAssignmentStatement
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.rmarkdown.RMarkdownLanguage
import org.jetbrains.r.rmarkdown.RMarkdownPsiUtil

class RBreadcrumbsProvider : BreadcrumbsProvider {
  override fun getLanguages(): Array<Language> {
    return arrayOf(RLanguage.INSTANCE, RMarkdownLanguage)
  }

  override fun acceptElement(element: PsiElement): Boolean {
    return element is RAssignmentStatement || element is RFile || element is MarkdownFile || element is MarkdownCodeFenceImpl
  }

  override fun getElementInfo(element: PsiElement): String {
    return when (element) {
      is RAssignmentStatement -> element.name
      is RFile -> element.name
      is MarkdownFile -> element.name
      is MarkdownCodeFenceImpl -> RMarkdownPsiUtil.getExecutableFenceLabel(element)
      else -> "unknown"
    }
  }

  override fun getElementTooltip(element: PsiElement): String? {
    return RBundle.message("breadcrumbs.navigate.tooltip")
  }
}
