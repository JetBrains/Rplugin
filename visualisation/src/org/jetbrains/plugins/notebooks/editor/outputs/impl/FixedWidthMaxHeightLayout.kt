package org.jetbrains.plugins.notebooks.editor.outputs.impl

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputComponentFactory
import java.awt.*
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min

internal class FixedWidthMaxHeightLayout(private val innerComponent: InnerComponent) : LayoutManager2 {
  data class Constraint(val widthStretching: NotebookOutputComponentFactory.WidthStretching, val limitedHeight: Boolean)

  override fun addLayoutComponent(comp: Component, constraints: Any) {
    // Can't rely on this method. See DS-1566.
  }

  override fun addLayoutComponent(name: String?, comp: Component) {
    error("Should not be called")
  }

  override fun removeLayoutComponent(comp: Component) {
    require(comp is JComponent)
    comp.layoutConstraints = null
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
    val parentDesiredWidth = innerComponent.width
    val maxHeight = innerComponent.maxHeight
    for (component in parent.components) {
      check(component is CollapsingComponent) { "$component is not CollapsingComponent" }
      val proposedSize = component.sizeProposer()
      val newWidth = getComponentWidthByConstraint(parentDesiredWidth, parentInsets, component, proposedSize.width)
      val newHeight =
        if (!component.isPreferredSizeSet && component.layoutConstraints?.limitedHeight == true) min(maxHeight, proposedSize.height)
        else proposedSize.height
      handleComponent(component, newWidth, newHeight)
    }
  }

  private fun getComponentWidthByConstraint(parentWidth: Int,
                                            parentInsets: Insets,
                                            component: JComponent,
                                            componentDesiredWidth: Int): Int =
    (parentWidth - parentInsets.left - parentInsets.right).let {
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

  companion object {
    // Used only in exceptional, very rare cases, therefore covered by "lazy".
    private val LOG by lazy { logger<FixedWidthMaxHeightLayout>() }
  }
}