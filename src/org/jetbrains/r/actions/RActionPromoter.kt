/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.rmarkdown.RMarkdownLanguage

/**
 * Marker interface to distinguish R actions invoked in the editor from all other actions.
 */
interface RPromotedAction

class RActionPromoter : ActionPromoter {
  override fun promote(actions: MutableList<AnAction>, context: DataContext): List<AnAction> {
    return if (isRContext(context)) actions.filter { it is RPromotedAction } else emptyList<AnAction>()
  }

  private fun isRContext(context: DataContext): Boolean {
    val editor = context.getData(CommonDataKeys.EDITOR)
    val document = editor?.document ?: return false
    val project = context.getData(CommonDataKeys.PROJECT) ?: return false
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
    val language = psiFile?.language
    return language == RLanguage.INSTANCE || language == RMarkdownLanguage
  }
}