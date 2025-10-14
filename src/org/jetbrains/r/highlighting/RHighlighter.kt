// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.intellij.r.psi.lexer.RLexer
import com.intellij.r.psi.parsing.RElementTypes
import com.intellij.r.psi.parsing.RTokenTypes
import com.intellij.r.psi.psi.RPsiUtil

class RHighlighter : SyntaxHighlighterBase() {

  override fun getHighlightingLexer(): Lexer {
    return RLexer()
  }

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
    return pack(ATTRIBUTES[tokenType])
  }

  companion object {
    private val ATTRIBUTES = HashMap<IElementType, TextAttributesKey>()

    init {
      fillMap(ATTRIBUTES, RPsiUtil.RESERVED_WORDS, KEYWORD)
      fillMap(ATTRIBUTES, RPsiUtil.OPERATORS, OPERATION_SIGN)

      ATTRIBUTES[RElementTypes.R_STRING] = STRING
      ATTRIBUTES[RElementTypes.R_INVALID_STRING] = STRING
      ATTRIBUTES[RElementTypes.R_NUMERIC] = NUMBER
      ATTRIBUTES[RElementTypes.R_COMPLEX] = NUMBER
      ATTRIBUTES[RElementTypes.R_INTEGER] = NUMBER

      ATTRIBUTES[RElementTypes.R_LPAR] = PARENTHESES
      ATTRIBUTES[RElementTypes.R_RPAR] = PARENTHESES

      ATTRIBUTES[RElementTypes.R_LBRACE] = BRACES
      ATTRIBUTES[RElementTypes.R_RBRACE] = BRACES

      ATTRIBUTES[RElementTypes.R_LBRACKET] = BRACKETS
      ATTRIBUTES[RElementTypes.R_LDBRACKET] = BRACKETS
      ATTRIBUTES[RElementTypes.R_RBRACKET] = BRACKETS
      ATTRIBUTES[RElementTypes.R_RDBRACKET] = BRACKETS

      ATTRIBUTES[RElementTypes.R_COMMA] = COMMA
      ATTRIBUTES[RElementTypes.R_SEMI] = SEMICOLON

      ATTRIBUTES[RTokenTypes.END_OF_LINE_COMMENT] = LINE_COMMENT
      ATTRIBUTES[RTokenTypes.ROXYGEN_COMMENT] = DOC_COMMENT

      ATTRIBUTES[RTokenTypes.BAD_CHARACTER] = BAD_CHARACTER
    }
  }
}
