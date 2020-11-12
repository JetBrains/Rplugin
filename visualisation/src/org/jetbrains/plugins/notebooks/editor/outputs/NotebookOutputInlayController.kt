package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.notebooks.editor.NotebookCellInlayController
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.notebookAppearance
import org.jetbrains.plugins.notebooks.editor.ui.addComponentInlay
import org.jetbrains.plugins.notebooks.editor.ui.registerEditorSizeWatcher
import org.jetbrains.plugins.notebooks.editor.ui.textEditingAreaWidth
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.plaf.ComponentUI

/**
 * Shows outputs for intervals using [NotebookOutputDataKeyExtractor] and [NotebookOutputComponentFactory].
 */
class NotebookOutputInlayController private constructor(
  override val factory: NotebookCellInlayController.Factory,
  private val editor: EditorImpl,
  lines: IntRange,
) : NotebookCellInlayController {
  private val innerComponent = InnerComponent(editor)
  private val outerComponent = OuterComponent.create(editor, innerComponent)

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
          val newComponent = factory.createComponent(editor, outputDataKey)
          if (newComponent != null) {
            innerComponent.add(newComponent, idx)
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
      val newComponent =
        NotebookOutputComponentFactory.EP_NAME.extensionList.asSequence()
          .mapNotNull { factory ->
            factory.createComponent(editor, outputDataKey)?.also { component ->
              component.outputComponentFactory = factory
            }
          }
          .firstOrNull()
      if (newComponent != null) {
        isFilled = true
        innerComponent.add(newComponent)
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

private class OuterComponent private constructor(
  private val editor: EditorImpl,
  innerComponent: InnerComponent,
) : JBScrollPane(innerComponent) {
  init {
    border = JBUI.Borders.empty(0)
  }

  override fun getPreferredSize(): Dimension =
    Dimension(
      editor.textEditingAreaWidth,
      super.getPreferredSize().height.coerceAtMost(editor.scrollingModel.visibleArea.height * 2 / 3),
    )

  companion object {
    @JvmStatic
    fun create(editor: EditorImpl, innerComponent: InnerComponent) = OuterComponent(editor, innerComponent).also { outerComponent ->
      registerEditorSizeWatcher(outerComponent) { editorComponent ->
        val oldWidth = innerComponent.fixedWidthLayout.fixedWidth
        innerComponent.fixedWidthLayout.fixedWidth = editorComponent?.editor?.textEditingAreaWidth ?: -1
        if (oldWidth != innerComponent.fixedWidthLayout.fixedWidth) {
          innerComponent.invalidate()
        }
      }
    }
  }
}

private class InnerComponent(private val editor: EditorImpl) : JPanel() {
  val fixedWidthLayout = FixedWidthLayout()

  init {
    layout = fixedWidthLayout
  }

  override fun setUI(newUI: ComponentUI) {
    super.setUI(newUI)
    background = editor.contentComponent.background
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

private class FixedWidthLayout(var fixedWidth: Int = -1) : LayoutManager {
  override fun addLayoutComponent(name: String, comp: Component): Unit = Unit

  override fun removeLayoutComponent(comp: Component): Unit = Unit

  override fun preferredLayoutSize(parent: Container): Dimension =
    foldSize(parent) { preferredSize }

  override fun minimumLayoutSize(parent: Container): Dimension =
    foldSize(parent) { minimumSize }

  override fun layoutContainer(parent: Container) {
    val parentInsets = parent.insets
    var totalY = parentInsets.top
    for (component in parent.components) {
      val preferredSize = component.preferredSize
      component.setBounds(
        parentInsets.left,
        totalY,
        (if (fixedWidth >= 0) fixedWidth else preferredSize.width) - parentInsets.left - parentInsets.right,
        preferredSize.height,
      )
      totalY += preferredSize.height
    }
  }

  private inline fun foldSize(parent: Container, crossinline handler: Component.() -> Dimension): Dimension =
    parent.components.fold(Dimension(0, parent.insets.run { top + bottom })) { acc, component ->
      component.handler().let {
        Dimension(if (fixedWidth >= 0) fixedWidth else it.width, acc.height + it.height)
      }
    }
}