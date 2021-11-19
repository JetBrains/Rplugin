package org.jetbrains.r.editor

import com.intellij.openapi.editor.Editor
import org.jetbrains.plugins.notebooks.visualization.CaretBasedCellSelectionModel
import org.jetbrains.plugins.notebooks.visualization.NotebookCellSelectionModel
import org.jetbrains.plugins.notebooks.visualization.NotebookCellSelectionModelProvider

class RMarkdownCellSelectionModelProvider : NotebookCellSelectionModelProvider {
  override fun create(editor: Editor): NotebookCellSelectionModel =
    CaretBasedCellSelectionModel(editor)
}