package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key

private val key = Key.create<NotebookCellSelectionModel>(NotebookCellSelectionModel::class.java.name)

interface NotebookCellSelectionModel {
  val primarySelectedCell: NotebookCellLines.Interval

  val selectedCells: List<NotebookCellLines.Interval>

  fun isSelectedCell(cell: NotebookCellLines.Interval): Boolean

  fun selectCell(cell: NotebookCellLines.Interval, makePrimary: Boolean = false)

  fun removeSecondarySelections()

  fun removeSelection(cell: NotebookCellLines.Interval)

  companion object {
    fun install(editor: Editor, selectionModel: NotebookCellSelectionModel?) {
      key.set(editor, selectionModel)
    }
  }
}

val Editor.cellSelectionModel: NotebookCellSelectionModel?
  get() = key.get(this)