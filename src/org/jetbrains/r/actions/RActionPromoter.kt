/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.lang.LanguageUtil
import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.rmarkdown.RMarkdownLanguage

/**
 * Marker interface to distinguish R actions invoked in the editor from all other actions.
 */
interface RPromotedAction

class RActionPromoter : ActionPromoter {
  override fun promote(actions: List<AnAction>, context: DataContext): List<AnAction> {
    return if (isRContext(context)) actions.filter { it is RPromotedAction } else emptyList()
  }

  private fun isRContext(context: DataContext): Boolean {
    val editor = context.getData(CommonDataKeys.EDITOR)
    val document = editor?.document ?: return false
    val virtualFile = FileDocumentManager.getInstance().getFile(document)
    val language = LanguageUtil.getFileLanguage(virtualFile)
    return language == RLanguage.INSTANCE || language == RMarkdownLanguage
  }
}