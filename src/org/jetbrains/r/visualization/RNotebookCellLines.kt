package org.jetbrains.r.visualization

import com.intellij.notebooks.visualization.NotebookCellLines
import com.intellij.openapi.editor.Document

class RNotebookCellLines {

  companion object {
    val INTERVAL_LANGUAGE_KEY = NotebookCellLines.INTERVAL_LANGUAGE_KEY

    fun get(document: Document): NotebookCellLines =
      NotebookCellLines.get(document)
  }
}