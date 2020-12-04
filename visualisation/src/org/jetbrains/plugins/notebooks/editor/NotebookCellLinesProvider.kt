package org.jetbrains.plugins.notebooks.editor

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Document

private const val ID: String = "org.jetbrains.plugins.notebooks.notebookCellLinesProvider"

interface NotebookCellLinesProvider {
  fun create(document: Document): NotebookCellLines

  companion object : LanguageExtension<NotebookCellLinesProvider>(ID)
}
