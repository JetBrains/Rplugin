/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.RFileType
import org.jetbrains.r.psi.api.*

internal object REditorActionUtil {
  data class SelectedCode(val code: String, val file: VirtualFile, val range: TextRange)

  fun getSelectedCode(e: AnActionEvent): SelectedCode? {
    val editor = e.getData(PlatformDataKeys.EDITOR) ?: return null

    val project = editor.project ?: throw RuntimeException("no project in $e")

    val code = editor.selectionModel.selectedText
    val virtualFile = e.virtualFile ?: return null
    if (code != null && !StringUtil.isEmptyOrSpaces(code)) {
      return SelectedCode(code, virtualFile, TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd))
    }
    val psiFile = e.psiFile ?: return null

    val lineNumber = editor.document.getLineNumber(editor.caretModel.offset)
    val lineStart = editor.document.getLineStartOffset(lineNumber)
    val lineEnd = editor.document.getLineEndOffset(lineNumber)
    var elementStart: PsiElement
    var elementEnd: PsiElement
    if (psiFile.fileType == RFileType) {
      elementStart = psiFile.findElementAt(lineStart) ?: return null
      elementEnd = psiFile.findElementAt(lineEnd - 1) ?: return null
    } else {
      elementStart = InjectedLanguageManager.getInstance(project).findInjectedElementAt(psiFile, lineStart) ?: return null
      elementEnd = InjectedLanguageManager.getInstance(project).findInjectedElementAt(psiFile, lineEnd) ?: return null
    }
    elementStart = elementStart.takeIf { !StringUtil.isEmptyOrSpaces(it.text) } ?: PsiTreeUtil.nextVisibleLeaf(elementStart) ?: return null
    elementEnd = elementEnd.takeIf { !StringUtil.isEmptyOrSpaces(it.text) } ?: PsiTreeUtil.prevVisibleLeaf(elementEnd) ?: return null
    if (elementStart.textOffset > elementEnd.textOffset) return null
    val element = PsiTreeUtil.findCommonParent(elementStart, elementEnd)

    // grow until we reach expression barrier
    val evalElement = PsiTreeUtil.findFirstParent(element) { psiElement ->
      // avoid that { would be evaluated if cursor is set after block
      if (psiElement is LeafPsiElement) return@findFirstParent false

      val parent = psiElement.parent
      parent is RBlockExpression ||
      parent is RWhileStatement ||
      parent is RForStatement ||
      parent is RIfStatement ||
      parent is RFile
    } ?: return null

    if (StringUtil.isEmptyOrSpaces(evalElement.text)) return null
    val result = SelectedCode(evalElement.text, virtualFile, evalElement.textRange)

    // set caret to next downstream element
    // todo add preference for caret transition after eval
    val nextSibling = PsiTreeUtil.getNextSiblingOfType(evalElement, RExpression::class.java)
    if (nextSibling != null) {
      val siblingEndPos = nextSibling.textOffset + nextSibling.textLength
      editor.caretModel.currentCaret.moveToOffset(siblingEndPos)
    }

    return result
  }
}