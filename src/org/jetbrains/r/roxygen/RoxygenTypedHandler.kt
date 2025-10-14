/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes
import com.intellij.r.psi.roxygen.psi.RoxygenFile

class RoxygenTypedHandler : TypedHandlerDelegate() {

  override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
    if (overwriteClosingBracket(c, editor, file)) {
      EditorModificationUtil.moveCaretRelatively(editor, 1)
      return Result.STOP
    }
    return Result.CONTINUE
  }

  override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
    return if (handleBracketTyped(c, project, editor, file)) Result.STOP
    else Result.CONTINUE
  }

  private fun overwriteClosingBracket(c: Char, editor: Editor, file: PsiFile): Boolean {
    if (file !is RoxygenFile || c != ']' && c != ')') return false
    if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return false

    val offset = editor.caretModel.offset
    val document = editor.document
    val text = document.text
    if (offset >= document.textLength || text[offset] != c) return false

    val element = file.findElementAt(offset) ?: return false
    val elementType = element.elementType

    return when (c) {
      ']' -> {
        // if the bracket is not part of a link, it will be part of ROXYGEN_TEXT, not a separate ROXYGEN_RBRACKET element
        elementType == RoxygenElementTypes.ROXYGEN_RBRACKET || (offset > 0 && text[offset - 1] == '[')
      }
      ')' -> elementType == RoxygenElementTypes.ROXYGEN_RPAR
      else -> false
    }
  }

  private fun handleBracketTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Boolean {
    if (file !is RoxygenFile || c != '[' && c != '(') return false
    if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return false

    val offset = editor.caretModel.offset
    if (offset == 0) return false
    val document = editor.document
    PsiDocumentManager.getInstance(project).commitDocument(document)
    when (c) {
      '[' -> {
        document.insertString(offset, "]")
        return true
      }
      '(' -> {
        if (offset > 1 && document.text[offset - 2] == ']') {
          document.insertString(offset, ")")
          return true
        }
      }
    }
    return false
  }
}
