// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.editor

import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.jetbrains.r.parsing.RElementTypes

class RQuoteHandler : SimpleTokenSetQuoteHandler(RElementTypes.R_STRING), MultiCharQuoteHandler {
  override fun isOpeningQuote(iterator: HighlighterIterator?, offset: Int): Boolean {
    if (isRawString(iterator, offset)) return true
    return super.isOpeningQuote(iterator, offset)
  }

  override fun getClosingQuote(iterator: HighlighterIterator, offset: Int): CharSequence? {
    return if (isRawString(iterator, offset)) "()\""
    else iterator.document.charsSequence[iterator.start].takeIf { it == '"' || it == '\'' }?.toString()
  }

  override fun insertClosingQuote(editor: Editor, offset: Int, closingQuote: CharSequence) {
    super.insertClosingQuote(editor, offset, closingQuote)
    if (isRawString(editor.document, offset)) {
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

private fun isRawString(iterator: HighlighterIterator?, offset: Int): Boolean {
  val document = iterator?.document ?: return false
  return isRawString(document, offset)
}

private fun isRawString(document: Document, offset: Int): Boolean {
  val text: CharSequence = document.charsSequence
  return text.length >= 2 && offset > 2 && text[offset - 1] == '"' && text[offset - 2] == 'r'
}
