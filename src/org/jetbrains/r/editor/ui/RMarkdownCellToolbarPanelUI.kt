package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.r.editor.RMarkdownEditorAppearance
import org.jetbrains.r.visualization.ui.use
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.plaf.PanelUI
import kotlin.math.max

internal class RMarkdownCellToolbarPanelUI(private val editor: EditorImpl) : PanelUI() {
  override fun getPreferredSize(c: JComponent): Dimension {
    val preferredSize = super.getPreferredSize(c)
    val height = max(preferredSize?.height ?: 0, RMarkdownEditorAppearance.getToolbarHeight())
    val width = max(preferredSize?.width ?: 0, editor.scrollingModel.visibleArea.width)
    return Dimension(width, height)
  }

  override fun paint(g: Graphics, c: JComponent) {
    @Suppress("NAME_SHADOWING") g.create().use { g ->
      g.color = RMarkdownEditorAppearance.getCodeCellBackgroundColor(editor.colorsScheme)
      g.fillRect(0, 0, editor.scrollPane.let { it.viewport.width - it.verticalScrollBar.width }, c.height)
    }
    super.paint(g, c)
  }
}