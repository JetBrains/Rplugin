package org.jetbrains.r.visualization


import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.util.Consumer
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookCodeCellBackgroundLineMarkerRenderer
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookLineMarkerRenderer
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookTextCellBackgroundLineMarkerRenderer
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLines
import org.jetbrains.plugins.notebooks.visualization.addEditorDocumentListener


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
    val highlighters = editor.markupModel.allHighlighters.filter { it.lineMarkerRenderer is NotebookLineMarkerRenderer }
    highlighters.forEach { editor.markupModel.removeHighlighter(it) }

    val notebookCellLines = NotebookCellLines.get(editor)

    for (interval in notebookCellLines.intervals) {
      val startOffset = editor.document.getLineStartOffset(interval.lines.first)
      val endOffset = editor.document.getLineEndOffset(interval.lines.last)

      if (interval.type == NotebookCellLines.CellType.CODE) {
        val changeAction = Consumer { o: RangeHighlighterEx ->
          o.lineMarkerRenderer = NotebookCodeCellBackgroundLineMarkerRenderer(o)
        }
        editor.markupModel.addRangeHighlighterAndChangeAttributes(null, startOffset, endOffset, HighlighterLayer.FIRST - 100, HighlighterTargetArea.LINES_IN_RANGE, false, changeAction)
      }
      else if (editor.editorKind != EditorKind.DIFF) {
        val changeAction = Consumer { o: RangeHighlighterEx ->
          o.lineMarkerRenderer = NotebookTextCellBackgroundLineMarkerRenderer(o)
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
