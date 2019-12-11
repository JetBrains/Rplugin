/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.lexer.DelegateLexer
import com.intellij.psi.tree.IElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.MarkdownElementType
import org.intellij.plugins.markdown.lang.lexer.MarkdownToplevelLexer
import java.util.*

val R_FENCE_ELEMENT_TYPE = IElementType("R Fence", RMarkdownLanguage)

val MARKDOWN_EOL: IElementType = MarkdownElementType.platformType(MarkdownTokenTypes.EOL)

private val FenceLangType = MarkdownElementType.platformType(MarkdownTokenTypes.FENCE_LANG)
private val FenceEndType = MarkdownElementType.platformType(MarkdownTokenTypes.CODE_FENCE_END)
private val WhiteSpaceType = MarkdownElementType.platformType(MarkdownTokenTypes.WHITE_SPACE)


class RMarkdownPatchingLexer : DelegateLexer(MarkdownToplevelLexer(RMarkdownFlavourDescriptor)) {
  private val queue: Queue<TokenData> = ArrayDeque<TokenData>()

  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    super.start(buffer, startOffset, endOffset, initialState)
    queue.clear()
  }

  override fun advance() {
    if (!queue.isEmpty()) {
      queue.remove()
      return
    }
    super.advance()
    if (delegate.tokenType === FenceLangType) {
      val fenceType: IElementType = RmdFenceProvider.matchHeader(tokenSequence)?.fenceElementType ?: return
      queue.add(getTokenData())

      super.advance()
      if (delegate.tokenType != MARKDOWN_EOL) {
        return
      }
      queue.add(getTokenData())

      val start = delegate.tokenEnd
      val state = delegate.state
      var end = start
      var eol: TokenData? = null
      val restore = ArrayList<TokenData>()
      while(true) {
        super.advance()
        val cur = getTokenData()
        when(cur.type) {
          null -> {
            queue.addAll(restore)
            return
          }
          FenceEndType -> {
            if (end - start > 0) {
              queue.add(TokenData(fenceType, start, end, state))
            }
            if (eol != null) queue.add(eol)
            return
          }
          MARKDOWN_EOL -> {
            end = cur.start
            eol = cur
          }
          else -> {
            if (cur.type == WhiteSpaceType && bufferSequence.subSequence(cur.start, cur.end).contains('>')) {
              queue.addAll(restore)
              return
            }
            end = cur.end
          }
        }
        restore.add(cur)
      }
    }
  }

  override fun getTokenType(): IElementType? {
    return if (!queue.isEmpty()) queue.peek().type else delegate.tokenType
  }

  override fun getTokenEnd(): Int {
    return if (!queue.isEmpty()) queue.peek().end else delegate.tokenEnd
  }

  override fun getTokenStart(): Int {
    return if (!queue.isEmpty()) queue.peek().start else delegate.tokenStart
  }

  override fun getState(): Int {
    return if (!queue.isEmpty()) queue.peek().state else delegate.state
  }

  private fun getTokenData() = TokenData(delegate.tokenType, delegate.tokenStart, delegate.tokenEnd, delegate.state)

  private data class TokenData(val type : IElementType?, val start: Int, val end: Int, val state: Int)
}
