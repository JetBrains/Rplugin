/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.r.psi.roxygen.lexer

import com.intellij.lexer.FlexAdapter
import com.intellij.psi.tree.IElementType
import com.intellij.r.psi.lexer.PatchingLexerWithQueue
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_AUTOLINK_URI
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_DOUBLECOLON
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_IDENTIFIER
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_LANGLE
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_LBRACKET
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_LPAR
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_NL
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_RANGLE
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_RBRACKET
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_RPAR
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_TEXT
import com.intellij.r.psi.roxygen.parsing.RoxygenElementTypes.ROXYGEN_WS

class RoxygenLexer : PatchingLexerWithQueue(RoxygenLexerWithCollapsedSequences()) {

  // WS TEXT WS TEXT WS -> WS TEXT WS
  override fun processToken() {
    if (delegate.tokenType == ROXYGEN_TEXT) {
      val sequence = mutableListOf<TokenData>()
      while (true) {
        if (delegate.tokenType !in listOf(ROXYGEN_WS, ROXYGEN_TEXT)) break
        sequence.add(getTokenData())
        delegate.advance()
      }

      // Don't collapse whitespaces at the end
      val lastText = sequence.indexOfLast { it.type == ROXYGEN_TEXT }
      val firstToken = sequence.first()
      val lastTextToken = sequence[lastText]
      val lastToken = sequence.last()

      queue.add(TokenData(ROXYGEN_TEXT, firstToken.start, lastTextToken.end, lastTextToken.state))
      if (lastTextToken != lastToken) {
        queue.add(TokenData(ROXYGEN_WS, sequence[lastText + 1].start, lastToken.end, lastToken.state))
      }
    }
  }
}

private class RoxygenLexerWithCollapsedSequences : PatchingLexerWithQueue(FlexAdapter(_RoxygenLexer(null))) {

  override fun processToken() {
    when (delegate.tokenType) {
      ROXYGEN_LBRACKET -> collapseText(ROXYGEN_RBRACKET, listOf(ROXYGEN_LBRACKET, ROXYGEN_LANGLE)) {
        val nextTokenType = delegate.tokenType

        nextTokenType == ROXYGEN_LBRACKET // [text][help_page_link] -> TEXT[help_page_link]
        || nextTokenType == ROXYGEN_LPAR  // [text](link_destination) -> TEXT(link_destination)
        || incorrectLink(it)              // [not help page link] -> TEXT
      }
      ROXYGEN_LPAR -> collapseText(ROXYGEN_RPAR)
      ROXYGEN_LANGLE -> collapseText(ROXYGEN_RANGLE, listOf(ROXYGEN_LBRACKET, ROXYGEN_LANGLE)) { sequence ->
        sequence.map { it.type } != listOf(ROXYGEN_LANGLE, ROXYGEN_AUTOLINK_URI, ROXYGEN_RANGLE) // <SCHEME:PATH>
      }
    }
  }

  private fun collapseText(endSequenceType: IElementType,
                           interruptingTokens: List<IElementType> = emptyList(),
                           collapseSequence: (List<TokenData>) -> Boolean = { false }) {
    val sequence = mutableListOf<TokenData>()
    sequence.add(getTokenData())
    delegate.advance()

    var lastTokenType: IElementType?
    while (true) {
      lastTokenType = delegate.tokenType
      if (lastTokenType == null || lastTokenType == ROXYGEN_NL || lastTokenType in interruptingTokens) break
      sequence.add(getTokenData())
      delegate.advance()
      if (lastTokenType == endSequenceType) break
    }

    if (lastTokenType == endSequenceType && !collapseSequence(sequence)) {
      queue.addAll(sequence)
      return
    }

    // If the sequence need to collapse or sequence is incomplete due to interruptingTokens
    val firstToken = sequence.first()
    val lastToken = sequence.last()
    queue.add(TokenData(ROXYGEN_TEXT, firstToken.start, lastToken.end, lastToken.state))
  }

  companion object {
    private fun incorrectLink(tokens: List<TokenData>): Boolean {
      val tokensString = buildString {
        for (token in tokens) {
          val cunTokenChar = when (token.type) {
            ROXYGEN_LBRACKET -> 'l'
            ROXYGEN_IDENTIFIER -> 'i'
            ROXYGEN_RBRACKET -> 'r'
            ROXYGEN_DOUBLECOLON -> ':'
            ROXYGEN_LPAR -> 'o'
            ROXYGEN_RPAR -> 'c'
            else -> '?'
          }
          append(cunTokenChar)
        }
      }
      return !Regex("l(i:)?i(oc)?r").matches(tokensString)
    }
  }
}