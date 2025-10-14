/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.parsing

import com.intellij.lang.DefaultASTFactoryImpl
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.impl.source.tree.PsiCommentImpl
import com.intellij.psi.impl.source.tree.injected.CommentLiteralEscaper
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.roxygen.RoxygenCommentPlaceholder

class RAstFactory : DefaultASTFactoryImpl() {

  private class PsiCommentPlaceholder(type: IElementType, text: CharSequence) : PsiCommentImpl(type, text), RoxygenCommentPlaceholder {
    override fun createLiteralTextEscaper(): LiteralTextEscaper<PsiCommentImpl> {
      return object : CommentLiteralEscaper(this) {
        override fun isOneLine(): Boolean {
          return false // that is need to receive EnterAction action in editor
        }
      }
    }

    override fun getOwner(): PsiElement? {
      return PsiTreeUtil.getNextSiblingOfType(this, RExpression::class.java)
    }
  }

  override fun createLeaf(type: IElementType, text: CharSequence): LeafElement {
//    if (type == R_NL) {
//      return RNextLinePsiWhiteSpace(type, text)
//    }
    return super.createLeaf(type, text)
  }

  override fun createComment(type: IElementType, text: CharSequence): LeafElement {
    if (type === RTokenTypes.ROXYGEN_COMMENT) {
        return PsiCommentPlaceholder(type, text)
    }
    return super.createComment(type, text)
  }
}

private class RNextLinePsiWhiteSpace(type: IElementType, text: CharSequence) : LeafPsiElement(type, text), PsiWhiteSpace
