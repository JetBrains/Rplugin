/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.execution.filters.InputFilter
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Pair

class RSourceProgressInputFilter(private val onProgressEvent: (String) -> Unit) : InputFilter {
  private val lexer = Lexer()
  private val output = StringBuffer()
  private val command = StringBuffer()
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
      consumeChar(char)
      if (command.length == MARKER.length) {
        markerOccurred = true
        lexer.pos = 0
        command.setLength(0)
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

  private fun consumeChar(char: Char) {
    if (lexer.consume(char)) {
      command.append(char)
    } else {
      output.append(command)
      command.setLength(0)
      if (lexer.pos == 1) {
        command.append(char)
      } else {
        output.append(char)
      }
    }
  }

  companion object {
    private const val MARKER = ">__jb_rplugin_progress__"
  }

  private class Lexer {
    var pos: Int = 0

    fun consume(c: Char): Boolean {
      val match = MARKER[pos] == c
      pos = if (match) pos + 1 else (if (MARKER[0] == c) 1 else 0)
      return match
    }
  }

}