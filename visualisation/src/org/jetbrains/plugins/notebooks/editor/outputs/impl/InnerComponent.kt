package org.jetbrains.plugins.notebooks.editor.outputs.impl

import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel

internal class InnerComponent : JPanel() {
  var maxHeight: Int = Int.MAX_VALUE

  init {
    layout = FixedWidthMaxHeightLayout(this)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = false
  }

  override fun add(comp: Component): Component = throw UnsupportedOperationException()

  override fun add(comp: Component, constraints: Any) {
    add(comp, constraints, -1)
  }

  override fun add(comp: Component, constraints: Any, index: Int) {
    require(comp is CollapsingComponent)
    // It'd rather the constraint was set in the layout manager, but it's broken. See DS-1566.
    comp.layoutConstraints = constraints as FixedWidthMaxHeightLayout.Constraint
    super.add(comp, constraints, index)
  }

  val mainComponents: List<JComponent>
    get() = ArrayList<JComponent>(componentCount).also {
      repeat(componentCount) { i ->
        it += (getComponent(i) as CollapsingComponent).mainComponent
      }
    }
}