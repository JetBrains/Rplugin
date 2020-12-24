package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputDataKey
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputDataKeyExtractor


class RMarkdownOutputDataKeyExtractor: NotebookOutputDataKeyExtractor {
  override fun extract(editor: EditorImpl, interval: NotebookCellLines.Interval): List<NotebookOutputDataKey>? {
    if (!isRMarkdown(editor)) return null
    if (interval.type != NotebookCellLines.CellType.CODE) return null
    return emptyList()
  }
}