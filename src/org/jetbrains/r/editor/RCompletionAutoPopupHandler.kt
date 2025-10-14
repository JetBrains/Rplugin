/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.r.psi.RFileType
import com.intellij.r.psi.parsing.RElementTypes
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RListSubsetOperator
import com.intellij.r.psi.psi.api.RNamespaceAccessExpression
import com.intellij.r.psi.psi.api.RStringLiteralExpression
import com.intellij.r.psi.rmarkdown.RMarkdownFileType
import com.intellij.r.psi.roxygen.RoxygenFileType
import org.jetbrains.r.settings.REditorSettings

private const val identifierPrefixLength = 2

class RCompletionAutoPopupHandler : TypedHandlerDelegate() {

  override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
    if (file.fileType !in listOf(RFileType, RoxygenFileType) && file.virtualFile?.fileType != RMarkdownFileType) return Result.CONTINUE
    val offset = editor.caretModel.offset

    if (identifierPart(charTyped)) {
      if (!REditorSettings.disableCompletionAutoPopupForShortPrefix) {
        return Result.CONTINUE
      }
      if (offset < identifierPrefixLength) {
        return Result.STOP
      }
      val content = editor.document.charsSequence
      return if (identifierPart(content[offset - 1]) && identifierPart(content[offset - 2])) {
        Result.CONTINUE
      } else {
        Result.STOP
      }
    }

    if (charTyped != '$' && charTyped != ':' && charTyped != '/' && charTyped != '\\') return Result.CONTINUE
    AutoPopupController.getInstance(project).scheduleAutoPopup(editor) l@{ psiFile ->
      val element = psiFile.findElementAt(offset) ?: return@l false
      val parent = element.parent ?: return@l false
      if (parent is RNamespaceAccessExpression && parent.identifier == null) return@l true
      if (isSubsetOperator(element)) return@l true
      if (parent is RStringLiteralExpression && charTyped == '/' || charTyped == '\\') return@l true
      return@l false
    }
    return Result.STOP
  }

  private fun isSubsetOperator(element: PsiElement): Boolean {
    if (element.node.elementType != RElementTypes.R_LIST_SUBSET) return false
    return if (element.parent is RListSubsetOperator) {
      element.parent.prevSibling is RExpression
    }
    else {
      element.prevSibling is RExpression
    }
  }

  private fun showPopup(project: Project, editor: Editor): Result {
    AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
    return Result.STOP
  }
}

private fun identifierPart(charTyped: Char) = Character.isLetterOrDigit(charTyped) || charTyped == '_' || charTyped == '.'
