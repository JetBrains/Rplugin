package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.LineMarkerRenderer
import com.intellij.openapi.util.TextRange
import org.jetbrains.plugins.notebooks.visualization.NotebookIntervalPointer
import org.jetbrains.plugins.notebooks.visualization.r.ui.UiCustomizer
import javax.swing.JComponent

class NotebookInlayComponentInterval(val cell: NotebookIntervalPointer, editor: EditorImpl) : NotebookInlayComponent(editor) {
  override fun updateCellSeparator() {
    if (!UiCustomizer.instance.showUpdateCellSeparator) {
      return
    }

    if (separatorHighlighter != null) {
      editor.markupModel.removeHighlighter(separatorHighlighter!!)
    }

    try {
      val interval = cell.get() ?: return
      val doc = editor.document
      val textRange = TextRange(doc.getLineStartOffset(interval.lines.first), doc.getLineEndOffset(interval.lines.last))
      separatorHighlighter = createSeparatorHighlighter(editor, textRange)
    }
    catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

private fun createSeparatorHighlighter(editor: EditorImpl, textRange: TextRange) =
  editor.markupModel.addRangeHighlighter(textRange.startOffset, textRange.endOffset,
                                         HighlighterLayer.SYNTAX - 1, null,
                                         HighlighterTargetArea.LINES_IN_RANGE).apply {

    customRenderer = NotebookInlayComponent.separatorRenderer
    lineMarkerRenderer = LineMarkerRenderer { _, g, r ->
      val gutterWidth = ((editor as EditorEx).gutterComponentEx as JComponent).width

      val y = r.y + r.height - editor.lineHeight
      g.color = editor.colorsScheme.getColor(EditorColors.RIGHT_MARGIN_COLOR)
      g.drawLine(0, y, gutterWidth + 10, y)
    }
  }