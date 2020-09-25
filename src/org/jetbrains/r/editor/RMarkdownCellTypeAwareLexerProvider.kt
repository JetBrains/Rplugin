package org.jetbrains.r.editor

import com.intellij.lexer.Lexer
import com.intellij.lexer.MergeFunction
import com.intellij.lexer.MergingLexerAdapterBase
import com.intellij.psi.tree.IElementType
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.intellij.plugins.markdown.lang.lexer.MarkdownLexerAdapter
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.NotebookCellTypeAwareLexerProvider
import org.jetbrains.r.rmarkdown.RMarkdownLanguage

class RMarkdownCellTypeAwareLexerProvider: NotebookCellTypeAwareLexerProvider {
  override fun createNotebookCellTypeAwareLexer(): Lexer = RMarkdownMergingLangLexer()

  override fun getCellType(tokenType: IElementType): NotebookCellLines.CellType? =
    when(tokenType) {
      RMarkdownCellType.HEADER_CELL.elementType -> NotebookCellLines.CellType.RAW
      RMarkdownCellType.MARKDOWN_CELL.elementType -> NotebookCellLines.CellType.MARKDOWN
      RMarkdownCellType.CODE_CELL.elementType -> NotebookCellLines.CellType.CODE
      else -> null
    }
}

internal enum class RMarkdownCellType(val debugName: String) {
  HEADER_CELL("HEADER_CELL"),
  MARKDOWN_CELL("MARKDOWN_CELL"),
  CODE_CELL("CODE_CELL");

  val elementType = IElementType(debugName, RMarkdownLanguage)
}

internal class RMarkdownMergingLangLexer : MergingLexerAdapterBase(MarkdownLexerAdapter()) {

  override fun getMergeFunction(): MergeFunction = mergeFunction

  private val mergeFunction: MergeFunction = MergeFunction { type, originalLexer ->
    when {
      type == MarkdownTokenTypes.BACKTICK -> {
        consumeCode(originalLexer)
        RMarkdownCellType.CODE_CELL
      }
      tokenStart == 0 && tokenText == "---" -> {
        consumeHeader(originalLexer)
        RMarkdownCellType.HEADER_CELL
      }
      else -> {
        consumeMarkdown(originalLexer)
        RMarkdownCellType.MARKDOWN_CELL
      }
    }.elementType
  }

  private fun consumeMarkdown(lexer: Lexer) {
    while (lexer.tokenType != null && lexer.tokenType != MarkdownTokenTypes.BACKTICK) {
      lexer.advance()
    }
  }

  private fun consumeHeader(lexer: Lexer) {
    while (true) {
      when (lexer.tokenType) {
        null, MarkdownTokenTypes.BACKTICK -> return
        else -> {
          if (lexer.tokenType == MarkdownTokenTypes.TEXT && lexer.tokenText == "---") {
            lexer.advance()
            consumeEndOfLine(lexer)
            return
          }
          lexer.advance()
        }
      }
    }
  }

  private fun consumeCode(lexer: Lexer) {
    while (true) {
      when (lexer.tokenType) {
        null -> return
        MarkdownTokenTypes.BACKTICK -> {
          lexer.advance()
          consumeEndOfLine(lexer)
          return
        }
        else -> lexer.advance()
      }
    }
  }

  private fun consumeEndOfLine(lexer: Lexer) {
    if (lexer.tokenType == MarkdownTokenTypes.EOL) {
      lexer.advance()
    }
  }
}