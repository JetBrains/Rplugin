package org.jetbrains.r.visualization

import com.intellij.lang.Language


data class RNotebookCellLinesLexerMarker(
  val ordinal: Int,
  val type: RNotebookCellLines.CellType,
  val offset: Int,
  val length: Int,
  val language: Language,
) : Comparable<RNotebookCellLinesLexerMarker> {
  val endOffset: Int
    get() = offset + length

  override fun compareTo(other: RNotebookCellLinesLexerMarker): Int = offset - other.offset
}