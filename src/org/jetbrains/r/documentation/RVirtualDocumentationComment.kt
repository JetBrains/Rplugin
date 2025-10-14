package org.jetbrains.r.documentation

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.r.psi.parsing.RTokenTypes

class RVirtualDocumentationComment(private val parent: PsiElement, private val range: TextRange): FakePsiElement(), PsiDocCommentBase  {
  override fun getParent(): PsiElement {
    return parent
  }

  override fun getTokenType(): IElementType {
    return RTokenTypes.END_OF_LINE_COMMENT
  }

  override fun getTextRange(): TextRange {
    return range
  }

  override fun getText(): String {
    return range.substring(parent.containingFile.text)
  }

  override fun getOwner(): PsiElement {
    return parent
  }
}