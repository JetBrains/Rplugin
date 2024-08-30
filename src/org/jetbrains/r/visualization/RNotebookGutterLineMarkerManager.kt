package org.jetbrains.r.visualization


import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.util.Consumer
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookCodeCellBackgroundLineMarkerRenderer
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookLineMarkerRenderer
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookTextCellBackgroundLineMarkerRenderer
import org.jetbrains.plugins.notebooks.ui.visualization.notebookAppearance
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLines
import org.jetbrains.plugins.notebooks.visualization.addEditorDocumentListener
import org.jetbrains.r.visualization.ui.NotebookCellLineNumbersLineMarkerRenderer


class RNotebookGutterLineMarkerManager {

  fun attachHighlighters(editor: EditorEx) {
    editor.addEditorDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) = putHighlighters(editor)
      override fun bulkUpdateFinished(document: Document) = putHighlighters(editor)
    })

    editor.caretModel.addCaretListener(object : CaretListener {
      override fun caretPositionChanged(event: CaretEvent) {
        putHighlighters(editor)
      }
    })

    putHighlighters(editor)
  }

  fun putHighlighters(editor: EditorEx) {
    val highlighters = editor.markupModel.allHighlighters.filter { it.lineMarkerRenderer is NotebookLineMarkerRenderer }
    highlighters.forEach { editor.markupModel.removeHighlighter(it) }

    val notebookCellLines = NotebookCellLines.get(editor)

    for (interval in notebookCellLines.intervals) {
      val startOffset = editor.document.getLineStartOffset(interval.lines.first)
      val endOffset = editor.document.getLineEndOffset(interval.lines.last)

      if (interval.type == NotebookCellLines.CellType.CODE && editor.notebookAppearance.shouldShowCellLineNumbers() && editor.editorKind != EditorKind.DIFF) {
        editor.markupModel.addRangeHighlighter(
          null,
          startOffset,
          endOffset,
          HighlighterLayer.FIRST - 99,  // Border should be seen behind any syntax highlighting, selection or any other effect.
          HighlighterTargetArea.LINES_IN_RANGE
        ).also {
          it.lineMarkerRenderer = NotebookCellLineNumbersLineMarkerRenderer(it)
        }
      }

      if (interval.type == NotebookCellLines.CellType.CODE) {
        val changeAction = Consumer { o: RangeHighlighterEx ->
          o.lineMarkerRenderer = NotebookCodeCellBackgroundLineMarkerRenderer(o)
        }
        editor.markupModel.addRangeHighlighterAndChangeAttributes(null, startOffset, endOffset, HighlighterLayer.FIRST - 100, HighlighterTargetArea.LINES_IN_RANGE, false, changeAction)
      } else if (editor.editorKind != EditorKind.DIFF) {
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

  companion object {
    fun install(editor: EditorEx): RNotebookGutterLineMarkerManager {
      val instance = RNotebookGutterLineMarkerManager()
      instance.attachHighlighters(editor)

      return instance
    }
  }
}
