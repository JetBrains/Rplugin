package org.jetbrains.r.editor.ui

import com.intellij.notebooks.ui.visualization.NotebookUtil.paintNotebookCellBackgroundGutter
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.editor.ex.RangeMarkerEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.notebooks.visualization.NotebookCellInlayController
import com.intellij.notebooks.visualization.NotebookCellLines
import com.intellij.notebooks.visualization.NotebookIntervalPointer
import com.intellij.notebooks.visualization.NotebookIntervalPointerFactory
import com.intellij.notebooks.visualization.ui.EditorCellView
import org.jetbrains.r.editor.ui.RMarkdownOutputInlayControllerUtil.addBlockElement
import org.jetbrains.r.editor.ui.RMarkdownOutputInlayControllerUtil.disposeComponent
import org.jetbrains.r.editor.ui.RMarkdownOutputInlayControllerUtil.extractOffset
import org.jetbrains.r.editor.ui.RMarkdownOutputInlayControllerUtil.isInViewportByY
import org.jetbrains.r.editor.ui.RMarkdownOutputInlayControllerUtil.setupInlayComponent
import org.jetbrains.r.editor.ui.RMarkdownOutputInlayControllerUtil.updateInlayWidth
import org.jetbrains.r.rendering.chunk.ChunkPath
import org.jetbrains.r.rendering.chunk.RMarkdownInlayDescriptor
import org.jetbrains.r.visualization.inlays.RInlayDimensions
import org.jetbrains.r.visualization.inlays.components.InlayProgressStatus
import java.awt.Graphics
import java.awt.Rectangle

class RMarkdownOutputInlayController private constructor(
  val editor: EditorImpl,
  override val factory: NotebookCellInlayController.Factory,
  override val intervalPointer: NotebookIntervalPointer,
  offset: Int,
) : NotebookCellInlayController, RMarkdownNotebookOutput {

  private val notebook: RMarkdownNotebook = RMarkdownNotebook.installIfNotExists(editor)
  private val inlayComponent: NotebookInlayComponent = addInlayComponent(editor, intervalPointer, offset)
  override val inlay: Inlay<*> = inlayComponent.inlay!!

  init {
    notebook.update(this)
    updateOutputs(resetComponent = false)
    Disposer.register(inlayComponent.inlay!!, Disposable { dispose() })
  }

  override fun paintGutter(editor: EditorImpl,
                           g: Graphics,
                           r: Rectangle,
                           interval: NotebookCellLines.Interval) {
    val inlayBounds = inlay.bounds ?: return
    paintNotebookCellBackgroundGutter(editor, g, r, inlayBounds.y, inlayBounds.height)
  }

  override fun createGutterRendererLineMarker(editor: EditorEx, interval: NotebookCellLines.Interval, cellView: EditorCellView) {
    val startOffset = editor.document.getLineStartOffset(interval.lines.first)
    val endOffset = editor.document.getLineEndOffset(interval.lines.last)

    cellView.addCellHighlighter {
      val rangeHighlighter = editor.markupModel.addRangeHighlighter(null, startOffset, endOffset, HighlighterLayer.FIRST - 100, HighlighterTargetArea.LINES_IN_RANGE)
      rangeHighlighter.lineMarkerRenderer = RMarkdownOutputCellGutterLineMarkerRenderer(interval.lines, (inlay as RangeMarkerEx).id)
      rangeHighlighter
    }
  }

  override fun addText(text: String, outputType: Key<*>) {
    invokeLater {
      inlayComponent.addText(text, outputType)
    }
  }

  override fun clearOutputs(removeFiles: Boolean) {
    invokeLater { // preserve order with addText() calls
      if (removeFiles) {
        makeChunkPath()?.let {
          RMarkdownInlayDescriptor.cleanup(it)
        }
      }
      resetComponent()
    }
  }

  override fun updateOutputs() {
    updateOutputs(resetComponent = true)
  }

  override fun updateProgressStatus(progressStatus: InlayProgressStatus) {
    invokeLater {
      if (Disposer.isDisposed(inlay)) {
        return@invokeLater
      }

      inlayComponent.updateProgressStatus(progressStatus)
    }
  }

  private fun updateOutputs(resetComponent: Boolean) {
    invokeLater {
      if (resetComponent) {
        resetComponent()
      }
      else {
        // reuse inlayComponent, check that it is valid
        if (Disposer.isDisposed(inlay)) {
          return@invokeLater
        }
      }

      val outputs = makeChunkPath()?.let { RMarkdownInlayDescriptor.getInlayOutputs(it, editor) } ?: emptyList()
      if (outputs.isEmpty()) return@invokeLater

      inlayComponent.addInlayOutputs(outputs)
    }
  }

  override fun dispose() {
    notebook.remove(this)
    disposeComponent(inlayComponent)
  }

  private fun resetComponent() {
    if (editor.isDisposed)
      return

    inlayComponent.clearOutputs()
  }

  private fun addInlayComponent(editor: EditorImpl, intervalPointer: NotebookIntervalPointer, offset: Int): NotebookInlayComponent {
    RInlayDimensions.init(editor)
    val inlayComponent = NotebookInlayComponent(intervalPointer, editor)

    if (!editor.inlayModel.isInBatchMode) {
      // TODO may be no need in `setBounds(...)` at all. In batch mode `offsetToXY` is not available

      // On editor creation it has 0 width
      val gutterWidth = (editor.gutter as EditorGutterComponentEx).width
      var editorWideWidth = editor.component.width - inlayComponent.width - gutterWidth - RInlayDimensions.rightBorder
      if (editorWideWidth <= 0) {
        editorWideWidth = RInlayDimensions.width
      }

      inlayComponent.setBounds(0, editor.offsetToXY(offset).y + editor.lineHeight, editorWideWidth, RInlayDimensions.smallHeight)
    }
    editor.contentComponent.add(inlayComponent)
    val inlay = addBlockElement(editor, offset, inlayComponent)

    inlayComponent.assignInlay(inlay)
    setupInlayComponent(editor, inlayComponent)

    return inlayComponent
  }

  // onViewportChange is called inside invokeLater and this is the source of flickering
  override fun onViewportChange() {
    if (Disposer.isDisposed(inlay))
      return

    updateInlayWidth(editor, inlayComponent)
    inlayComponent.onViewportChange(isInViewportByY(editor, inlayComponent.bounds))
  }

  private fun makeChunkPath(): ChunkPath? =
    intervalPointer.get()?.let { ChunkPath.create(editor, it) }

  class Factory : NotebookCellInlayController.Factory {
    override fun compute(editor: EditorImpl,
                         currentControllers: Collection<NotebookCellInlayController>,
                         intervalIterator: ListIterator<NotebookCellLines.Interval>
    ): NotebookCellInlayController? {
      if (!isRMarkdown(editor))
        return null

      val interval: NotebookCellLines.Interval = intervalIterator.next()
      val offset = extractOffset(editor.document, interval)

      return when (interval.type) {
        NotebookCellLines.CellType.CODE -> {
          val pointer = NotebookIntervalPointerFactory.get(editor).create(interval)
          currentControllers.asSequence()
            .filterIsInstance<RMarkdownOutputInlayController>()
            .firstOrNull {
              it.intervalPointer.get() == pointer.get()
              && it.inlay.offset == offset
            }
          ?: RMarkdownOutputInlayController(editor, this, pointer, offset)
        }
        NotebookCellLines.CellType.MARKDOWN,
        NotebookCellLines.CellType.RAW -> null
      }
    }
  }
}
