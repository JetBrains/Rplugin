package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.RangeMarkerEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.LineMarkerRendererEx
import org.jetbrains.r.editor.RMarkdownEditorAppearance
import java.awt.Graphics
import java.awt.Rectangle

abstract class RMarkdownLineMarkerRenderer(private val inlayId: Long? = null) : LineMarkerRendererEx {
  override fun getPosition(): LineMarkerRendererEx.Position = LineMarkerRendererEx.Position.CUSTOM

  protected fun getInlayBounds(editor: EditorEx, linesRange: IntRange): Rectangle? {
    val startOffset = editor.document.getLineStartOffset(linesRange.first)
    val endOffset = editor.document.getLineEndOffset(linesRange.last)
    val inlays = editor.inlayModel.getBlockElementsInRange(startOffset, endOffset)

    val inlay = inlays.firstOrNull { it is RangeMarkerEx && it.id == inlayId }
    return inlay?.bounds
  }

  /**
   * see the history of [com.intellij.notebooks.ui.visualization.NotebookUtil.paintNotebookCellBackgroundGutter] in a git
   * It has a lot of changes of UI not related to R, may be something should be reverted or fixed
   */
  internal fun paintRNotebookCellBackgroundGutter(
    editor: EditorImpl,
    g: Graphics,
    r: Rectangle,
    top: Int,
    height: Int,
  ) {
    val diffViewOffset = 6  // randomly picked a number that fits well
    val borderWidth = RMarkdownEditorAppearance.getLeftBorderWidth()

    val fillX = r.width - borderWidth
    val fillWidth = borderWidth
    g.color = RMarkdownEditorAppearance.getCodeCellBackgroundColor(editor.colorsScheme)

    if (editor.editorKind == EditorKind.DIFF) {
      g.fillRect(fillX + diffViewOffset, top, fillWidth - diffViewOffset, height)
    }
    else {
      g.fillRect(fillX, top, fillWidth, height)
    }
  }
}