package org.jetbrains.plugins.notebooks.editor.outputs.impl

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.editor.SwingClientProperty
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputComponentFactory
import java.awt.Component
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.min

internal class InnerComponent(private val editor: EditorImpl) : JPanel() {
  data class Constraint(val widthStretching: NotebookOutputComponentFactory.WidthStretching, val limitedHeight: Boolean)

  var maxHeight: Int = Int.MAX_VALUE

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  override fun add(comp: Component, constraints: Any, index: Int) {
    require(comp is CollapsingComponent)
    require(constraints is Constraint)
    comp.layoutConstraints = constraints
    super.add(comp, constraints, index)
  }

  override fun remove(index: Int) {
    (getComponent(index) as JComponent).layoutConstraints = null
    super.remove(index)
  }

  val mainComponents: List<JComponent>
    get() = ArrayList<JComponent>(componentCount).also {
      repeat(componentCount) { i ->
        it += (getComponent(i) as CollapsingComponent).mainComponent
      }
    }

  override fun getPreferredSize(): Dimension =
    foldSize { preferredSize }

  override fun getMinimumSize(): Dimension =
    foldSize { minimumSize }

  override fun getMaximumSize(): Dimension =
    foldSize { maximumSize }

  override fun doLayout() {
    val editorVisibleYTop = editor.scrollingModel.visibleArea.y
    val editorVisibleYBottom = editor.scrollingModel.visibleArea.run { y + height }
    val editorRelativeYTop = SwingUtilities.convertPoint(this, 0, 0, editor.contentComponent).y
    val oldInsets = insets
    val oldComponentHeights = components.map { it.height }

    var totalY = insets.top
    forEveryComponent(Component::getPreferredSize) { component, newWidth, newHeight ->
      component.setBounds(
        insets.left,
        totalY,
        newWidth,
        newHeight,
      )
      totalY += newHeight
    }

    val newComponentHeights = components.map { it.height }

    // When components adjust their sizes, the code below compensates vertical scroll position. The topmost visible line should keep its
    // visual position.
    val (diff, animate) = run {
      var animate = false
      var diff = insets.top - oldInsets.top
      var oldTop = editorRelativeYTop
      var newTop = editorRelativeYTop
      var oldBottom: Int
      var newBottom: Int

      for ((oldHeight, newHeight) in oldComponentHeights.zip(newComponentHeights)) {
        if (editorVisibleYTop < oldTop) break

        oldBottom = oldTop + oldHeight
        newBottom = newTop + newHeight

        if (
          newHeight < oldHeight &&
          oldTop <= editorVisibleYTop &&
          editorVisibleYBottom <= oldBottom &&
          editorRelativeYTop + diff <= newTop
        ) {
          // No code lines were visible. Centering the component.
          diff += oldTop - editorVisibleYTop - editor.scrollingModel.visibleArea.height / 2
          animate = true
          break
        }

        diff += newHeight - oldHeight

        oldTop = oldBottom
        newTop = newBottom
      }
      diff to animate
    }

    if (diff != 0) {
      scrollToSelectedCell(editor)
    }
  }

  private inline fun foldSize(crossinline handler: Component.() -> Dimension): Dimension {
    val acc = Dimension(0, insets.run { top + bottom })

    forEveryComponent(handler) { _, newWidth, newHeight ->
      acc.width = max(acc.width, newWidth)
      acc.height += newHeight
    }

    return acc
  }

  private inline fun forEveryComponent(
    crossinline sizeProposer: Component.() -> Dimension,
    crossinline handleComponent: (component: Component, newWidth: Int, newHeight: Int) -> Unit,
  ) {
    repeat(componentCount) { index ->
      val component = getComponent(index)
      check(component is CollapsingComponent) { "$component is not CollapsingComponent" }
      val proposedSize = component.sizeProposer()
      val newWidth = getComponentWidthByConstraint(component, proposedSize.width)
      val newHeight =
        if (!component.isPreferredSizeSet && component.layoutConstraints?.limitedHeight == true) min(maxHeight, proposedSize.height)
        else proposedSize.height
      handleComponent(component, newWidth, newHeight)
    }
  }

  private fun getComponentWidthByConstraint(component: JComponent, componentDesiredWidth: Int): Int =
    (width - insets.left - insets.right).let {
      when (component.layoutConstraints?.widthStretching) {
        NotebookOutputComponentFactory.WidthStretching.STRETCH_AND_SQUEEZE -> it
        NotebookOutputComponentFactory.WidthStretching.STRETCH -> max(it, componentDesiredWidth)
        NotebookOutputComponentFactory.WidthStretching.SQUEEZE -> min(it, componentDesiredWidth)
        NotebookOutputComponentFactory.WidthStretching.NOTHING -> componentDesiredWidth
        null -> {
          LOG.error("The component $component has no constraints")
          componentDesiredWidth
        }
      }
    }

  private var JComponent.layoutConstraints: Constraint? by SwingClientProperty()

  companion object {
    private val LOG = logger<InnerComponent>()
  }
}