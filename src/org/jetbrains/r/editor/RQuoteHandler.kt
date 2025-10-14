// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.editor

import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.r.psi.parsing.RElementTypes

class RQuoteHandler : SimpleTokenSetQuoteHandler(RElementTypes.R_STRING), MultiCharQuoteHandler {
  override fun isOpeningQuote(iterator: HighlighterIterator?, offset: Int): Boolean {
    if (getRawStringQuote(iterator, offset) != null) return true
    return super.isOpeningQuote(iterator, offset)
  }

  override fun getClosingQuote(iterator: HighlighterIterator, offset: Int): CharSequence? {
    val quote = getRawStringQuote(iterator, offset)
    return if (quote != null) "()$quote"
    else iterator.document.charsSequence[iterator.start].takeIf { it == '"' || it == '\'' }?.toString()
  }

  override fun insertClosingQuote(editor: Editor, offset: Int, closingQuote: CharSequence) {
    super.insertClosingQuote(editor, offset, closingQuote)
    if (getRawStringQuote(editor.document, offset) != null) {
      editor.caretModel.moveToOffset(offset + 1)
    }
  }

  override fun hasNonClosedLiteral(editor: Editor?, iterator: HighlighterIterator?, offset: Int): Boolean {
    val document = editor!!.document
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

private fun getRawStringQuote(iterator: HighlighterIterator?, offset: Int): Char? {
  val document = iterator?.document ?: return null
  return getRawStringQuote(document, offset)
}

private fun getRawStringQuote(document: Document, offset: Int): Char? {
  val text: CharSequence = document.charsSequence
  if(text.length >= 2 && offset >= 2 && text[offset - 1].let { it == '"' || it == '\''} && text[offset - 2].let { it == 'R' || it == 'r'})
    return text[offset - 1]
  return null
}
