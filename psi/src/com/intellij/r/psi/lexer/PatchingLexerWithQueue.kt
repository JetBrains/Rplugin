/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.lexer

import com.intellij.lexer.DelegateLexer
import com.intellij.lexer.Lexer
import com.intellij.psi.tree.IElementType
import java.util.ArrayDeque
import java.util.Queue

abstract class PatchingLexerWithQueue(delegate: Lexer) : DelegateLexer(delegate) {
  protected val queue: Queue<TokenData> = ArrayDeque()

  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    super.start(buffer, startOffset, endOffset, initialState)
    queue.clear()
  }

  override fun advance() {
    if (queue.isNotEmpty()) {
      queue.remove()
      if (queue.isNotEmpty()) return // The next token may also need to be processed
    } else {
      delegate.advance()
    }

    processToken()
  }

  protected abstract fun processToken()

  override fun getTokenType(): IElementType? {
    return if (queue.isNotEmpty()) queue.peek().type else delegate.tokenType
  }

  override fun getTokenEnd(): Int {
    return if (queue.isNotEmpty()) queue.peek().end else delegate.tokenEnd
  }

  override fun getTokenStart(): Int {
    return if (queue.isNotEmpty()) queue.peek().start else delegate.tokenStart
  }

  override fun getState(): Int {
    return if (queue.isNotEmpty()) queue.peek().state else delegate.state
  }

  protected fun getTokenData() = TokenData(delegate.tokenType, delegate.tokenStart, delegate.tokenEnd, delegate.state)

  protected data class TokenData(val type : IElementType?, val start: Int, val end: Int, val state: Int)
}