package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.notebooks.visualization.NotebookCellLines
import com.intellij.notebooks.visualization.outputs.NotebookOutputDataKey
import com.intellij.notebooks.visualization.outputs.NotebookOutputDataKeyExtractor
import com.intellij.notebooks.visualization.outputs.statistic.NotebookOutputKeyType
import org.jetbrains.r.rendering.chunk.ChunkPath
import org.jetbrains.r.rendering.chunk.RMarkdownInlayDescriptor
import org.jetbrains.r.visualization.inlays.InlayOutput

data class RMarkdownInlayOutputDataKey(val inlayOutput: InlayOutput): NotebookOutputDataKey {
  override fun getStatisticKey() = NotebookOutputKeyType.R_MARKDOWN
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