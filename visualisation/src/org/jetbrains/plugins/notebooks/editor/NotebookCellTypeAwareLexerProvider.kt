package org.jetbrains.plugins.notebooks.editor

import com.intellij.lang.LanguageExtension
import com.intellij.lexer.Lexer
import com.intellij.psi.tree.IElementType

private const val ID: String = "org.jetbrains.plugins.notebooks.notebookCellTypeAwareLexerProvider"

interface NotebookCellTypeAwareLexerProvider {

  fun createNotebookCellTypeAwareLexer(): Lexer

  fun getCellType(tokenType: IElementType): NotebookCellLines.CellType?

  fun shouldParseWholeFile(): Boolean = false

  companion object : LanguageExtension<NotebookCellTypeAwareLexerProvider>(ID)
}
