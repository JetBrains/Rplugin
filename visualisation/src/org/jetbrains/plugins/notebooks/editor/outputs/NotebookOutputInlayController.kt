package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.castSafelyTo
import org.jetbrains.plugins.notebooks.editor.NotebookCellInlayController
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.notebookAppearance
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputComponentFactory.Companion.gutterPainter
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputComponentFactory.WidthStretching.*
import org.jetbrains.plugins.notebooks.editor.ui.addComponentInlay
import org.jetbrains.plugins.notebooks.editor.ui.registerEditorSizeWatcher
import org.jetbrains.plugins.notebooks.editor.ui.textEditingAreaWidth
import org.jetbrains.plugins.notebooks.editor.ui.yOffsetFromEditor
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_INLAY_HEIGHT = 200

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
    } else {
      DEFAULT_INLAY_HEIGHT
    }

    Disposer.register(inlay) {
      for (disposable in innerComponent.components) {
        if (disposable is Disposable) {
          Disposer.dispose(disposable)
        }
      }
    }
  }

  override fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle, intervalIterator: ListIterator<NotebookCellLines.Interval>) {
    val yOffset = innerComponent.yOffsetFromEditor(editor) ?: return
    val bounds = Rectangle()
    val oldClip = g.clipBounds
    g.clip = Rectangle(oldClip.x, yOffset, oldClip.width, innerComponent.height).intersection(oldClip)
    for (component in innerComponent.components) {
      if (component is JComponent) {
        component.gutterPainter?.let { painter ->
          component.yOffsetFromEditor(editor)?.let { yOffset ->
            bounds.setBounds(r.x, yOffset, r.width, component.height)
            painter.paintGutter(editor, g, bounds)
          }
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
    for (component in innerComponent.components) {
      if (component is JComponent) {
        val factory = component.outputComponentFactory
        if (factory != null) {
          it += component to factory
        }
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
              innerComponent.add(newComponent.component, newComponent.fixedWidthLayoutConstraint, idx)
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
      val old = innerComponent.getComponent(idx)
      innerComponent.remove(idx)
      if (old is Disposable) {
        Disposer.dispose(old)
      }
    }

    for (outputDataKey in newDataKeyIterator) {
      val newComponent = createOutputGuessingFactory(outputDataKey)
      if (newComponent != null) {
        isFilled = true
        innerComponent.add(newComponent.component, newComponent.fixedWidthLayoutConstraint)
      }
    }

    return isFilled
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

val EditorCustomElementRenderer.notebookInlayOutputComponent: JComponent?
  get() = castSafelyTo<JComponent>()?.components?.firstOrNull()?.castSafelyTo<SurroundingComponent>()

private val NotebookOutputComponentFactory.CreatedComponent.fixedWidthLayoutConstraint
  get() = FixedWidthMaxHeightLayout.Constraint(widthStretching, limitHeight)

private class SurroundingComponent private constructor(private val innerComponent: InnerComponent) : JPanel(
  BorderLayout()) {
  private var presetWidth = 0

  init {
    border = IdeBorderFactory.createEmptyBorder(Insets(10, 0, 10, 0))
    add(innerComponent, BorderLayout.CENTER)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  override fun getPreferredSize(): Dimension = super.getPreferredSize().also {
    it.width = presetWidth
    // No need to show anything for the empty component
    if (innerComponent.preferredSize.height == 0) {
      it.height = 0
    }
  }

  companion object {
    @JvmStatic
    fun create(
      editor: EditorImpl,
      innerComponent: InnerComponent,
    ) = SurroundingComponent(innerComponent).also {
      registerEditorSizeWatcher(it) {
        val oldWidth = it.presetWidth
        it.presetWidth = editor.textEditingAreaWidth
        if (oldWidth != it.presetWidth) {
          innerComponent.revalidate()
        }
      }

      for (wrapper in NotebookOutputComponentWrapper.EP_NAME.extensionList) {
        wrapper.wrap(it)
      }
    }
  }
}

private class InnerComponent : JPanel() {
  var maxHeight: Int = Int.MAX_VALUE

  init {
    layout = FixedWidthMaxHeightLayout(widthGetter = { width }, maxHeightGetter = { maxHeight })
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }
}

private fun <A, B> Iterator<A>.zip(other: Iterator<B>): Iterator<Pair<A, B>> = object : Iterator<Pair<A, B>> {
  override fun hasNext(): Boolean = this@zip.hasNext() && other.hasNext()
  override fun next(): Pair<A, B> = this@zip.next() to other.next()
}

private var JComponent.outputComponentFactory: NotebookOutputComponentFactory?
  get() =
    getClientProperty(NotebookOutputComponentFactory::class.java) as NotebookOutputComponentFactory?
  set(value) {
    putClientProperty(NotebookOutputComponentFactory::class.java, value)
  }

private class FixedWidthMaxHeightLayout(
  private val widthGetter: (Container) -> Int,
  private val maxHeightGetter: () -> Int,
) : LayoutManager2 {
  data class Constraint(val widthStretching: NotebookOutputComponentFactory.WidthStretching, val limitedHeight: Boolean)

  override fun addLayoutComponent(comp: Component, constraints: Any) {
    comp.constraints = constraints as Constraint
  }

  override fun addLayoutComponent(name: String?, comp: Component) {
    error("Should not be called")
  }

  override fun removeLayoutComponent(comp: Component) {
    comp.constraints = null
  }

  override fun preferredLayoutSize(parent: Container): Dimension =
    foldSize(parent) { preferredSize }

  override fun minimumLayoutSize(parent: Container): Dimension =
    foldSize(parent) { minimumSize }

  override fun maximumLayoutSize(target: Container): Dimension =
    foldSize(target) { maximumSize }

  override fun layoutContainer(parent: Container) {
    val parentInsets = parent.insets
    var totalY = parentInsets.top
    forEveryComponent(parent, Component::getPreferredSize) { component, newWidth, newHeight ->
      component.setBounds(
        parentInsets.left,
        totalY,
        newWidth,
        newHeight,
      )
      totalY += newHeight
    }
  }

  override fun getLayoutAlignmentX(target: Container): Float = 0f

  override fun getLayoutAlignmentY(target: Container): Float = 0f

  override fun invalidateLayout(target: Container): Unit = Unit

  private inline fun foldSize(parent: Container, crossinline handler: Component.() -> Dimension): Dimension {
    val acc = Dimension(0, parent.insets.run { top + bottom })

    forEveryComponent(parent, handler) { _, newWidth, newHeight ->
      acc.width = max(acc.width, newWidth)
      acc.height += newHeight
    }

    return acc
  }

  private inline fun forEveryComponent(
    parent: Container,
    crossinline sizeProposer: Component.() -> Dimension,
    crossinline handleComponent: (component: Component, newWidth: Int, newHeight: Int) -> Unit,
  ) {
    val parentInsets = parent.insets
    val parentDesiredWidth = widthGetter(parent)
    val maxHeight = maxHeightGetter()
    for (component in parent.components) {
      val proposedSize = component.sizeProposer()
      val newWidth = getComponentWidthByConstraint(parentDesiredWidth, parentInsets, component.constraints, proposedSize.width)
      val newHeight =
        if (component.constraints?.limitedHeight == true) min(maxHeight, proposedSize.height)
        else proposedSize.height
      handleComponent(component, newWidth, newHeight)
    }
  }

  private fun getComponentWidthByConstraint(parentWidth: Int,
                                            parentInsets: Insets,
                                            componentConstraints: Constraint?,
                                            componentDesiredWidth: Int): Int =
    (parentWidth - parentInsets.left - parentInsets.right).let {
      when (componentConstraints?.widthStretching) {
        STRETCH_AND_SQUEEZE -> it
        STRETCH -> max(it, componentDesiredWidth)
        SQUEEZE -> min(it, componentDesiredWidth)
        NOTHING -> componentDesiredWidth
        else -> error(componentConstraints.toString())
      }
    }

  private var Component.constraints: Constraint?
    get() = castSafelyTo<JComponent>()?.getClientProperty(Constraint::class.java) as Constraint?
    set(value) {
      castSafelyTo<JComponent>()?.putClientProperty(Constraint::class.java, value)
    }
}