package org.jetbrains.r.editor.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import org.intellij.datavis.r.inlays.*
import org.intellij.datavis.r.ui.UiCustomizer
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes
import org.jetbrains.plugins.notebooks.editor.NotebookCellInlayController
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.r.rendering.chunk.ChunkDescriptorProvider
import org.jetbrains.r.rendering.chunk.RMarkdownInlayDescriptor
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ComponentEvent

class RMarkdownOutputInlayController private constructor(
  val editor: EditorImpl,
  override val factory: NotebookCellInlayController.Factory,
  override val psiElement: PsiElement) : NotebookCellInlayController, RMarkdownNotebookOutput {

  private val notebook: RMarkdownNotebook = RMarkdownNotebook.installIfNotExists(editor)
  private var inlayComponent: NotebookInlayComponent = addInlayComponent(editor, psiElement)
  override var inlay: Inlay<*> = inlayComponent.inlay!!

  init {
    registerDisposable(inlayComponent)
    notebook.update(this)
    updateOutputs(resetComponent = false)
  }

  private var skipDisposeComponent = false

  private fun<T> skipDisposeComponent(action: () -> T): T {
    val prevValue = skipDisposeComponent
    skipDisposeComponent = true
    val result = action()
    skipDisposeComponent = prevValue
    return result
  }

  private fun registerDisposable(registeredInlayComponent: NotebookInlayComponent) {
    Disposer.register(registeredInlayComponent.inlay!!, Disposable {
      if (skipDisposeComponent)
        return@Disposable

      if (inlayComponent == registeredInlayComponent) {
        notebook.remove(this)
      }

      disposeComponent(registeredInlayComponent)
    })
  }

  override fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle, intervalIterator: ListIterator<NotebookCellLines.Interval>) = Unit

  override fun addText(text: String, outputType: Key<*>) {
    invokeLater {
      inlayComponent.addText(text, outputType)
    }
  }

  override fun clearOutputs() {
    invokeLater { // preserve order with addText() calls
      resetComponent(inlayComponent)
    }
  }

  override fun updateOutputs() {
    updateOutputs(resetComponent = true)
  }

  private fun updateOutputs(resetComponent: Boolean) {
    invokeLater {
      if (resetComponent) {
        resetComponent(inlayComponent)
      } else {
        // reuse inlayComponent, check that it is valid
        if (Disposer.isDisposed(inlay)) {
          return@invokeLater
        }
      }

      val outputs = RMarkdownInlayDescriptor.getInlayOutputs(psiElement)
      if (outputs.isEmpty()) return@invokeLater

      inlayComponent.addInlayOutputs(outputs) {
        RMarkdownInlayDescriptor.cleanup(psiElement)
        clearOutputs()
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

  private fun resetComponent(oldComponent: NotebookInlayComponent) {
    if (Disposer.isDisposed(editor.disposable))
      return
    inlayComponent = addInlayComponent(editor, psiElement)
    inlay = inlayComponent.inlay!!
    registerDisposable(inlayComponent)
    oldComponent.inlay?.let { Disposer.dispose(it) }
  }

  private fun addInlayComponent(editor: EditorImpl, cell: PsiElement): NotebookInlayComponent {
    val offset = extractOffset(cell)

    InlayDimensions.init(editor)
    val inlayComponent = UiCustomizer.instance.createNotebookInlayComponent(cell, editor)

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

  override fun onPsiDocumentCommitted() {
    if (Disposer.isDisposed(editor.disposable)) return

    val offset = extractOffset(psiElement)
    if (inlayComponent.inlay?.offset == offset) {
      return
    }

    skipDisposeComponent {
      inlayComponent.disposeInlay()
    }

    inlay = addBlockElement(editor, offset, inlayComponent)
    inlayComponent.assignInlay(inlay)
    registerDisposable(inlayComponent)
    inlayComponent.updateComponentBounds(inlayComponent.inlay!!)
  }

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
          getCodeFenceEnd(editor, offset(editor.document, interval.lines))?.let{ codeEndElement ->
            currentControllers.asSequence()
              .filterIsInstance<RMarkdownOutputInlayController>()
              .firstOrNull {
                it.psiElement == codeEndElement
              }
            ?: RMarkdownOutputInlayController(editor, this, codeEndElement)
          }
        }
        NotebookCellLines.CellType.MARKDOWN,
        NotebookCellLines.CellType.RAW -> null
      }
    }

    private fun getCodeFenceEnd(editor: EditorImpl, offset: Int): PsiElement? {
      val psiElement = getPsiElement(editor, offset) ?: return null
      return getCodeFenceEnd(psiElement)
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

private fun disposeComponent(component: NotebookInlayComponent) {
  component.parent?.remove(component)
  component.disposeInlay()
  component.dispose()
}

private fun extractOffset(cell: PsiElement) =
  cell.endOffset - 1

private val key = Key.create<RMarkdownNotebook>(RMarkdownNotebook::class.java.name)


/**
 * calls to clearOutputs, addText and updateOutputs are runned in edt, order of calls is preserved
 * dispose() and updates of RMarkdownNotebook done at call time
 */
interface RMarkdownNotebookOutput {
  /** clear outputs and text */
  fun clearOutputs()

  /** add text as output */
  fun addText(text: String, outputType: Key<*>)

  /** do clearOutputs(), load outputs from filesystem */
  fun updateOutputs()

  fun dispose()

  fun onPsiDocumentCommitted()

  fun setWidth(width: Int)

  val psiElement: PsiElement
}

class RMarkdownNotebook(editor: EditorImpl) {
  private val outputs: MutableMap<PsiElement, RMarkdownNotebookOutput> = LinkedHashMap()

  init {
    addResizeListener(editor)
    addDocumentListener(editor)
  }

  operator fun get(cell: PsiElement): RMarkdownNotebookOutput? = outputs[cell]

  fun update(output: RMarkdownNotebookOutput) {
    val previousOutput = outputs.put(output.psiElement, output) as RMarkdownOutputInlayController?
    previousOutput?.dispose()
  }

  fun remove(output: RMarkdownNotebookOutput) {
    outputs.remove(output.psiElement, output)
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

  private fun addDocumentListener(editor: EditorImpl) {
    val project = editor.project ?: return

    editor.document.addDocumentListener(object: DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        PsiDocumentManager.getInstance(project).performForCommittedDocument(editor.document) {
          outputs.values.forEach {
            it.onPsiDocumentCommitted()
          }
        }
      }
    }, editor.disposable)
  }

  companion object {
    private fun install(editor: EditorImpl): RMarkdownNotebook =
      RMarkdownNotebook(editor).also {
        key.set(editor, it)
      }

    fun installIfNotExists(editor: EditorImpl): RMarkdownNotebook =
      editor.rMarkdownNotebook ?: install(editor)
  }
}

val Editor.rMarkdownNotebook: RMarkdownNotebook?
  get() = key.get(this)
