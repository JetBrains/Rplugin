/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.elementType
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.psi.api.*

internal object REditorActionUtil {
  data class SelectedCode(val code: String, val file: VirtualFile, val range: TextRange)

  fun getSelectedCode(editor: Editor): SelectedCode? {
    val code = editor.selectionModel.selectedText
    val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
    if (code != null && !StringUtil.isEmptyOrSpaces(code)) {
      return SelectedCode(code, virtualFile, TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd))
    }
    val elementAtCaret = PsiUtilBase.getElementAtCaret(editor) ?: return null
    val skipFirstNL = if (elementAtCaret.elementType == RElementTypes.R_NL) elementAtCaret.prevSibling ?: return null else elementAtCaret
    val element = PsiTreeUtil.findFirstParent(skipFirstNL) { it is RExpression }
    val evalElement = PsiTreeUtil.findFirstParent(element) { psiElement ->
      if (element is RBlockExpression) {
        psiElement.parent?.let { it is RFile || it is RBlockExpression } == true
      } else when (val parent = psiElement.parent) {
        is RFunctionExpression -> parent.expression == psiElement
        is RWhileStatement -> parent.body == psiElement
        is RIfStatement -> parent.ifBody == psiElement || parent.elseBody == psiElement
        is RRepeatStatement -> parent.body == psiElement
        is RForStatement -> parent.body == psiElement
        else -> parent is RBlockExpression || parent is RFile
      }
    } ?: return null

    if (StringUtil.isEmptyOrSpaces(evalElement.text)) return null
    val result = SelectedCode(evalElement.text, virtualFile, evalElement.textRange)

    val nextSibling = PsiTreeUtil.getNextSiblingOfType(evalElement, RExpression::class.java)
    if (nextSibling != null) {
      val siblingEndPos = nextSibling.textOffset
      editor.caretModel.currentCaret.moveToOffset(siblingEndPos)
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }

    return result
  }

  fun isRunningCommand(console: RConsoleView?, allowDebug: Boolean = false): Boolean {
    if (console == null) return false
    return (!allowDebug && console.debugger.isEnabled) || console.isRunningCommand
  }

  fun isRunningCommand(project: Project?, allowDebug: Boolean = false): Boolean {
    if (project == null) return false
    return isRunningCommand(RConsoleManager.getInstance(project).currentConsoleOrNull, allowDebug)
  }
}