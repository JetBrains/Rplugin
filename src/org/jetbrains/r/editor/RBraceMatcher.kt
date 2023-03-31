/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
package org.jetbrains.r.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.parsing.RTokenTypes
import org.jetbrains.r.rmarkdown.MARKDOWN_EOL

private val bracePairs = arrayOf(
  BracePair(RElementTypes.R_LPAR, RElementTypes.R_RPAR, false),
  BracePair(RElementTypes.R_LBRACKET, RElementTypes.R_RBRACKET, false),
  BracePair(RElementTypes.R_LDBRACKET, RElementTypes.R_RDBRACKET, false),
  BracePair(RElementTypes.R_LBRACE, RElementTypes.R_RBRACE, false))

private val completionContext = TokenSet.create(
  TokenType.WHITE_SPACE,
  MARKDOWN_EOL,

  RTokenTypes.END_OF_LINE_COMMENT,
  RElementTypes.R_SEMI,

  RElementTypes.R_LBRACE,
  RElementTypes.R_RBRACE,
  RElementTypes.R_LBRACKET,
  RElementTypes.R_RBRACKET,
  RElementTypes.R_LDBRACKET,
  RElementTypes.R_RDBRACKET,
  RElementTypes.R_LPAR,
  RElementTypes.R_RPAR,

  RElementTypes.R_AND,
  RElementTypes.R_ANDAND,
  RElementTypes.R_AT,
  RElementTypes.R_COLON,
  RElementTypes.R_COMMA,
  RElementTypes.R_DIV,
  RElementTypes.R_EQ,
  RElementTypes.R_EQEQ,
  RElementTypes.R_EXP,
  RElementTypes.R_GE,
  RElementTypes.R_GT,
  RElementTypes.R_LE,
  RElementTypes.R_LEFT_ASSIGN,
  RElementTypes.R_LEFT_ASSIGN_OLD,
  RElementTypes.R_LEFT_COMPLEX_ASSIGN,
  RElementTypes.R_LIST_SUBSET,
  RElementTypes.R_LT,
  RElementTypes.R_MULT,
  RElementTypes.R_NOTEQ,
  RElementTypes.R_OR,
  RElementTypes.R_OROR,
  RElementTypes.R_RIGHT_ASSIGN,
  RElementTypes.R_RIGHT_COMPLEX_ASSIGN,
  RElementTypes.R_EQ
)

class RBraceMatcher : PairedBraceMatcher {
  override fun getPairs(): Array<BracePair> = bracePairs

  override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean =
    contextType == null || completionContext.contains(contextType)

  override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}
