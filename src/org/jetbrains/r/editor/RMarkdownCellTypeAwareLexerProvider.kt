package org.jetbrains.r.editor

import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.lexer.MergeFunction
import com.intellij.lexer.MergingLexerAdapterBase
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.psi.tree.IElementType
import org.intellij.plugins.markdown.lang.MarkdownLanguage
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.lexer.MarkdownLexerAdapter
import org.jetbrains.plugins.notebooks.visualization.IntervalsGenerator
import org.jetbrains.plugins.notebooks.visualization.NonIncrementalCellLinesProvider
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLines
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLinesLexer
import org.jetbrains.r.rmarkdown.*

class RMarkdownCellLinesProvider : NonIncrementalCellLinesProvider(RMarkdownIntervalsGenerator())


private class RMarkdownIntervalsGenerator : IntervalsGenerator, NotebookCellLinesLexer {
  override fun makeIntervals(document: Document): List<NotebookCellLines.Interval> {
    val markers = markerSequence(document.charsSequence, 0, 0, PlainTextLanguage.INSTANCE).toList()
    return markers.map { toInterval(document, it) }
  }

  override fun markerSequence(chars: CharSequence,
                              ordinalIncrement: Int,
                              offsetIncrement: Int,
                              defaultLanguage: Language): Sequence<NotebookCellLinesLexer.Marker> =
    sequence {
      val langMap = RmdCellLanguageProvider.getAllLanguages()
      val maxLangSize: Int = langMap.keys.maxOfOrNull { it.length } ?: 0

      val seq = NotebookCellLinesLexer.defaultMarkerSequence(
        { RMarkdownMergingLangLexer() },
        getCellLanguageAndType = { token, lexer ->
          when (token) {
            RMarkdownCellType.MARKDOWN_CELL.elementType -> {
              Pair(MarkdownLanguage.INSTANCE, NotebookCellLines.CellType.MARKDOWN)
            }
            RMarkdownCellType.CODE_CELL.elementType -> {
              val cellText = lexer.tokenText
              val cellLanguageText = parseLanguage(cellText, maxLangSize)
              val language = langMap.getOrDefault(cellLanguageText, defaultLanguage)
              Pair(language, NotebookCellLines.CellType.CODE)
            }
            else -> null
          }
        },
        chars, ordinalIncrement, offsetIncrement
      )

      var lastMarker: NotebookCellLinesLexer.Marker? = null
      for (marker in seq) {
        lastMarker = marker
        yield(marker)
      }

      if (lastMarker?.type == NotebookCellLines.CellType.CODE && chars.endsWith('\n')) {
        yield(NotebookCellLinesLexer.Marker(
          ordinal = lastMarker.ordinal + 1,
          type = NotebookCellLines.CellType.MARKDOWN,
          offset = chars.length + offsetIncrement,
          length = 0,
          language = MarkdownLanguage.INSTANCE
        ))
      }

      if (lastMarker == null) {
        yield(NotebookCellLinesLexer.Marker(
          ordinal = 0,
          type = NotebookCellLines.CellType.MARKDOWN,
          offset = 0,
          length = 0,
          language = MarkdownLanguage.INSTANCE
        ))
      }
    }

  private fun parseLanguage(cellText: CharSequence, maxLangSize: Int): String? {
    val prefix = "```{"
    if (!cellText.startsWith(prefix)) return null
    return cellText.drop(prefix.length).take(maxLangSize + 1).takeWhile { it.isLetterOrDigit() }.toString()
  }
}


internal enum class RMarkdownCellType(private val debugName: String) {
  MARKDOWN_CELL("MARKDOWN_CELL"),
  CODE_CELL("CODE_CELL");

  val elementType = IElementType(debugName, RMarkdownLanguage)
}


internal enum class RMarkdownCodeMarkers(debugName: String) {
  BACKTICK_WITH_LANG("BACKTICK_WITH_LANG"),
  BACKTICK_NO_LANG("BACKTICK_NO_LANG");

  val elementType = IElementType(debugName, RMarkdownLanguage)
}


/**
 * merge ```{r} to BACKTICK_WITH_LANG and ``` to BACKTICK_NO_LANG
 * ```{ and ``` should be at the start of line, otherwise they are ignored
 */
internal class RMarkdownMapBackticks : MergingLexerAdapterBase(MarkdownLexerAdapter()) {
  override fun getMergeFunction(): MergeFunction = mergeFunction

  private val mergeFunction: MergeFunction = MergeFunction { type, originalLexer ->
    val isStartOfLine = tokenStart == 0 || bufferSequence[tokenStart - 1] == '\n'
    if (isStartOfLine && type == MarkdownTokenTypes.BACKTICK && tokenText == "```") {
      if (originalLexer.tokenText.startsWith("{")) {
        originalLexer.advance()
        RMarkdownCodeMarkers.BACKTICK_WITH_LANG.elementType
      }
      else {
        RMarkdownCodeMarkers.BACKTICK_NO_LANG.elementType
      }
    }
    else {
      type
    }
  }
}


internal class RMarkdownMergingLangLexer : MergingLexerAdapterBase(RMarkdownMapBackticks()) {

  override fun getMergeFunction(): MergeFunction = mergeFunction

  private val mergeFunction: MergeFunction = MergeFunction { type, originalLexer ->
    when {
      type == RMarkdownCodeMarkers.BACKTICK_WITH_LANG.elementType -> {
        if (consumeCode(originalLexer)) {
          RMarkdownCellType.CODE_CELL
        }
        else {
          RMarkdownCellType.MARKDOWN_CELL
        }
      }
      else -> {
        consumeMarkdown(originalLexer)
        RMarkdownCellType.MARKDOWN_CELL
      }
    }.elementType
  }

  private fun consumeMarkdown(lexer: Lexer) {
    while (lexer.tokenType != null &&
           lexer.tokenType != RMarkdownCodeMarkers.BACKTICK_WITH_LANG.elementType) {
      consumeToEndOfLine(lexer) // quoted line
    }
  }

  private fun consumeCode(lexer: Lexer): Boolean {
    while (true) {
      when (lexer.tokenType) {
        null -> return false
        RMarkdownCodeMarkers.BACKTICK_NO_LANG.elementType -> {
          lexer.advance()
          consumeToEndOfLine(lexer)
          return true
        }
        RMarkdownCodeMarkers.BACKTICK_WITH_LANG.elementType -> {
          return false
        }
        else -> consumeToEndOfLine(lexer)
      }
    }
  }
}

private fun consumeEndOfLine(lexer: Lexer) {
  if (lexer.tokenType == MarkdownTokenTypes.EOL) {
    lexer.advance()
  }
}

private fun consumeToEndOfLine(lexer: Lexer) {
  while (lexer.tokenType != null && lexer.tokenType != MarkdownTokenTypes.EOL) {
    lexer.advance()
  }
  consumeEndOfLine(lexer)
}

private fun toInterval(document: Document, marker: NotebookCellLinesLexer.Marker): NotebookCellLines.Interval {
  // for RMarkdown markers offset + length == nextMarker.offset, actually markers are intervals
  val startLine = document.getLineNumber(marker.offset)

  val endLine =
    if (marker.length == 0) startLine
    else document.getLineNumber(marker.offset + marker.length - 1)

  val markersAtLines =
    if (marker.type == NotebookCellLines.CellType.CODE) NotebookCellLines.MarkersAtLines.TOP_AND_BOTTOM
    else NotebookCellLines.MarkersAtLines.NO

  return NotebookCellLines.Interval(
    ordinal = marker.ordinal,
    type = marker.type,
    lines = startLine..endLine,
    markers = markersAtLines,
    language = marker.language // marker.language is provided in makeIntervals
  )
}