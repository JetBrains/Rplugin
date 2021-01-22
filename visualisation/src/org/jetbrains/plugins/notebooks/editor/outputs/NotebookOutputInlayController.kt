package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.castSafelyTo
import com.intellij.util.ui.UIUtil
import org.intellij.datavis.r.inlays.MouseWheelUtils
import org.intellij.datavis.r.inlays.ResizeController
import org.jetbrains.plugins.notebooks.editor.NotebookCellInlayController
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.notebookAppearance
import org.jetbrains.plugins.notebooks.editor.ui.addComponentInlay
import org.jetbrains.plugins.notebooks.editor.ui.registerEditorSizeWatcher
import org.jetbrains.plugins.notebooks.editor.ui.textEditingAreaWidth
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
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
  private val outerComponent = SurroundingComponent.create(editor, innerComponentScrollPane)

  override val inlay: Inlay<*> =
    editor.addComponentInlay(
      outerComponent,
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
            for (wrapper in NotebookOutputComponentWrapper.EP_NAME.extensionList) {
              wrapper.newOutputComponentCreated(outerComponent, editor, outputDataKey)
            }
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
        for (wrapper in NotebookOutputComponentWrapper.EP_NAME.extensionList) {
          wrapper.newOutputComponentCreated(outerComponent, editor, outputDataKey)
        }
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

val EditorCustomElementRenderer.notebookInlayOutputComponent: JComponent?
  get() = castSafelyTo<JComponent>()?.components?.firstOrNull()?.castSafelyTo<SurroundingComponent>()

private val NotebookOutputComponentFactory.WidthStretching.fixedWidthLayoutConstraint
  get() = when (this) {
    NotebookOutputComponentFactory.WidthStretching.STRETCH_AND_SQUEEZE -> FixedWidthLayout.STRETCH_AND_SQUEEZE
    NotebookOutputComponentFactory.WidthStretching.STRETCH -> FixedWidthLayout.STRETCH
    NotebookOutputComponentFactory.WidthStretching.SQUEEZE -> FixedWidthLayout.SQUEEZE
    NotebookOutputComponentFactory.WidthStretching.NOTHING -> FixedWidthLayout.NOTHING
  }

private class SurroundingComponent private constructor(innerComponentScrollPane: InnerComponentScrollPane) : JPanel(BorderLayout()) {
  private var presetWidth = 0

  init {
    border = IdeBorderFactory.createEmptyBorder(Insets(10, 0, 10, 0))
    add(innerComponentScrollPane, BorderLayout.CENTER)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  override fun getPreferredSize(): Dimension = super.getPreferredSize().also {
    it.width = presetWidth
  }

  override fun validateTree() {
    size = preferredSize
    super.validateTree()
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
    horizontalScrollBar.isOpaque = false
    verticalScrollBar.isOpaque = false

    UIUtil.getEditorPaneBackground().let { background ->
      verticalScrollBar.background = background
      horizontalScrollBar.background = background
    }
  }

  override fun isValidateRoot(): Boolean = false

  override fun getPreferredSize(): Dimension =
    super.getPreferredSize().also {
      if (
        it.width > width
        // JBScrollPane contradicts with JScrollPane: it adds a vertical padding if the horizontal scrollbar not at the zero position.
        // Had there been JScrollPane, the condition below wouldn't have been needed.
        && horizontalScrollBar.value == 0
      ) {
        it.height += horizontalScrollBar.height
      }
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

// Can't inherit Scrollable: if Scrollable.getScrollableTracksViewportWidth returns true, the scrollbar is never shown, even if
// the content overflows. Without that, Scrollable is not needed.
private class InnerComponent : JPanel() {
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
    require(
      name == STRETCH_AND_SQUEEZE ||
      name == STRETCH ||
      name == SQUEEZE ||
      name == NOTHING
    ) {
      name.toString()
    }
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
    val parentDesiredWidth = widthGetter(parent)
    var totalY = parentInsets.top
    for (component in parent.components) {
      val componentPreferedSize = component.preferredSize
      val newWidth = getComponentWidthByConstraint(parentDesiredWidth, parentInsets, component.constraints, componentPreferedSize.width)
      component.setBounds(
        parentInsets.left,
        totalY,
        newWidth,
        componentPreferedSize.height,
      )
      totalY += componentPreferedSize.height
    }
  }

  private inline fun foldSize(parent: Container, crossinline handler: Component.() -> Dimension): Dimension {
    var acc = Dimension(0, parent.insets.run { top + bottom })

    for (component in parent.components) {
      val componentDimensions = component.handler()
      val parentPreferedWidth = widthGetter(parent)
      val componentWidth = getComponentWidthByConstraint(parentPreferedWidth, parent.insets, component.constraints,
                                                         componentDimensions.width)
      acc = Dimension(componentWidth, acc.height + componentDimensions.height)
    }
    return acc
  }

  private fun getComponentWidthByConstraint(parentWidth: Int,
                                            parentInsets: Insets,
                                            componentConstraints: String?,
                                            componentDesiredWidth: Int): Int =
    (parentWidth - parentInsets.left - parentInsets.right).let {
      when (componentConstraints) {
        STRETCH_AND_SQUEEZE -> it
        STRETCH -> max(it, componentDesiredWidth)
        SQUEEZE -> min(it, componentDesiredWidth)
        NOTHING -> componentDesiredWidth
        else -> error(componentConstraints.toString())
      }
    }

  companion object Constraints {
    /** See [NotebookOutputComponentFactory.WidthStretching.STRETCH_AND_SQUEEZE] */
    const val STRETCH_AND_SQUEEZE = "STRETCH_AND_SQUEEZE"

    /** See [NotebookOutputComponentFactory.WidthStretching.STRETCH] */
    const val STRETCH = "STRETCH"

    /** See [NotebookOutputComponentFactory.WidthStretching.SQUEEZE] */
    const val SQUEEZE = "SQUEEZE"

    /** See [NotebookOutputComponentFactory.WidthStretching.NOTHING] */
    const val NOTHING = "NOTHING"

    private var Component.constraints: String?
      get() = castSafelyTo<JComponent>()?.getClientProperty(Constraints) as String?
      set(value) {
        castSafelyTo<JComponent>()?.putClientProperty(Constraints, value)
      }
  }
}
