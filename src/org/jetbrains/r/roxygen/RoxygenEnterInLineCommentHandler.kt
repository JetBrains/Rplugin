/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.codeInsight.editorActions.EnterHandler
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.jetbrains.r.roxygen.parsing.RoxygenElementTypes.ROXYGEN_DOC_PREFIX
import org.jetbrains.r.roxygen.parsing.RoxygenElementTypes.ROXYGEN_WS

class RoxygenEnterInLineCommentHandler : EnterHandlerDelegate {

  override fun preprocessEnter(file: PsiFile,
                               editor: Editor,
                               caretOffsetRef: Ref<Int>,
                               caretAdvance: Ref<Int>,
                               dataContext: DataContext,
                               originalHandler: EditorActionHandler?): Result {
    if (EnterHandler.getLanguage(dataContext) !is RoxygenLanguage) return Result.Continue
    val caretOffset = editor.caretModel.offset
    val document = editor.document
    PsiDocumentManager.getInstance(file.project).commitDocument(document)
    val elementOffset = if (caretOffset < document.textLength) caretOffset else document.textLength - 1
    val element = file.findElementAt(elementOffset) ?: return Result.Continue

    if (element.elementType != ROXYGEN_DOC_PREFIX || caretOffset != elementOffset) {
      val text = document.text
      var newCaretPosition = caretOffset
      val result = buildString {
        val wsPrefix = element.containingFile?.firstChild.let { if (it?.elementType == ROXYGEN_WS) it!!.text else "" }
        append("\n").append(wsPrefix).append(RoxygenUtil.DOCUMENTATION_COMMENT_PREFIX)
        if (caretOffset >= text.length || text[caretOffset] != ' ') {
          append(" ")
        } else {
          ++newCaretPosition
        }
        newCaretPosition += length
      }
      document.insertString(caretOffset, result)
      editor.caretModel.moveToOffset(newCaretPosition)
      return Result.Stop
    }
    return Result.Continue
  }
}
