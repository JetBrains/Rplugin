package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.util.castSafelyTo
import org.jetbrains.plugins.notebooks.editor.NotebookCellInlayController
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.SwingClientProperty
import org.jetbrains.plugins.notebooks.editor.notebookAppearance
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputComponentFactory.Companion.gutterPainter
import org.jetbrains.plugins.notebooks.editor.outputs.impl.*
import org.jetbrains.plugins.notebooks.editor.ui.addComponentInlay
import org.jetbrains.plugins.notebooks.editor.ui.yOffsetFromEditor
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.Toolkit
import javax.swing.JComponent

private const val DEFAULT_INLAY_HEIGHT = 200

val EditorCustomElementRenderer.notebookInlayOutputComponent: JComponent?
  get() = castSafelyTo<JComponent>()?.components?.firstOrNull()?.castSafelyTo<SurroundingComponent>()

/**
 * Shows outputs for intervals using [NotebookOutputDataKeyExtractor] and [NotebookOutputComponentFactory].
 */
class NotebookOutputInlayController private constructor(
  override val factory: NotebookCellInlayController.Factory,
  private val editor: EditorImpl,
  lines: IntRange,
) : NotebookCellInlayController {
  private val innerComponent = InnerComponent()
  private val outerComponent = SurroundingComponent.create(editor, innerComponent)

  override val inlay: Inlay<*> =
    editor.addComponentInlay(
      outerComponent,
      isRelatedToPrecedingText = true,
      showAbove = false,
      priority = editor.notebookAppearance.NOTEBOOK_OUTPUT_INLAY_PRIORITY,
      offset = editor.document.getLineEndOffset(lines.last),
    )

  init {
    innerComponent.maxHeight = if (!ApplicationManager.getApplication().isUnitTestMode) {
      (Toolkit.getDefaultToolkit().screenSize.height * 0.3).toInt()
    }
    else {
      DEFAULT_INLAY_HEIGHT
    }

    Disposer.register(inlay) {
      for (disposable in innerComponent.mainComponents) {
        if (disposable is Disposable) {
          Disposer.dispose(disposable)
        }
      }
    }

    OutputCollapsingGutterMouseListener.ensureInstalled((editor as EditorEx).gutterComponentEx)
  }

  override fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle, intervalIterator: ListIterator<NotebookCellLines.Interval>) {
    val yOffset = innerComponent.yOffsetFromEditor(editor) ?: return
    val bounds = Rectangle()
    val oldClip = g.clipBounds
    g.clip = Rectangle(oldClip.x, yOffset, oldClip.width, innerComponent.height).intersection(oldClip)
    for (collapsingComponent in innerComponent.components) {
      val mainComponent = (collapsingComponent as CollapsingComponent).mainComponent

      collapsingComponent.paintGutter(editor, yOffset, g)

      mainComponent.gutterPainter?.let { painter ->
        mainComponent.yOffsetFromEditor(editor)?.let { yOffset ->
          bounds.setBounds(r.x, yOffset, r.width, mainComponent.height)
          painter.paintGutter(editor, g, bounds)
        }
      }
    }
    g.clip = oldClip
  }

  private fun rankCompatibility(outputDataKeys: List<NotebookOutputDataKey>): Int =
    getComponentsWithFactories().zip(outputDataKeys).sumBy { (pair, outputDataKey) ->
      val (component, factory) = pair
      when (factory.match(component, outputDataKey)) {
        NotebookOutputComponentFactory.Match.NONE -> 0
        NotebookOutputComponentFactory.Match.COMPATIBLE -> 1
        NotebookOutputComponentFactory.Match.SAME -> 1000
      }
    }

  private fun getComponentsWithFactories() = mutableListOf<Pair<JComponent, NotebookOutputComponentFactory>>().also {
    for (component in innerComponent.mainComponents) {
      val factory = component.outputComponentFactory
      if (factory != null) {
        it += component to factory
      }
    }
  }

  private fun updateData(newDataKeys: List<NotebookOutputDataKey>): Boolean {
    val newDataKeyIterator = newDataKeys.iterator()
    val oldComponentsWithFactories = getComponentsWithFactories().iterator()
    var isFilled = false
    for ((idx, pair1) in newDataKeyIterator.zip(oldComponentsWithFactories).withIndex()) {
      val (newDataKey, pair2) = pair1
      val (oldComponent, oldFactory: NotebookOutputComponentFactory) = pair2
      isFilled =
        when (oldFactory.match(oldComponent, newDataKey)) {
          NotebookOutputComponentFactory.Match.NONE -> {
            innerComponent.remove(idx)
            if (oldComponent is Disposable) {
              Disposer.dispose(oldComponent)
            }
            val newComponent = createOutputGuessingFactory(newDataKey)
            if (newComponent != null) {
              addIntoInnerComponent(newComponent, idx)
              true
            }
            else false
          }
          NotebookOutputComponentFactory.Match.COMPATIBLE -> {
            oldFactory.updateComponent(editor, oldComponent, newDataKey)
            true
          }
          NotebookOutputComponentFactory.Match.SAME -> true
        } || isFilled
    }

    for (ignored in oldComponentsWithFactories) {
      val idx = innerComponent.componentCount - 1
      val old = innerComponent.getComponent(idx).let { if (it is CollapsingComponent) it.mainComponent else it }
      innerComponent.remove(idx)
      if (old is Disposable) {
        Disposer.dispose(old)
      }
    }

    for (outputDataKey in newDataKeyIterator) {
      val newComponent = createOutputGuessingFactory(outputDataKey)
      if (newComponent != null) {
        isFilled = true
        addIntoInnerComponent(newComponent)
      }
    }

    return isFilled
  }

  private fun addIntoInnerComponent(newComponent: NotebookOutputComponentFactory.CreatedComponent, pos: Int = -1) {
    val collapsingComponent = CollapsingComponent(
      editor,
      newComponent.component,
      newComponent.limitHeight,
      newComponent.collapsedTextSupplier,
    )

    innerComponent.add(
      collapsingComponent,
      FixedWidthMaxHeightLayout.Constraint(newComponent.widthStretching, newComponent.limitHeight),
      pos,
    )
  }

  private fun createOutputGuessingFactory(outputDataKey: NotebookOutputDataKey): NotebookOutputComponentFactory.CreatedComponent? =
    NotebookOutputComponentFactory.EP_NAME.extensionList.asSequence()
      .mapNotNull { factory ->
        createOutput(factory, outputDataKey)
      }
      .firstOrNull()

  private fun createOutput(factory: NotebookOutputComponentFactory,
                           outputDataKey: NotebookOutputDataKey): NotebookOutputComponentFactory.CreatedComponent? =
    factory.createComponent(editor, outputDataKey, inlay)?.also {
      it.component.outputComponentFactory = factory
      it.component.gutterPainter = it.gutterPainter
    }

  class Factory : NotebookCellInlayController.Factory {
    override fun compute(
      editor: EditorImpl,
      currentControllers: Collection<NotebookCellInlayController>,
      intervalIterator: ListIterator<NotebookCellLines.Interval>,
    ): NotebookCellInlayController? {
      if (!NewOutputInlaysSwitch.useNewForAnything) return null

      val interval = intervalIterator.next()
      if (interval.type != NotebookCellLines.CellType.CODE) return null

      val outputDataKeys =
        NotebookOutputDataKeyExtractor.EP_NAME.extensionList.asSequence()
          .mapNotNull { it.extract(editor, interval) }
          .firstOrNull()
          ?.takeIf { it.isNotEmpty() }
        ?: return null

      val controller =
        currentControllers
          .filterIsInstance<NotebookOutputInlayController>()
          .maxByOrNull { it.rankCompatibility(outputDataKeys) }
        ?: NotebookOutputInlayController(this, editor, interval.lines)
      return controller.takeIf { it.updateData(outputDataKeys) }
    }
  }
}

private fun <A, B> Iterator<A>.zip(other: Iterator<B>): Iterator<Pair<A, B>> = object : Iterator<Pair<A, B>> {
  override fun hasNext(): Boolean = this@zip.hasNext() && other.hasNext()
  override fun next(): Pair<A, B> = this@zip.next() to other.next()
}

private var JComponent.outputComponentFactory: NotebookOutputComponentFactory? by SwingClientProperty()