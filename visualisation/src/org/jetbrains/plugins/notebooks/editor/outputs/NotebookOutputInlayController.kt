package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.castSafelyTo
import org.intellij.datavis.r.inlays.MouseWheelUtils
import org.intellij.datavis.r.inlays.ResizeController
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.notebooks.editor.NotebookCellInlayController
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.notebookAppearance
import org.jetbrains.plugins.notebooks.editor.ui.addComponentInlay
import org.jetbrains.plugins.notebooks.editor.ui.registerEditorSizeWatcher
import org.jetbrains.plugins.notebooks.editor.ui.textEditingAreaWidth
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.JViewport
import kotlin.math.max
import kotlin.math.min

/**
 * Shows outputs for intervals using [NotebookOutputDataKeyExtractor] and [NotebookOutputComponentFactory].
 */
class NotebookOutputInlayController private constructor(
  override val factory: NotebookCellInlayController.Factory,
  private val editor: EditorImpl,
  lines: IntRange,
) : NotebookCellInlayController {
  private val innerComponent = InnerComponent()
  private val innerComponentScrollPane = InnerComponentScrollPane(innerComponent)

  override val inlay: Inlay<*> =
    editor.addComponentInlay(
      SurroundingComponent.create(editor, innerComponentScrollPane),
      isRelatedToPrecedingText = true,
      showAbove = false,
      priority = editor.notebookAppearance.NOTEBOOK_OUTPUT_INLAY_PRIORITY,
      offset = editor.document.getLineEndOffset(lines.last),
    )

  init {
    Disposer.register(inlay) {
      for (disposable in innerComponent.components) {
        if (disposable is Disposable) {
          Disposer.dispose(disposable)
        }
      }
    }

    registerEditorSizeWatcher(innerComponentScrollPane) {
      innerComponentScrollPane.maxHeight = (editor.scrollingModel.visibleArea.height * 0.66).toInt()
      innerComponentScrollPane.invalidate()
    }
  }

  override fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle, intervalIterator: ListIterator<NotebookCellLines.Interval>) {}

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

  private fun updateData(outputDataKeys: List<NotebookOutputDataKey>): Boolean {
    val outputDataKeyIterator = outputDataKeys.iterator()
    val componentsWithFactories = getComponentsWithFactories().iterator()
    var isFilled = false
    for ((idx, pair1) in outputDataKeyIterator.zip(componentsWithFactories).withIndex()) {
      val (outputDataKey, pair2) = pair1
      val (component, factory) = pair2
      isFilled = isFilled || when (factory.match(component, outputDataKey)) {
        NotebookOutputComponentFactory.Match.NONE -> {
          innerComponent.remove(idx)
          val newComponentPair = factory.createComponent(editor, outputDataKey, inlay)
          if (newComponentPair != null) {
            innerComponent.add(newComponentPair.first, newComponentPair.second.fixedWidthLayoutConstraint, idx)
            true
          }
          else false
        }
        NotebookOutputComponentFactory.Match.COMPATIBLE -> {
          factory.updateComponent(editor, component, outputDataKey)
          true
        }
        NotebookOutputComponentFactory.Match.SAME -> true
      }
    }

    for (ignored in componentsWithFactories) {
      innerComponent.remove(innerComponent.componentCount - 1)
    }

    for (outputDataKey in outputDataKeyIterator) {
      val newComponentPair =
        NotebookOutputComponentFactory.EP_NAME.extensionList.asSequence()
          .mapNotNull { factory ->
            factory.createComponent(editor, outputDataKey, inlay)?.also { (component, _) ->
              component.outputComponentFactory = factory
            }
          }
          .firstOrNull()
      if (newComponentPair != null) {
        isFilled = true
        innerComponent.add(newComponentPair.first, newComponentPair.second.fixedWidthLayoutConstraint)
      }
    }

    return isFilled
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

/**
 * A dirty hack which is aware of [com.intellij.openapi.editor.impl.EditorEmbeddedComponentManager] bugs and features.
 *
 * `EditorEmbeddedComponentManager` doesn't react on invalidate. This function forcibly resized a whole cell output component
 * to its preferred size.
 *
 * See also DS-573.
 */
fun updateOutputInlayComponentSize(grandchild: Component): Boolean {
  val component = generateSequence(grandchild) { it.parent }.find { it is SurroundingComponent } ?: return false
  component.size = component.preferredSize
  return true
}

val EditorCustomElementRenderer.notebookInlayOutputComponent: JComponent?
  get() = castSafelyTo<JComponent>()?.components?.firstOrNull()?.castSafelyTo<SurroundingComponent>()

private val NotebookOutputComponentFactory.WidthStretching.fixedWidthLayoutConstraint
  get() = when (this) {
    NotebookOutputComponentFactory.WidthStretching.UNLIMITED -> FixedWidthLayout.UNLIMITED_WIDTH
    NotebookOutputComponentFactory.WidthStretching.LIMITED -> FixedWidthLayout.LIMITED_WIDTH
  }

private class SurroundingComponent private constructor(innerComponentScrollPane: InnerComponentScrollPane) : JPanel(BorderLayout()) {
  private var presetWidth = 0

  init {
    border = IdeBorderFactory.createEmptyBorder(Insets(0, 0, 10, 0))
    add(innerComponentScrollPane, BorderLayout.CENTER)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  override fun getPreferredSize(): Dimension = super.getPreferredSize().also {
    it.width = presetWidth
  }

  companion object {
    @JvmStatic
    fun create(
      editor: EditorImpl,
      innerComponentScrollPane: InnerComponentScrollPane,
    ) = SurroundingComponent(innerComponentScrollPane).also {
      val resizeController = ResizeController(it, editor)
      it.addMouseListener(resizeController)
      it.addMouseWheelListener(resizeController)
      it.addMouseMotionListener(resizeController)

      MouseWheelUtils.wrapMouseWheelListeners(it, editor.disposable)

      registerEditorSizeWatcher(it) {
        it.presetWidth = editor.textEditingAreaWidth
        it.invalidate()
      }

      for (wrapper in NotebookOutputComponentWrapper.EP_NAME.extensionList) {
        wrapper.wrap(it)
      }
    }
  }
}

private class InnerComponentScrollPane(innerComponent: InnerComponent) : NotebookOutputNonStickyScrollPane(innerComponent) {
  var maxHeight = 0

  init {
    viewport.addComponentListener(ViewportComponentListener)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    viewport.isOpaque = false
    verticalScrollBar.isOpaque = false
    horizontalScrollBar.isOpaque = false
  }

  override fun setScrollBar(scrollBar: JScrollBar): Unit = Unit

  override fun isValidateRoot(): Boolean = false

  override fun getPreferredSize(): Dimension =
    super.getPreferredSize().also {
      it.height = min(maxHeight, it.height)
    }

  private companion object {
    private object ViewportComponentListener : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        val target = (e.source as JViewport).parent as InnerComponentScrollPane
        val innerComponent = target.viewport.view as InnerComponent
        val firstTime = innerComponent.minWidth == 0

        // Tricky JBScrollPane calculates preferred sizes like there's no scrollbar, and applies that size even if there's a scrollbar.
        // Outsmarting it.
        innerComponent.minWidth = target.viewport.width - target.verticalScrollBar.width
        innerComponent.invalidate()

        if (firstTime) {
          // Without this call, the component will be sized incorrectly until any AWT event.
          innerComponent.validate()
        }
      }
    }
  }
}

@TestOnly
interface NotebookOutputComponentMarker

// Can't inherit Scrollable: if Scrollable.getScrollableTracksViewportWidth returns true, the scrollbar is never shown, even if
// the content overflows. Without that, Scrollable is not needed.
private class InnerComponent : JPanel(), NotebookOutputComponentMarker {
  var minWidth = 0

  init {
    layout = FixedWidthLayout { minWidth }
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

private class FixedWidthLayout(private val widthGetter: (Container) -> Int) : LayoutManager {
  override fun addLayoutComponent(name: String?, comp: Component) {
    require(name == LIMITED_WIDTH || name == UNLIMITED_WIDTH) { name.toString() }
    comp.constraints = name
  }

  override fun removeLayoutComponent(comp: Component) {
    comp.constraints = null
  }

  override fun preferredLayoutSize(parent: Container): Dimension =
    foldSize(parent) { preferredSize }

  override fun minimumLayoutSize(parent: Container): Dimension =
    foldSize(parent) { minimumSize }

  override fun layoutContainer(parent: Container) {
    val parentInsets = parent.insets
    val desiredWidth = widthGetter(parent)
    var totalY = parentInsets.top
    for (component in parent.components) {
      val preferredSize = component.preferredSize
      val newWidth = (desiredWidth - parentInsets.left - parentInsets.right).let {
        when (val c = component.constraints) {
          LIMITED_WIDTH -> it
          UNLIMITED_WIDTH -> max(it, preferredSize.width)
          else -> error(c.toString())
        }
      }
      component.setBounds(
        parentInsets.left,
        totalY,
        newWidth,
        preferredSize.height,
      )
      totalY += preferredSize.height
    }
  }

  private inline fun foldSize(parent: Container, crossinline handler: Component.() -> Dimension): Dimension {
    val desiredWidth = widthGetter(parent)
    var acc = Dimension(0, parent.insets.run { top + bottom })
    for (component in parent.components) {
      val dimension = component.handler()
      acc = Dimension(max(desiredWidth, dimension.width), acc.height + dimension.height)
    }
    return acc
  }

  companion object Constraints {
    /** See [NotebookOutputComponentFactory.WidthStretching.LIMITED] */
    const val LIMITED_WIDTH = "LIMITED_WIDTH"

    /** See [NotebookOutputComponentFactory.WidthStretching.UNLIMITED] */
    const val UNLIMITED_WIDTH = "UNLIMITED_WIDTH"

    private var Component.constraints: String?
      get() = castSafelyTo<JComponent>()?.getClientProperty(Constraints) as String?
      set(value) {
        castSafelyTo<JComponent>()?.putClientProperty(Constraints, value)
      }
  }
}
