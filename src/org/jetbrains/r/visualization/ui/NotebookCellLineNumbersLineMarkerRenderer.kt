package org.jetbrains.r.visualization.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.impl.view.FontLayoutService
import com.intellij.openapi.editor.markup.RangeHighlighter
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookLineMarkerRenderer
import org.jetbrains.plugins.notebooks.ui.visualization.notebookAppearance
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.max
import kotlin.math.min

class NotebookCellLineNumbersLineMarkerRenderer(private val highlighter: RangeHighlighter) : NotebookLineMarkerRenderer() {
  override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
    if (!editor.settings.isLineNumbersShown) return

    val lines = IntRange(editor.document.getLineNumber(highlighter.startOffset), editor.document.getLineNumber(highlighter.endOffset))
    val visualLineStart = editor.xyToVisualPosition(Point(0, g.clip.bounds.y)).line
    val visualLineEnd = editor.xyToVisualPosition(Point(0, g.clip.bounds.run { y + height })).line
    val logicalLineStart = editor.visualToLogicalPosition(VisualPosition(visualLineStart, 0)).line
    val logicalLineEnd = editor.visualToLogicalPosition(VisualPosition(visualLineEnd, 0)).line

    g.font = editor.colorsScheme.getFont(EditorFontType.PLAIN).let {
      it.deriveFont(max(1f, it.size2D - 1f))
    }
    g.color = editor.colorsScheme.getColor(EditorColors.LINE_NUMBERS_COLOR)

    val notebookAppearance = editor.notebookAppearance
    var previousVisualLine = -1
    // The first line of the cell is the delimiter, don't draw the line number for it.
    for (logicalLine in max(logicalLineStart, lines.first + 1)..min(logicalLineEnd, lines.last)) {
      val visualLine = editor.logicalToVisualPosition(LogicalPosition(logicalLine, 0)).line
      if (previousVisualLine == visualLine) continue  // If a region is folded, it draws only the first line number.
      previousVisualLine = visualLine

      if (visualLine < visualLineStart) continue
      if (visualLine > visualLineEnd) break

      // TODO conversions from document position to Y are expensive and should be cached.
      val yTop = editor.visualLineToY(visualLine)
      val lineNumber = logicalLine - lines.first
      val text: String = lineNumber.toString()
      val left =
        (
          r.width
          - FontLayoutService.getInstance().stringWidth(g.fontMetrics, text)
          - notebookAppearance.LINE_NUMBERS_MARGIN
          - notebookAppearance.getLeftBorderWidth()
        )
      g.drawString(text, left, yTop + editor.ascent)
    }
  }
}