/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.psi.tree.IElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.MarkdownElementType
import org.intellij.plugins.markdown.lang.lexer.MarkdownToplevelLexer
import org.jetbrains.r.lexer.PatchingLexerWithQueue
import java.util.*

val R_FENCE_ELEMENT_TYPE = IElementType("R Fence", RMarkdownLanguage)

/** IMPORTANT: this element type is used only on lexical level. PSI has standard TokenType.WHITE_SPACE */
val MARKDOWN_EOL: IElementType = MarkdownElementType.platformType(MarkdownTokenTypes.EOL)

private val FenceLangType = MarkdownElementType.platformType(MarkdownTokenTypes.FENCE_LANG)
private val FenceEndType = MarkdownElementType.platformType(MarkdownTokenTypes.CODE_FENCE_END)

/** IMPORTANT: this element type is used only on lexical level. PSI has standard TokenType.WHITE_SPACE */
private val WhiteSpaceType = MarkdownElementType.platformType(MarkdownTokenTypes.WHITE_SPACE)

class PatchingLexer(private val provideFence: (CharSequence) -> IElementType?) : PatchingLexerWithQueue(MarkdownToplevelLexer(RMarkdownFlavourDescriptor)) {
  override fun processToken() {
    if (delegate.tokenType === FenceLangType) {
      val fenceType: IElementType = provideFence(tokenSequence) ?: return
      queue.add(getTokenData())

      delegate.advance()
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
        delegate.advance()
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
}
