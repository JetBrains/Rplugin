/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
package org.jetbrains.r.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import com.intellij.util.containers.Stack

object RParserUtil : GeneratedParserUtilBase() {
  private val R_PARSING_DATA: Key<ParsingData> = Key.create<ParsingData>("hello")

  private fun getRelatedData(builder: PsiBuilder): ParsingData =
    builder.getUserData(R_PARSING_DATA) ?: ParsingData().also { builder.putUserData(R_PARSING_DATA, it) }

  @JvmStatic
  fun parseEmptyExpression(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    val emptyMarker = builder.mark()
    emptyMarker.done(RElementTypes.R_EMPTY_EXPRESSION)
    return true
  }

  @JvmStatic
  fun isNewLine(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    if (getRelatedData(builder).brackets.let { !it.empty() && it.peek() != 0 }) {
      return false
    }
    if (builder.tokenType == null) return true
    var offset = builder.currentOffset - 1
    val text = builder.originalText
    while (offset >= 0) {
      if (text[offset] == '\n') return true
      if (!text[offset].isWhitespace()) return false
      offset--
    }
    return false
  }

  @JvmStatic
  fun resetBrackets(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    getRelatedData(builder).brackets.apply { push(0) }
    return true
  }

  @JvmStatic
  fun incBrackets(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    getRelatedData(builder).brackets.apply {
      val previous = if (empty()) 0 else peek()
      push(previous + 1)
    }
    return true
  }

  @JvmStatic
  fun decBrackets(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    getRelatedData(builder).brackets.pop()
    return true
  }

  private class ParsingData {
    val brackets = Stack<Int>()
  }
}
