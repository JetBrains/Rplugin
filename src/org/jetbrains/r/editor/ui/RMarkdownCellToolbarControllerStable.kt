package org.jetbrains.r.editor.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.RangeMarkerEx
import com.intellij.openapi.editor.impl.EditorEmbeddedComponentManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import org.jetbrains.r.editor.RMarkdownEditorAppearance
import org.jetbrains.r.visualization.RNotebookCellInlayController
import org.jetbrains.r.visualization.RNotebookCellLines.CellType
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import org.jetbrains.r.visualization.RNotebookIntervalPointer
import org.jetbrains.r.visualization.RNotebookIntervalPointerFactory
import java.awt.Graphics
import java.awt.Rectangle

internal class RMarkdownCellToolbarControllerStable private constructor(
  val editor: EditorImpl,
  override val factory: Factory,
  private val intervalPointer: RNotebookIntervalPointer,
  inlayOffset: Int,
) : RNotebookCellInlayController, EditorCustomElementRenderer {

  private val panel = RMarkdownCellToolbarPanel(editor, intervalPointer)

  override fun calcWidthInPixels(inlay: Inlay<*>): Int = inlay.editor.scrollingModel.visibleArea.width

  override fun calcHeightInPixels(inlay: Inlay<*>): Int = RMarkdownEditorAppearance.getToolbarHeight()

  override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes): Unit = Unit

  override val inlay: Inlay<*> =
    EditorEmbeddedComponentManager.getInstance().addComponent(
      editor,
      panel,
      EditorEmbeddedComponentManager.Properties(
        EditorEmbeddedComponentManager.ResizePolicy.none(),
        null,
        isRelatedToPrecedingText,
        true,
        RMarkdownEditorAppearance.CELL_SPACERS_INLAY_PRIORITY,
        inlayOffset
      )
    )!!

  override fun createGutterRendererLineMarker(editor: EditorEx, interval: Interval) {
    val startOffset = editor.document.getLineStartOffset(interval.lines.first)
    val endOffset = editor.document.getLineEndOffset(interval.lines.last)

    val rangeHighlighter = editor.markupModel.addRangeHighlighter(null, startOffset, endOffset, HighlighterLayer.FIRST - 100, HighlighterTargetArea.LINES_IN_RANGE)
    rangeHighlighter.lineMarkerRenderer = RMarkdownCellToolbarGutterLineMarkerRenderer(interval.lines, (inlay as RangeMarkerEx).id)
  }

  class Factory : RNotebookCellInlayController.Factory {
    override fun compute(
      editor: EditorImpl,
      currentControllers: Collection<RNotebookCellInlayController>,
      interval: Interval,
    ): RNotebookCellInlayController? {

      return when (interval.type) {
        CellType.CODE -> {
          val offset = getOffset(editor.document, RNotebookIntervalPointerFactory.get(editor).create(interval))
          val intervalPointer = RNotebookIntervalPointerFactory.get(editor).create(interval)
          currentControllers.asSequence()
            .filterIsInstance<RMarkdownCellToolbarControllerStable>()
            .firstOrNull {
              it.intervalPointer.get() == interval
              && it.inlay.offset == offset
            }
          ?: RMarkdownCellToolbarControllerStable(editor, this, intervalPointer, offset)
        }
        CellType.MARKDOWN,
          -> null
      }
    }

    private fun getOffset(document: Document, intervalPointer: RNotebookIntervalPointer): Int =
      document.getLineStartOffset(intervalPointer.get()!!.lines.first)
  }

  companion object {
    private const val isRelatedToPrecedingText: Boolean = true
  }
}
