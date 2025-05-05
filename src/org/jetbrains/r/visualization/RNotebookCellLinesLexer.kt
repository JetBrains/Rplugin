package org.jetbrains.r.visualization

import com.intellij.lang.Language
import com.intellij.util.keyFMap.KeyFMap
import org.jetbrains.r.visualization.RNotebookCellLines.CellType

internal interface RNotebookCellLinesLexer {
  fun markerSequence(chars: CharSequence, ordinalIncrement: Int, offsetIncrement: Int, defaultLanguage: Language): Sequence<Marker>

  data class Marker(
    val ordinal: Int,
    val type: CellType,
    val offset: Int,
    val length: Int,
    val data: KeyFMap,
  ) : Comparable<Marker> {
    val endOffset: Int
      get() = offset + length

    override fun compareTo(other: Marker): Int = offset - other.offset
  }
}