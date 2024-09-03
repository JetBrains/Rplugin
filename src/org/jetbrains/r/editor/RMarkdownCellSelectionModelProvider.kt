package org.jetbrains.r.editor

import com.intellij.openapi.editor.Editor
import com.intellij.notebooks.visualization.CaretBasedCellSelectionModel
import com.intellij.notebooks.visualization.NotebookCellSelectionModel
import com.intellij.notebooks.visualization.NotebookCellSelectionModelProvider

class RMarkdownCellSelectionModelProvider : NotebookCellSelectionModelProvider {
  override fun create(editor: Editor): NotebookCellSelectionModel =
    CaretBasedCellSelectionModel(editor)
}