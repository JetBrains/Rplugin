// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.editor

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.jetbrains.r.parsing.RElementTypes

class RQuoteHandler : SimpleTokenSetQuoteHandler(RElementTypes.R_STRING) {
  override fun hasNonClosedLiteral(editor: Editor,
                                   iterator: HighlighterIterator,
                                   offset: Int): Boolean {
    val document = editor.document
    val lineEndOffset = document.getLineEndOffset(document.getLineNumber(offset))
    if (offset < lineEndOffset) {
      val charSequence = document.charsSequence
      val openQuote = charSequence[offset]
      val nextCharOffset = offset + 1
      if (nextCharOffset < lineEndOffset && charSequence[nextCharOffset] == openQuote) {
        return true
      }
      for (i in nextCharOffset + 1 until lineEndOffset) {
        if (charSequence[i] == openQuote) {
          return false
        }
      }
    }
    return true
  }
}