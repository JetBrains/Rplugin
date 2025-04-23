package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import java.awt.Graphics
import java.awt.Rectangle

class RMarkdownOutputCellGutterLineMarkerRenderer(private val lines: IntRange, inlayId: Long) : RMarkdownLineMarkerRenderer(inlayId) {
  override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
    editor as EditorImpl
    val inlayBounds = getInlayBounds(editor, lines) ?: return
    paintRNotebookCellBackgroundGutter(editor, g, r, inlayBounds.y, inlayBounds.height)
  }
}
