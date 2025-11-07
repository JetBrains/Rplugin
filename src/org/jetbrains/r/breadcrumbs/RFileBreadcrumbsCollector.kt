/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.breadcrumbs

import com.intellij.lang.LanguageUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.rmarkdown.RMarkdownLanguage
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import com.intellij.ui.components.breadcrumbs.Crumb
import com.intellij.xml.breadcrumbs.CrumbPresentation

class RFileBreadcrumbsCollector(project: Project) : CommonPsiFileBreadcrumbsCollector(project) {
  override fun handlesFile(virtualFile: VirtualFile): Boolean {
    if (!virtualFile.isValid) return false
    val language = LanguageUtil.getFileLanguage(virtualFile)
    return language == RLanguage.INSTANCE || language == RMarkdownLanguage
  }

  override fun createCrumb(element: PsiElement, provider: BreadcrumbsProvider, presentation: CrumbPresentation?): Crumb {
    return RPsiCrumb(element, provider, presentation)
  }
}