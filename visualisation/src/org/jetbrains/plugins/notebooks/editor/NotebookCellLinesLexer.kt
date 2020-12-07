package org.jetbrains.plugins.notebooks.editor

import com.intellij.lexer.Lexer
import com.intellij.psi.tree.IElementType

interface NotebookCellLinesLexer {
  val longestTokenLength: Int

  fun createNotebookCellTypeAwareLexer(): Lexer

  fun getCellType(tokenType: IElementType): NotebookCellLines.CellType?

  fun shouldParseWholeFile(): Boolean = false

  fun markerSequence(chars: CharSequence, ordinalIncrement: Int, offsetIncrement: Int): Sequence<NotebookCellLines.Marker> = sequence {
    val lexer = createNotebookCellTypeAwareLexer()
    lexer.start(chars, 0, chars.length)
    var ordinal = 0
    while (true) {
      val tokenType = lexer.tokenType ?: break
      val cellType = getCellType(tokenType)
      if (cellType != null) {
        yield(NotebookCellLines.Marker(
          ordinal = ordinal++ + ordinalIncrement,
          type = cellType,
          offset = lexer.currentPosition.offset + offsetIncrement,
          length = lexer.tokenText.length,
        ))
      }
      lexer.advance()
    }
  }
}