package org.jetbrains.plugins.notebooks.editor

import com.intellij.lang.LanguageExtension

private const val ID: String = "org.jetbrains.plugins.notebooks.notebookCellTypeAwareLexerProvider"

interface NotebookCellTypeAwareLexerProvider: JupyterNotebookCellLines.LexerProvider {
  companion object : LanguageExtension<NotebookCellTypeAwareLexerProvider>(ID)
}
