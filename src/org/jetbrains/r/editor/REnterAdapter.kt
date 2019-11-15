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

class REnterAdapter : EnterHandlerDelegateAdapter() {
  override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): Result {
    adjustDocumentComment(editor, file)
    return Result.Continue
  }

  /**
   * This method relay on existing of [org.jetbrains.r.editor.RCommenter]
   * and standard logic in [com.intellij.codeInsight.editorActions.enter.EnterInLineCommentHandler]
   */
  private fun adjustDocumentComment(editor: Editor, file: PsiFile) {
    val offset = editor.caretModel.offset
    if (offset < 2 || editor.document.charsSequence.subSequence(offset - 2, offset).toString() != "# ") return
    val comment = file.findElementAt(offset) as? PsiComment ?: return
    val prevSpace = comment.prevSibling as? PsiWhiteSpace ?: return
    val prevCommentText = (prevSpace.prevSibling as? PsiComment)?.text ?: return
    if (!prevCommentText.startsWith("#'")) return
    editor.document.insertString(offset - 1, "'")
  }
}
