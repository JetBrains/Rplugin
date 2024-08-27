package org.jetbrains.r.editor.ui

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLines
import org.jetbrains.plugins.notebooks.visualization.NotebookIntervalPointer
import org.jetbrains.plugins.notebooks.visualization.NotebookIntervalPointerFactory
import org.jetbrains.r.editor.ui.RMarkdownOutputInlayControllerUtil.getCodeFenceEnd
import org.jetbrains.r.visualization.inlays.RInlayDimensions
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import kotlin.math.max
import kotlin.math.min


class RMarkdownNotebook(project: Project, editor: EditorImpl) {
  // pointerFactory reuses pointers
  private val outputs: MutableMap<NotebookIntervalPointer, RMarkdownNotebookOutput> = LinkedHashMap()
  private val viewportQueue = MergingUpdateQueue(VIEWPORT_TASK_NAME, VIEWPORT_TIME_SPAN, true, null, editor.disposable)
  private val pointerFactory = NotebookIntervalPointerFactory.Companion.get(editor)
  private val psiToInterval = PsiToInterval(project, editor) { interval -> getCodeFenceEnd(editor, interval) }

  init {
    addResizeListener(editor)
    addViewportListener(editor)
  }

  operator fun get(cell: NotebookCellLines.Interval?): RMarkdownNotebookOutput? {
    if (cell == null) return null
    val intervalPointer = ReadAction.compute<NotebookIntervalPointer, Throwable> {
      pointerFactory.create(cell)
    }
    return outputs[intervalPointer]
  }

  operator fun get(cell: PsiElement): RMarkdownNotebookOutput? =
    correctCell(cell)?.let { psiToInterval[it] }?.let { this[it] }

  private fun correctCell(cell: PsiElement): PsiElement? =
    when (cell.elementType) {
      MarkdownTokenTypes.FENCE_LANG -> cell.parent.children.find { it.elementType == MarkdownTokenTypes.CODE_FENCE_END }
      else -> cell
    }

  fun update(output: RMarkdownNotebookOutput) {
    require(output.intervalPointer !in outputs)
    outputs[output.intervalPointer] = output
  }

  fun remove(output: RMarkdownNotebookOutput) {
    outputs.remove(output.intervalPointer, output)
  }

  private fun addResizeListener(editor: EditorEx) {
    editor.component.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        val inlayWidth = RInlayDimensions.calculateInlayWidth(editor)
        if (inlayWidth > 0) {
          outputs.values.forEach {
            it.setWidth(inlayWidth)
          }
        }
      }
    })
  }

  private fun addViewportListener(editor: EditorImpl) {
    editor.scrollPane.viewport.addChangeListener {
      viewportQueue.queue(object : Update(VIEWPORT_TASK_IDENTITY) {
        override fun run() =
          updateInlaysForViewport(editor)
      })
    }
  }

  private fun updateInlaysForViewport(editor: EditorImpl) {
    invokeLater {
      if (editor.isDisposed) return@invokeLater
      val viewportRange = calculateViewportRange(editor)
      val expansionRange = calculateInlayExpansionRange(editor, viewportRange)
      outputs.values.forEach {
        it.onUpdateViewport(viewportRange, expansionRange)
      }
    }
  }

  companion object {
    private const val VIEWPORT_TASK_NAME = "On viewport change"
    private const val VIEWPORT_TASK_IDENTITY = "On viewport change task"
    private const val VIEWPORT_TIME_SPAN = 50

    private fun install(editor: EditorImpl): RMarkdownNotebook =
      RMarkdownNotebook(editor.project!!, editor).also {
        key.set(editor, it)
      }

    fun installIfNotExists(editor: EditorImpl): RMarkdownNotebook =
      editor.rMarkdownNotebook ?: install(editor)
  }
}

private val key = Key.create<RMarkdownNotebook>(RMarkdownNotebook::class.java.name)

val Editor.rMarkdownNotebook: RMarkdownNotebook?
  get() = key.get(this)



private const val VIEWPORT_INLAY_RANGE = 20

private fun calculateViewportRange(editor: EditorImpl): IntRange {
  val viewport = editor.scrollPane.viewport
  val yMin = viewport.viewPosition.y
  val yMax = yMin + viewport.height
  return yMin until yMax
}

private fun calculateInlayExpansionRange(editor: EditorImpl, viewportRange: IntRange): IntRange {
  val startLine = editor.xyToLogicalPosition(Point(0, viewportRange.first)).line
  val endLine = editor.xyToLogicalPosition(Point(0, viewportRange.last + 1)).line
  val startOffset = editor.document.getLineStartOffset(max(startLine - VIEWPORT_INLAY_RANGE, 0))
  val endOffset = editor.document.getLineStartOffset(max(min(endLine + VIEWPORT_INLAY_RANGE, editor.document.lineCount - 1), 0))
  return startOffset..endOffset
}