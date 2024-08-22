package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookLineMarkerRenderer
import org.jetbrains.plugins.notebooks.ui.visualization.paintNotebookCellBackgroundGutter
import java.awt.Graphics
import java.awt.Rectangle

class RMarkdownOutputCellGutterLineMarkerRenderer(private val lines: IntRange, inlayId: Long) : NotebookLineMarkerRenderer(inlayId) {
  override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
    editor as EditorImpl
    val inlayBounds = getInlayBounds(editor, lines) ?: return
    paintNotebookCellBackgroundGutter(editor, g, r, lines, inlayBounds.y, inlayBounds.height)
  }
}