/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.elementType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.parsing.RElementTypes

private val R_CLOSE_PAIRS = TokenSet.create(
  RElementTypes.R_RPAR,
  RElementTypes.R_RBRACKET,
  RElementTypes.R_RDBRACKET
)

private val R_OPEN_PAIRS = TokenSet.create(
  RElementTypes.R_LPAR,
  RElementTypes.R_LBRACKET,
  RElementTypes.R_LDBRACKET
)

class REnterAdapter : EnterHandlerDelegateAdapter() {
  override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): Result {
    if (file.language == RLanguage.INSTANCE) {
      when {
        adjustAlignmentAfterEnter(editor, file) -> return Result.Continue
        adjustDocumentComment(editor, file) -> return Result.Continue
      }
    }
    return Result.Continue
  }

  private fun adjustAlignmentAfterEnter(editor: Editor, file: PsiFile): Boolean {
    val offset = editor.caretModel.offset
    val bracket = file.findElementAt(offset) ?: return false
    if (!R_CLOSE_PAIRS.contains(bracket.elementType)) return false
    val prevElementType = bracket.prevSibling?.elementType
    if (prevElementType != RElementTypes.R_EMPTY_EXPRESSION && prevElementType != TokenType.ERROR_ELEMENT) return false
    val brackets = bracket.parent ?: return false
    val content = brackets.node.findChildByType(R_OPEN_PAIRS)?.psi?.nextSibling ?: return false
    val contentIndent = calculateIndent(content, editor)
    val bracketIndent = calculateIndent(bracket, editor)
    if (bracketIndent < contentIndent) {
      editor.document.insertString(offset, " ".repeat(contentIndent - bracketIndent))
    }
    return true
  }

  private fun calculateIndent(content: PsiElement, editor: Editor): Int {
    val contentOffset = content.textOffset
    val contentLine = editor.document.getLineNumber(contentOffset)
    val contentLineOffset = editor.document.getLineStartOffset(contentLine)
    return contentOffset - contentLineOffset
  }

  /**
   * This method relay on existing of [org.jetbrains.r.editor.RCommenter]
   * and standard logic in [com.intellij.codeInsight.editorActions.enter.EnterInLineCommentHandler]
   */
  private fun adjustDocumentComment(editor: Editor, file: PsiFile): Boolean {
    val offset = editor.caretModel.offset
    val elementAt = file.findElementAt(offset)

    var prevSibling = if (elementAt == null && offset == editor.document.textLength) {
      TreeUtil.findLastLeaf(file.node)?.psi
    }
    else {
      elementAt?.prevSibling
    } as? PsiWhiteSpace ?: return false

    if (prevSibling.elementType == TokenType.WHITE_SPACE)
      prevSibling = prevSibling.prevSibling as? PsiWhiteSpace ?: return false
    if (prevSibling.elementType != RElementTypes.R_NL) return false
    val prevCommentText = (prevSibling.prevSibling as? PsiComment)?.text ?: return false
    if (!prevCommentText.startsWith("#'")) return false
    if (elementAt is PsiComment) {
      editor.document.insertString(offset - 1, "'")
    }
    else {
      editor.document.insertString(offset, "#' ")
      editor.caretModel.moveCaretRelatively(3, 0, false, true, true)
    }
    return true
  }
}
