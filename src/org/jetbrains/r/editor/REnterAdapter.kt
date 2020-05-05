/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.elementType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.editor.formatting.R_SPACE_TOKENS
import org.jetbrains.r.editor.formatting.findPrevNonSpaceNode
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
    if (file.language == RLanguage.INSTANCE) adjustAlignmentAfterEnter(editor, file)
    return Result.Continue
  }

  private fun adjustAlignmentAfterEnter(editor: Editor, file: PsiFile): Boolean {
    val offset = editor.caretModel.offset
    val element = file.findElementAt(offset) ?: return false
    val brackets = element.parent ?: return false
    val content = brackets.node.findChildByType(R_OPEN_PAIRS)?.psi?.nextSibling ?: return false
    if (R_CLOSE_PAIRS.contains(element.elementType)) {
      val prevElementType = findPrevNonSpaceNode(element.node)?.elementType
      if (prevElementType != RElementTypes.R_EMPTY_EXPRESSION && prevElementType != TokenType.ERROR_ELEMENT) return false
    }
    val contentIndent = calculateIndent(content, editor)

    val elementIndent = calculateIndent(element, editor)
    if (elementIndent < contentIndent) {
      editor.document.insertString(offset, " ".repeat(contentIndent - elementIndent))
    }
    return true
  }

  private fun calculateIndent(content: PsiElement, editor: Editor): Int {
    val contentOffset = content.textOffset
    val contentLine = editor.document.getLineNumber(contentOffset)
    val contentLineOffset = editor.document.getLineStartOffset(contentLine)
    return contentOffset - contentLineOffset
  }
}
