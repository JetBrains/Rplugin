/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
package org.jetbrains.r.editor

import com.intellij.codeInsight.generation.IndentedCommenter
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.r.psi.parsing.RTokenTypes

class RCommenter : CodeDocumentationAwareCommenter, IndentedCommenter {
  override fun getLineCommentPrefix(): String = "# "

  override fun getBlockCommentPrefix(): String? = null

  override fun getBlockCommentSuffix(): String? = null

  override fun getCommentedBlockCommentPrefix(): String? = null

  override fun getCommentedBlockCommentSuffix(): String? = null

  override fun getLineCommentTokenType(): IElementType = RTokenTypes.END_OF_LINE_COMMENT

  override fun getBlockCommentTokenType(): IElementType? = null

  override fun getDocumentationCommentTokenType(): IElementType = RTokenTypes.ROXYGEN_COMMENT

  override fun getDocumentationCommentPrefix(): String = "#'"

  override fun getDocumentationCommentLinePrefix(): String = "#'"

  override fun getDocumentationCommentSuffix(): String = ""

  override fun isDocumentationComment(element: PsiComment): Boolean = element.elementType == documentationCommentTokenType

  override fun forceIndentedLineComment(): Boolean = true
}