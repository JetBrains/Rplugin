package org.jetbrains.r.visualization.inlays.components

import com.intellij.ui.components.JBScrollPane
import com.intellij.notebooks.visualization.outputs.NotebookOutputNonStickyScrollPane
import javax.swing.JComponent

object InlayStateScrollPaneCustomizer : InlayStateCustomizer {
  override fun customize(state: NotebookInlayState): NotebookInlayState {
    replaceAllScrollPanesWithJupyterOutputScrollPanes(state)
    state.revalidate()
    state.repaint()
    return state
  }

  private fun replaceAllScrollPanesWithJupyterOutputScrollPanes(root: JComponent) {
    val queue = mutableListOf<JComponent>()
    queue.add(root)
    while (queue.isNotEmpty()) {
      val c = queue.removeAt(queue.lastIndex)
      if (c is JBScrollPane) {
        val view = c.viewport.view
        if (view != null) { // maybe something is broken, previously there weren't nulls
          val jupyterOutputScrollPane = NotebookOutputNonStickyScrollPane(view)
          val parent = c.parent
          if (parent != null) {
            parent.remove(c)
            parent.add(jupyterOutputScrollPane)
          }
        }
      }
      c.components.filterIsInstance<JComponent>().forEach { queue.add(it) }
    }
  }
}