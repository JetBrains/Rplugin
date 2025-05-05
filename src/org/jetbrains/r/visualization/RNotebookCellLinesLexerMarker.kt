package org.jetbrains.r.visualization

import com.intellij.util.keyFMap.KeyFMap

data class RNotebookCellLinesLexerMarker(
  val ordinal: Int,
  val type: RNotebookCellLines.CellType,
  val offset: Int,
  val length: Int,
  val data: KeyFMap,
) : Comparable<RNotebookCellLinesLexerMarker> {
  val endOffset: Int
    get() = offset + length

  override fun compareTo(other: RNotebookCellLinesLexerMarker): Int = offset - other.offset
}