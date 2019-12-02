/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.breadcrumbs

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import com.intellij.ui.components.breadcrumbs.Crumb
import com.intellij.xml.breadcrumbs.CrumbPresentation
import org.jetbrains.r.RLanguage
import org.jetbrains.r.rmarkdown.RMarkdownLanguage

class RFileBreadcrumbsCollector(private val project: Project) : CommonPsiFileBreadcrumbsCollector(project) {
  override fun handlesFile(virtualFile: VirtualFile): Boolean {
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return false
    return psiFile.language == RLanguage.INSTANCE || psiFile.language == RMarkdownLanguage
  }

  override fun createCrumb(element: PsiElement, provider: BreadcrumbsProvider, presentation: CrumbPresentation?): Crumb {
    return RPsiCrumb(element, provider, presentation)
  }
}