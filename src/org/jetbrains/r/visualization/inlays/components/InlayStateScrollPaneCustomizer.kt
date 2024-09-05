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
      val scrollPane = queue.removeLast()
      if (scrollPane is JBScrollPane) {
        val parent = scrollPane.parent
        if (parent != null) {
          val view = scrollPane.viewport.view
          if (view != null) { // maybe something is broken, previously there weren't nulls
            val jupyterOutputScrollPane = NotebookOutputNonStickyScrollPane(view)

            parent.remove(scrollPane)
            parent.add(jupyterOutputScrollPane)

            // editorGutter component is in rowHeader
            jupyterOutputScrollPane.rowHeader = scrollPane.rowHeader
          }
        }
      }
      scrollPane.components.filterIsInstance<JComponent>().forEach { queue.add(it) }
    }
  }
}