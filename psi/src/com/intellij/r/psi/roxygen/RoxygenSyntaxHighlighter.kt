/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.intellij.r.psi.roxygen.lexer.RoxygenLexer
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes

class RoxygenSyntaxHighlighter : SyntaxHighlighterBase() {

  override fun getHighlightingLexer(): Lexer = RoxygenLexer()

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
    val attribute = when (tokenType) {
      RoxygenElementTypes.ROXYGEN_TAG_NAME -> TAG_NAME
      else -> COMMENT_TEXT
    }
    return pack(attribute)
  }
}