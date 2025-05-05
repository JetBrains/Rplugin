package org.jetbrains.r.visualization

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.util.Consumer
import org.jetbrains.r.editor.ui.RMarkdownLineMarkerRenderer
import org.jetbrains.r.visualization.RNotebookCellLines.CellType
import org.jetbrains.r.visualization.ui.addEditorDocumentListener
import java.awt.Graphics
import java.awt.Rectangle

object RNotebookGutterLineMarkerManager {
  fun install(editor: EditorImpl) {
    editor.addEditorDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) = updateHighlighters(editor)
      override fun bulkUpdateFinished(document: Document) = updateHighlighters(editor)
    })

    editor.caretModel.addCaretListener(object : CaretListener {
      override fun caretPositionChanged(event: CaretEvent) {
        updateHighlighters(editor)
      }
    })

    updateHighlighters(editor)
  }

  fun updateHighlighters(editor: EditorEx) {
    val highlighters = editor.markupModel.allHighlighters.filter { it.lineMarkerRenderer is RMarkdownLineMarkerRenderer }
    highlighters.forEach { editor.markupModel.removeHighlighter(it) }

    val snapshot = RNotebookCellLines.getSnapshot(editor.document)

    for (interval in snapshot.intervals) {
      val startOffset = editor.document.getLineStartOffset(interval.lines.first)
      val endOffset = editor.document.getLineEndOffset(interval.lines.last)

      if (interval.type == CellType.CODE) {
        val changeAction = Consumer { o: RangeHighlighterEx ->
          o.lineMarkerRenderer = RMarkdownCodeCellBackgroundLineMarkerRenderer(o)
        }
        editor.markupModel.addRangeHighlighterAndChangeAttributes(null, startOffset, endOffset, HighlighterLayer.FIRST - 100, HighlighterTargetArea.LINES_IN_RANGE, false, changeAction)
      }
      else if (editor.editorKind != EditorKind.DIFF) {
        val changeAction = Consumer { o: RangeHighlighterEx ->
          o.lineMarkerRenderer = RMarkdownTextCellBackgroundLineMarkerRenderer(o)
        }
        editor.markupModel.addRangeHighlighterAndChangeAttributes(null, startOffset, endOffset, HighlighterLayer.FIRST - 100, HighlighterTargetArea.LINES_IN_RANGE, false, changeAction)
      }

      val notebookCellInlayManager = RNotebookCellInlayManager.get(editor) ?: throw AssertionError("Register inlay manager first")
      for (controller: RNotebookCellInlayController in notebookCellInlayManager.inlaysForInterval(interval)) {
        controller.createGutterRendererLineMarker(editor, interval)
      }
    }
  }
}


private class RMarkdownTextCellBackgroundLineMarkerRenderer(private val highlighter: RangeHighlighter) : RMarkdownLineMarkerRenderer() {
  override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
    editor as EditorImpl

    val lines = IntRange(editor.document.getLineNumber(highlighter.startOffset), editor.document.getLineNumber(highlighter.endOffset))

    paintCaretRow(editor, g, lines)
  }

  private fun paintCaretRow(editor: EditorImpl, g: Graphics, lines: IntRange) {
    if (editor.settings.isCaretRowShown) {
      val caretModel = editor.caretModel
      val caretLine = caretModel.logicalPosition.line
      if (caretLine in lines) {
        g.color = editor.colorsScheme.getColor(EditorColors.CARET_ROW_COLOR)
        g.fillRect(
          0,
          editor.visualLineToY(caretModel.visualPosition.line),
          g.clipBounds.width,
          editor.lineHeight
        )
      }
    }
  }
}


private class RMarkdownCodeCellBackgroundLineMarkerRenderer(
  private val highlighter: RangeHighlighter,
  private val boundsProvider: (Editor) -> Pair<Int, Int> = { editor ->
    val lines = IntRange(editor.document.getLineNumber(highlighter.startOffset), editor.document.getLineNumber(highlighter.endOffset))
    val top = editor.offsetToXY(editor.document.getLineStartOffset(lines.first)).y
    val height = editor.offsetToXY(editor.document.getLineEndOffset(lines.last)).y + editor.lineHeight - top
    top to height
  },
) : RMarkdownLineMarkerRenderer() {
  override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
    editor as EditorImpl
    val (top, height) = boundsProvider(editor)

    paintRNotebookCellBackgroundGutter(editor, g, r, top, height)
  }
}
