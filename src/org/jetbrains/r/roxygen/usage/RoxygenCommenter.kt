/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.usage

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.lang.CodeDocumentationAwareCommenterEx
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_TEXT

class RoxygenCommenter : CodeDocumentationAwareCommenter, CodeDocumentationAwareCommenterEx {
  override fun getLineCommentTokenType(): IElementType? = null

  override fun getBlockCommentTokenType(): IElementType? = null

  override fun getDocumentationCommentTokenType(): IElementType? = null

  override fun getDocumentationCommentPrefix(): String? = null

  override fun getDocumentationCommentLinePrefix(): String? = null

  override fun getDocumentationCommentSuffix(): String? = null

  override fun isDocumentationComment(element: PsiComment?): Boolean = true

  override fun getLineCommentPrefix(): String? = null

  override fun getBlockCommentPrefix(): String? = null

  override fun getBlockCommentSuffix(): String? = null

  override fun getCommentedBlockCommentPrefix(): String? = null

  override fun getCommentedBlockCommentSuffix(): String? = null

  override fun isDocumentationCommentText(element: PsiElement?): Boolean {
    val node = element?.node ?: return false
    return node.elementType === ROXYGEN_TEXT
  }
}