package org.jetbrains.r.editor.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.plugins.notebooks.visualization.*
import org.jetbrains.plugins.notebooks.visualization.r.inlays.*
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.InlayProgressStatus
import org.jetbrains.r.rendering.chunk.ChunkDescriptorProvider
import org.jetbrains.r.rendering.chunk.ChunkPath
import org.jetbrains.r.rendering.chunk.RMarkdownInlayDescriptor
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ComponentEvent

class RMarkdownOutputInlayController private constructor(
  val editor: EditorImpl,
  override val factory: NotebookCellInlayController.Factory,
  override val intervalPointer: NotebookIntervalPointer
) : NotebookCellInlayController, RMarkdownNotebookOutput {

  private val notebook: RMarkdownNotebook = RMarkdownNotebook.installIfNotExists(editor)
  private val inlayComponent: NotebookInlayComponent = addInlayComponent(editor, intervalPointer)!!
  override val inlay: Inlay<*> = inlayComponent.inlay!!

  init {
    notebook.update(this)
    updateOutputs(resetComponent = false)
    Disposer.register(inlayComponent.inlay!!, Disposable { dispose() })
  }

  override fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle, intervalIterator: ListIterator<NotebookCellLines.Interval>) {
    val interval = intervalIterator.next()
    val inlayBounds = inlay.bounds ?: return
    paintNotebookCellBackgroundGutter(editor, g, r, interval, inlayBounds.y, inlayBounds.height)
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

      inlayComponent.addInlayOutputs(outputs) {
        clearOutputs(removeFiles = true)
      }
    }
  }

  override fun setWidth(width: Int) {
    inlayComponent.setSize(width, inlayComponent.height)
    inlayComponent.inlay?.update()
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

  private fun addInlayComponent(editor: EditorImpl, intervalPointer: NotebookIntervalPointer): NotebookInlayComponent? {
    InlayDimensions.init(editor)
    val interval = intervalPointer.get() ?: return null
    val offset = extractOffset(editor.document, interval)
    val inlayComponent = NotebookInlayComponentInterval(intervalPointer, editor)

    // On editor creation it has 0 width
    val gutterWidth = (editor.gutter as EditorGutterComponentEx).width
    var editorWideWidth = editor.component.width - inlayComponent.width - gutterWidth - InlayDimensions.rightBorder
    if (editorWideWidth <= 0) {
      editorWideWidth = InlayDimensions.width
    }

    inlayComponent.setBounds(0, editor.offsetToXY(offset).y + editor.lineHeight, editorWideWidth, InlayDimensions.smallHeight)
    editor.contentComponent.add(inlayComponent)
    val inlay = addBlockElement(editor, offset, inlayComponent)

    inlayComponent.assignInlay(inlay)
    setupInlayComponent(editor, inlayComponent)

    return inlayComponent
  }

  override fun onUpdateViewport(viewportRange: IntRange, expansionRange: IntRange) {
    if (Disposer.isDisposed(inlay))
      return

    val bounds = inlayComponent.bounds
    val isInViewport = bounds.y <= viewportRange.last && bounds.y + bounds.height >= viewportRange.first
    inlayComponent.onViewportChange(isInViewport)
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

      if (!ChunkDescriptorProvider.isNewMode(editor)) {
        return null
      }

      val interval: NotebookCellLines.Interval = intervalIterator.next()
      return when (interval.type) {
        NotebookCellLines.CellType.CODE -> {
          val pointer = NotebookIntervalPointerFactory.get(editor).create(interval)
          currentControllers.asSequence()
            .filterIsInstance<RMarkdownOutputInlayController>()
            .firstOrNull {
              it.intervalPointer.get() == pointer.get()
            }
          ?: RMarkdownOutputInlayController(editor, this, pointer)
        }
        NotebookCellLines.CellType.MARKDOWN,
        NotebookCellLines.CellType.RAW -> null
      }
    }
  }
}


private fun addBlockElement(editor: Editor, offset: Int, inlayComponent: NotebookInlayComponent): Inlay<NotebookInlayComponent> =
  editor.inlayModel.addBlockElement(offset, true, false, EditorInlaysManager.INLAY_PRIORITY, inlayComponent)!!


private fun setupInlayComponent(editor: Editor, inlayComponent: NotebookInlayComponent) {
  val scrollKeeper = EditorScrollingPositionKeeper(editor)

  fun updateInlaysInEditor(editor: Editor) {
    val end = editor.xyToLogicalPosition(Point(0, Int.MAX_VALUE))
    val offsetEnd = editor.logicalPositionToOffset(end)

    val inlays = editor.inlayModel.getBlockElementsInRange(0, offsetEnd)

    inlays.forEach { inlay ->
      if (inlay.renderer is InlayComponent) {
        (inlay.renderer as InlayComponent).updateComponentBounds(inlay)
      }
    }
  }
  inlayComponent.beforeHeightChanged = {
    scrollKeeper.savePosition()
  }
  inlayComponent.afterHeightChanged = {
    updateInlaysInEditor(editor)
    scrollKeeper.restorePosition(true)
  }
}

private fun getPsiElement(editor: Editor, offset: Int): PsiElement? =
  editor.psiFile?.viewProvider?.let { it.findElementAt(offset, it.baseLanguage) }

private fun getCodeFenceEnd(psiElement: PsiElement): PsiElement? =
  psiElement.let { it.parent.children.find { it.elementType == MarkdownTokenTypes.CODE_FENCE_END } }

private fun getCodeFenceEnd(editor: EditorImpl, interval: NotebookCellLines.Interval): PsiElement? {
  val offset = extractOffset(editor.document, interval)
  val psiElement = getPsiElement(editor, offset) ?: return null
  return getCodeFenceEnd(psiElement)
}

private fun disposeComponent(component: NotebookInlayComponent) {
  component.parent?.remove(component)
  component.disposeInlay()
  component.dispose()
}

private fun extractOffset(document: Document, interval: NotebookCellLines.Interval) =
  Integer.max(document.getLineEndOffset(interval.lines.last) - 1, 0)

private val key = Key.create<RMarkdownNotebook>(RMarkdownNotebook::class.java.name)


/**
 * calls to clearOutputs, addText and updateOutputs are runned in edt, order of calls is preserved
 * dispose() and updates of RMarkdownNotebook done at call time
 */
interface RMarkdownNotebookOutput {
  val intervalPointer: NotebookIntervalPointer

  /** clear outputs and text */
  fun clearOutputs(removeFiles: Boolean)

  /** add text as output */
  fun addText(text: String, outputType: Key<*>)

  /** do clearOutputs(), load outputs from filesystem */
  fun updateOutputs()

  fun updateProgressStatus(progressStatus: InlayProgressStatus)

  fun dispose()

  fun onUpdateViewport(viewportRange: IntRange, expansionRange: IntRange)

  fun setWidth(width: Int)
}

class RMarkdownNotebook(project: Project, editor: EditorImpl) {
  // pointerFactory reuses pointers
  private val outputs: MutableMap<NotebookIntervalPointer, RMarkdownNotebookOutput> = LinkedHashMap()
  private val viewportQueue = MergingUpdateQueue(VIEWPORT_TASK_NAME, VIEWPORT_TIME_SPAN, true, null, editor.disposable)
  private val pointerFactory = NotebookIntervalPointerFactory.get(editor)
  private val psiToInterval = PsiToInterval(project, editor) { interval -> getCodeFenceEnd(editor, interval) }

  init {
    addResizeListener(editor)
    addViewportListener(editor)
  }

  operator fun get(cell: NotebookCellLines.Interval?): RMarkdownNotebookOutput? =
    cell?.let { pointerFactory.create(it) }?.let { outputs[it] }

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
    editor.component.addComponentListener(object : java.awt.event.ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        val inlayWidth = InlayDimensions.calculateInlayWidth(editor)
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

val Editor.rMarkdownNotebook: RMarkdownNotebook?
  get() = key.get(this)
