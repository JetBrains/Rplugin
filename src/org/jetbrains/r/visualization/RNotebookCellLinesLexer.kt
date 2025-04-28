package org.jetbrains.r.visualization

import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.Document
import com.intellij.util.keyFMap.KeyFMap
import org.jetbrains.r.visualization.RNotebookCellLines.*
import kotlin.math.max

internal interface RNotebookCellLinesLexer {
  fun markerSequence(chars: CharSequence, ordinalIncrement: Int, offsetIncrement: Int, defaultLanguage: Language): Sequence<Marker>

  private data class IntervalInfo(val lineNumber: Int, val cellType: CellType, val markersAtLInes: MarkersAtLines, val data: KeyFMap)

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

  companion object {
    fun <Lex : Lexer> defaultMarkerSequence(
      underlyingLexerFactory: () -> Lex,
      getCellTypeAndData: (lexer: Lex) -> Pair<CellType, KeyFMap>?,
      chars: CharSequence,
      ordinalIncrement: Int,
      offsetIncrement: Int,
    ): Sequence<Marker> = sequence {
      val lexer = underlyingLexerFactory()
      lexer.start(chars, 0, chars.length)
      var ordinal = 0
      while (lexer.tokenType != null) {
        getCellTypeAndData(lexer)?.let { (type, data) ->
          yield(Marker(
            ordinal = ordinal++ + ordinalIncrement,
            type = type,
            offset = lexer.currentPosition.offset + offsetIncrement,
            length = lexer.tokenText.length,
            data = data,
          ))
        }
        lexer.advance()
      }
    }

    fun defaultIntervals(document: Document, markers: List<Marker>, firstMarkerData: KeyFMap, lastMarkerData: KeyFMap): List<Interval> {
      val intervals = toIntervalsInfo(document, markers, firstMarkerData, lastMarkerData)

      val result = mutableListOf<Interval>()
      for (i in 0 until (intervals.size - 1)) {
        result += Interval(ordinal = i, type = intervals[i].cellType,
                           lines = intervals[i].lineNumber until intervals[i + 1].lineNumber,
                           markers = intervals[i].markersAtLInes,
                           intervals[i].data)
      }
      return result
    }

    private fun toIntervalsInfo(
      document: Document,
      markers: List<Marker>,
      firstMarkerData: KeyFMap,
      lastMarkerData: KeyFMap,
    ): List<IntervalInfo> {
      val m = mutableListOf<IntervalInfo>()

      // add first if necessary
      if (markers.isEmpty() || document.getLineNumber(markers.first().offset) != 0) {
        m += IntervalInfo(0, CellType.RAW, MarkersAtLines.NO, firstMarkerData)
      }

      for (marker in markers) {
        m += IntervalInfo(document.getLineNumber(marker.offset), marker.type, MarkersAtLines.TOP, marker.data)
      }

      // marker for the end
      m += IntervalInfo(max(document.lineCount, 1), CellType.RAW, MarkersAtLines.NO, lastMarkerData)
      return m
    }
  }
}