/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.parsing.RElementTypes

class REnterAdapter : EnterHandlerDelegateAdapter() {
  override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): Result {
    if (file.language == RLanguage.INSTANCE) {
      adjustDocumentComment(editor, file)
    }
    return Result.Continue
  }

  /**
   * This method relay on existing of [org.jetbrains.r.editor.RCommenter]
   * and standard logic in [com.intellij.codeInsight.editorActions.enter.EnterInLineCommentHandler]
   */
  private fun adjustDocumentComment(editor: Editor, file: PsiFile) {
    val offset = editor.caretModel.offset
    val elementAt = file.findElementAt(offset)

    var prevSibling = if (elementAt == null && offset == editor.document.textLength) {
      TreeUtil.findLastLeaf(file.node)?.psi
    }
    else {
      elementAt?.prevSibling
    } as? PsiWhiteSpace ?: return

    if (prevSibling.elementType == TokenType.WHITE_SPACE)
      prevSibling = prevSibling.prevSibling as? PsiWhiteSpace ?: return
    if (prevSibling.elementType != RElementTypes.R_NL) return
    val prevCommentText = (prevSibling.prevSibling as? PsiComment)?.text ?: return
    if (!prevCommentText.startsWith("#'")) return
    if (elementAt is PsiComment) {
      editor.document.insertString(offset - 1, "'")
    }
    else {
      editor.document.insertString(offset, "#' ")
      editor.caretModel.moveCaretRelatively(3, 0, false, true, true)
    }
  }
}
