package org.jetbrains.r.editor

import com.intellij.lexer.Lexer
import com.intellij.lexer.MergeFunction
import com.intellij.lexer.MergingLexerAdapterBase
import com.intellij.openapi.editor.Document
import com.intellij.psi.tree.IElementType
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.lexer.MarkdownLexerAdapter
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.NotebookCellLinesImpl
import org.jetbrains.plugins.notebooks.editor.NotebookCellLinesLexer
import org.jetbrains.plugins.notebooks.editor.NotebookCellLinesProvider
import org.jetbrains.r.rmarkdown.RMarkdownLanguage

class RMarkdownCellLinesProvider : NotebookCellLinesProvider, NotebookCellLinesLexer {
  private fun getCellType(tokenType: IElementType): NotebookCellLines.CellType? =
    when (tokenType) {
      RMarkdownCellType.MARKDOWN_CELL.elementType -> NotebookCellLines.CellType.MARKDOWN
      RMarkdownCellType.CODE_CELL.elementType -> NotebookCellLines.CellType.CODE
      else -> null
    }

  override fun shouldParseWholeFile(): Boolean = true

  override fun create(document: Document): NotebookCellLines =
    NotebookCellLinesImpl.get(document, this)

  override fun markerSequence(chars: CharSequence, ordinalIncrement: Int, offsetIncrement: Int): Sequence<NotebookCellLines.Marker> =
    sequence {
      var lastMarker: NotebookCellLines.Marker? = null
      val seq = NotebookCellLinesLexer.defaultMarkerSequence({ RMarkdownMergingLangLexer() }, this@RMarkdownCellLinesProvider::getCellType,
                                                             chars, ordinalIncrement, offsetIncrement)

      for(marker in seq) {
        lastMarker = marker
        yield(marker)
      }

      if (lastMarker?.type == NotebookCellLines.CellType.CODE && chars.endsWith('\n')) {
        yield(NotebookCellLines.Marker(
          ordinal = lastMarker.ordinal + 1,
          type = NotebookCellLines.CellType.MARKDOWN,
          offset = chars.length + offsetIncrement,
          length = 0
        ))
      }
    }
}

internal enum class RMarkdownCellType(val debugName: String) {
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
        } else {
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