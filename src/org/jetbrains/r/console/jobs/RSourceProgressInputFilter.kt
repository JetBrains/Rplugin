/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.execution.filters.InputFilter
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Pair
import com.intellij.r.psi.lexer.SingleStringTokenLexer

class RSourceProgressInputFilter(private val onProgressEvent: (String) -> Unit) : InputFilter {
  private val output = StringBuffer()
  private val command = StringBuffer()
  private val lexer = SingleStringTokenLexer(MARKER, output)
  private var markerOccurred: Boolean = false

  override fun applyFilter(text: String, contentType: ConsoleViewContentType): MutableList<Pair<String, ConsoleViewContentType>>? {
    if (contentType == ConsoleViewContentType.ERROR_OUTPUT) {
      for (char in text) {
        processCharacter(char)
      }
      return mutableListOf(Pair(output.toString(), contentType)).also { output.setLength(0)}
    }
    return null
  }

  private fun processCharacter(char: Char) {
    if (!markerOccurred) {
      if (lexer.advanceChar(char)) {
        markerOccurred = true
        lexer.restore()
      }
    } else {
      if (char == '\n') {
        onProgressEvent(command.toString())
        command.setLength(0)
        markerOccurred = false
      } else {
        command.append(char)
      }
    }
  }

  companion object {
    private const val MARKER = ">__jb_rplugin_progress__"
  }
}