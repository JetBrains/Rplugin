package org.jetbrains.r.editor

import com.intellij.openapi.editor.Editor
import org.jetbrains.plugins.notebooks.editor.CaretBasedCellSelectionModel
import org.jetbrains.plugins.notebooks.editor.NotebookCellSelectionModel
import org.jetbrains.plugins.notebooks.editor.NotebookCellSelectionModelProvider

class RMarkdownCellSelectionModelProvider : NotebookCellSelectionModelProvider {
  override fun create(editor: Editor): NotebookCellSelectionModel =
    CaretBasedCellSelectionModel(editor)
}