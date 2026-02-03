package org.jetbrains.r.editor

import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.lexer.MergeFunction
import com.intellij.lexer.MergingLexerAdapterBase
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.r.psi.rmarkdown.RMarkdownLanguage
import org.intellij.plugins.markdown.lang.MarkdownLanguage
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.lexer.MarkdownLexerAdapter
import org.jetbrains.r.rmarkdown.RmdCellLanguageProvider
import org.jetbrains.r.visualization.RIntervalsGenerator
import org.jetbrains.r.visualization.RNotebookCellLines.CellType
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import org.jetbrains.r.visualization.RNotebookCellLines.MarkersAtLines
import kotlin.math.max


internal object RMarkdownIntervalsGenerator : RIntervalsGenerator {
  override fun makeIntervals(document: Document, event: DocumentEvent?): List<Interval> {
    val resultIntervals = mutableListOf<Interval>()

    val markdownLanguage = MarkdownLanguage.INSTANCE
    val documentTimestamp = document.modificationStamp
    val chars = document.charsSequence
    val codeIntervalLanguageParser = CodeIntervalLanguageParser()

    val lexer = RMarkdownMergingLangLexer()
    lexer.start(chars)

    while (lexer.tokenType != null) {
      when (lexer.tokenType) {
        RMarkdownCellType.MARKDOWN_CELL.elementType -> {
          resultIntervals += Interval(
            ordinal = resultIntervals.size,
            type = CellType.MARKDOWN,
            lines = lexer.getCurrentLinesIn(document),
            markers = MarkersAtLines.NO,
            language = markdownLanguage
          )
        }
        RMarkdownCellType.CODE_CELL.elementType -> {
          resultIntervals += Interval(
            ordinal = resultIntervals.size,
            type = CellType.CODE,
            lines = lexer.getCurrentLinesIn(document),
            markers = MarkersAtLines.TOP_AND_BOTTOM,
            language = codeIntervalLanguageParser.parse(cellText = lexer.tokenSequence) ?: PlainTextLanguage.INSTANCE
          )
        }
        else -> Unit
      }

      lexer.advance()
    }

    if (resultIntervals.lastOrNull()?.type == CellType.CODE && chars.endsWith('\n')) {
      val line = document.getLineNumber(chars.length)

      resultIntervals += Interval(
        ordinal = resultIntervals.size,
        type = CellType.MARKDOWN,
        lines = line..line,
        markers = MarkersAtLines.NO,
        language = markdownLanguage,
      )
    }

    if (resultIntervals.isEmpty()) {
      resultIntervals += Interval(
        ordinal = 0,
        type = CellType.MARKDOWN,
        lines = 0..max(0, document.lineCount - 1),
        markers = MarkersAtLines.NO,
        language = markdownLanguage,
      )
    }

    if (documentTimestamp != document.modificationStamp) {
      thisLogger().error("Document was modified during intervals creation")
      return makeIntervals(document, event)
    }

    return resultIntervals
  }
}

private fun RMarkdownMergingLangLexer.getCurrentLinesIn(document: Document): IntRange {
  val offset = currentPosition.offset
  val length = tokenText.length
  val startLine = document.getLineNumber(offset)
  val endLine = document.getLineNumber(offset + length - 1)
  return startLine..endLine
}


private class CodeIntervalLanguageParser {
  private val langMap = RmdCellLanguageProvider.getAllLanguages()
  private val maxLangSize: Int = langMap.keys.maxOfOrNull { it.length } ?: 0

  private fun parse(cellText: CharSequence, maxLangSize: Int): String? {
    val prefix = "```{"
    if (!cellText.startsWith(prefix)) return null
    return cellText.drop(prefix.length).take(maxLangSize + 1).takeWhile { it.isLetterOrDigit() }.toString()
  }

  fun parse(cellText: CharSequence): Language? {
    val cellLanguageText = parse(cellText, maxLangSize)
    return langMap.get(cellLanguageText)
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