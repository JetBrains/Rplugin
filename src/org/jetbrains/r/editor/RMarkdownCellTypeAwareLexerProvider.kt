package org.jetbrains.r.editor

import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.lexer.MergeFunction
import com.intellij.lexer.MergingLexerAdapterBase
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.util.keyFMap.KeyFMap
import org.intellij.plugins.markdown.lang.MarkdownLanguage
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.lexer.MarkdownLexerAdapter
import org.jetbrains.r.rmarkdown.RMarkdownLanguage
import org.jetbrains.r.rmarkdown.RmdCellLanguageProvider
import org.jetbrains.r.visualization.RIntervalsGenerator
import org.jetbrains.r.visualization.RNotebookCellLines
import org.jetbrains.r.visualization.RNotebookCellLines.*
import org.jetbrains.r.visualization.RNotebookCellLinesLexerMarker


internal object RMarkdownIntervalsGenerator : RIntervalsGenerator {
  override fun makeIntervals(document: Document, event: DocumentEvent?): List<Interval> {
    val markers = markerSequence(document.charsSequence).toList()
    return markers.map { toInterval(document, it) }
  }

  private fun markerSequence(chars: CharSequence): Sequence<RNotebookCellLinesLexerMarker> =
    sequence {
      val defaultLanguage: Language = PlainTextLanguage.INSTANCE
      val langMap = RmdCellLanguageProvider.getAllLanguages()
      val maxLangSize: Int = langMap.keys.maxOfOrNull { it.length } ?: 0

      val markdownDataPair = Pair(CellType.MARKDOWN,
                                  KeyFMap.EMPTY_MAP.plus(RNotebookCellLines.INTERVAL_LANGUAGE_KEY, MarkdownLanguage.INSTANCE))
      val codeDataCache = mutableMapOf<Language, Pair<CellType, KeyFMap>>()

      val seq = defaultMarkerSequence(
        getCellTypeAndData = { lexer ->
          when (lexer.tokenType) {
            RMarkdownCellType.MARKDOWN_CELL.elementType -> markdownDataPair
            RMarkdownCellType.CODE_CELL.elementType -> {
              val cellText = lexer.tokenText
              val cellLanguageText = parseLanguage(cellText, maxLangSize)
              val language = langMap.getOrDefault(cellLanguageText, defaultLanguage)
              codeDataCache.getOrPut(language) {
                Pair(CellType.CODE, KeyFMap.EMPTY_MAP.plus(RNotebookCellLines.INTERVAL_LANGUAGE_KEY, language))
              }
            }
            else -> null
          }
        },
        chars
      )

      var lastMarker: RNotebookCellLinesLexerMarker? = null
      for (marker in seq) {
        lastMarker = marker
        yield(marker)
      }

      if (lastMarker?.type == CellType.CODE && chars.endsWith('\n')) {
        yield(RNotebookCellLinesLexerMarker(
          ordinal = lastMarker.ordinal + 1,
          type = CellType.MARKDOWN,
          offset = chars.length,
          length = 0,
          data = markdownDataPair.second
        ))
      }

      if (lastMarker == null) {
        yield(RNotebookCellLinesLexerMarker(
          ordinal = 0,
          type = CellType.MARKDOWN,
          offset = 0,
          length = 0,
          data = markdownDataPair.second,
        ))
      }
    }

  private fun parseLanguage(cellText: CharSequence, maxLangSize: Int): String? {
    val prefix = "```{"
    if (!cellText.startsWith(prefix)) return null
    return cellText.drop(prefix.length).take(maxLangSize + 1).takeWhile { it.isLetterOrDigit() }.toString()
  }
}


private enum class RMarkdownCellType(private val debugName: String) {
  MARKDOWN_CELL("MARKDOWN_CELL"),
  CODE_CELL("CODE_CELL");

  val elementType = IElementType(debugName, RMarkdownLanguage)
}


private enum class RMarkdownCodeMarkers(debugName: String) {
  BACKTICK_WITH_LANG("BACKTICK_WITH_LANG"),
  BACKTICK_NO_LANG("BACKTICK_NO_LANG");

  val elementType = IElementType(debugName, RMarkdownLanguage)
}


/**
 * merge ```{r} to BACKTICK_WITH_LANG and ``` to BACKTICK_NO_LANG
 * ```{ and ``` should be at the start of line, otherwise they are ignored
 */
private class RMarkdownMapBackticks : MergingLexerAdapterBase(MarkdownLexerAdapter()) {
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


private class RMarkdownMergingLangLexer : MergingLexerAdapterBase(RMarkdownMapBackticks()) {

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

private fun toInterval(document: Document, marker: RNotebookCellLinesLexerMarker): Interval {
  // for RMarkdown markers offset + length == nextMarker.offset, actually markers are intervals
  val startLine = document.getLineNumber(marker.offset)

  val endLine =
    if (marker.length == 0) startLine
    else document.getLineNumber(marker.offset + marker.length - 1)

  val markersAtLines =
    if (marker.type == CellType.CODE) MarkersAtLines.TOP_AND_BOTTOM
    else MarkersAtLines.NO

  return Interval(
    ordinal = marker.ordinal,
    type = marker.type,
    lines = startLine..endLine,
    markers = markersAtLines,
    data = marker.data,
  )
}

private fun defaultMarkerSequence(
  getCellTypeAndData: (lexer: RMarkdownMergingLangLexer) -> Pair<CellType, KeyFMap>?,
  chars: CharSequence,
): Sequence<RNotebookCellLinesLexerMarker> = sequence {
  val lexer = RMarkdownMergingLangLexer()
  lexer.start(chars, 0, chars.length)
  var ordinal = 0
  while (lexer.tokenType != null) {
    getCellTypeAndData(lexer)?.let { (type, data) ->
      yield(RNotebookCellLinesLexerMarker(
        ordinal = ordinal++,
        type = type,
        offset = lexer.currentPosition.offset,
        length = lexer.tokenText.length,
        data = data,
      ))
    }
    lexer.advance()
  }
}