package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLines
import org.jetbrains.plugins.notebooks.visualization.outputs.NotebookOutputDataKey
import org.jetbrains.plugins.notebooks.visualization.outputs.NotebookOutputDataKeyExtractor
import org.jetbrains.plugins.notebooks.visualization.r.inlays.InlayOutput
import org.jetbrains.r.rendering.chunk.ChunkPath
import org.jetbrains.r.rendering.chunk.RMarkdownInlayDescriptor

data class RMarkdownInlayOutputDataKey(val inlayOutput: InlayOutput): NotebookOutputDataKey {
  override fun getContentForDiffing(): String = inlayOutput.data

}

class RMarkdownOutputDataKeyExtractor: NotebookOutputDataKeyExtractor {
  override fun extract(editor: EditorImpl, interval: NotebookCellLines.Interval): List<NotebookOutputDataKey>? {
    if (!isRMarkdown(editor)) return null
    if (interval.type != NotebookCellLines.CellType.CODE) return null
    if (!isEnabled) return null

    val outputs = RMarkdownInlayDescriptor.getOutputs(ChunkPath.create(editor, interval)!!)
    return outputs.map { RMarkdownInlayOutputDataKey(it) }
  }

  companion object {
    private const val isEnabled: Boolean = false
  }
}