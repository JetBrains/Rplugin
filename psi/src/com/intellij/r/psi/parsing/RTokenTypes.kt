package com.intellij.r.psi.parsing

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.r.psi.psi.RElementType

object RTokenTypes {
  @JvmField
  val END_OF_LINE_COMMENT: IElementType = RElementType("END_OF_LINE_COMMENT")
  @JvmField
  val ROXYGEN_COMMENT: IElementType = RElementType("ROXYGEN_COMMENT")
  @JvmField
  val BAD_CHARACTER: IElementType = RElementType("BAD_CHARACTER")

  @JvmField
  val COMMENTS = TokenSet.create(END_OF_LINE_COMMENT, ROXYGEN_COMMENT)
  @JvmField
  val STRINGS = TokenSet.create(RElementTypes.R_STRING)
}