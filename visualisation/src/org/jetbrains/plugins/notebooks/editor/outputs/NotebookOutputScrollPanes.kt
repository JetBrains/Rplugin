package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.ComponentUtil
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBScrollPane
import java.awt.Component
import java.awt.Insets
import java.awt.Point
import java.awt.event.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.JScrollPane

/** Default output scroll pane similar to one used in the IDEA editor features no border and corners
 * that respect content background. */
open class NotebookOutputDefaultScrollPane(private val view: Component) : JBScrollPane(view) {
  init {
    border = IdeBorderFactory.createEmptyBorder(Insets(0, 0, 0, 0))
    setScrollBars()
    setCorners()
  }

  private fun setScrollBars() {
    setScrollBar(verticalScrollBar)
    setScrollBar(horizontalScrollBar)
  }

  private fun setScrollBar(scrollBar: JScrollBar) {
    scrollBar.apply {
      isOpaque = true
      background = view.background
    }
  }

  private fun setCorners() {
    setCorner(LOWER_RIGHT_CORNER, Corner())
    setCorner(UPPER_RIGHT_CORNER, Corner())
  }

  private inner class Corner : JPanel() {
    init {
      background = view.background
    }
  }
}

/** A scroll pane that doesn't capture cursor immediately.
 *
 * The main differences of this scroll pane implementation from [JBScrollPane] are:
 *
 * 1. It always shows the scroll bars.
 * 2. It doesn't start scrolling immediately after getting a mouse wheel event thus does not interfere with the editor scrolling.
 * 3. Mouse click inside the scroll pane makes it to handle further mouse wheel events unconditionally.
 *
 * */
open class NotebookOutputNonStickyScrollPane(view: Component) : NotebookOutputDefaultScrollPane(view) {
  private var latestMouseWheelEventTime = 0L
  private var mouseEnteredTime = 0L

  /** If true, the scroll pane should handle the mouse wheel event unconditionally. */
  private var isScrollCaptured = false

  private val mouseAdapter = MyMouseAdapter()
  private val containerAdapter = MyContainerAdapter()

  init {
    recursivelyAddMouseListenerToComponent(this, mouseAdapter)
    recursivelyAddContainerListenerToComponent(this, containerAdapter)
  }

  override fun processMouseWheelEvent(e: MouseWheelEvent) {
    val eventTime = e.`when`
    val threshold = Registry.get("python.ds.jupyter.scrolling.innerScrollCooldownTime").asInteger().toLong()
    when {
      isScrollCaptured -> {
        super.processMouseWheelEvent(e)
      }
      eventTime - mouseEnteredTime < threshold || eventTime - latestMouseWheelEventTime < threshold -> {
        latestMouseWheelEventTime = eventTime
        delegateToParentScrollPane(e)
      }
      else -> {
        super.processMouseWheelEvent(e)
      }
    }
  }

  private fun delegateToParentScrollPane(e: MouseEvent) {
    val parentScrollPane: JScrollPane? = findParentOfType(JScrollPane::class.java)
    if (parentScrollPane != null) {
      e.source = parentScrollPane
      parentScrollPane.dispatchEvent(e)
    }
  }

  private fun <T> findParentOfType(type: Class<T>): T? {
    parent ?: return null
    return ComponentUtil.getParentOfType(type, parent)
  }

  private inner class MyMouseAdapter : MouseAdapter() {
    override fun mouseEntered(e: MouseEvent) {
      if (mouseEnteredTime == 0L) {
        mouseEnteredTime = e.`when`
        latestMouseWheelEventTime = 0
      }
      super.mouseEntered(e)
    }

    override fun mouseExited(e: MouseEvent) {
      if (!isShowing) {
        // In some cases, e.g. for concatenated outputs, a component may not be shown,
        // so it is necessary to find a visible upper level scroll pane component and delegate
        // event processing to it.
        delegateToParentScrollPane(e)
      }
      else {
        val eventPointOnScreen = Point(e.xOnScreen, e.yOnScreen)
        val xRange = locationOnScreen.x..locationOnScreen.x + width
        val yRange = locationOnScreen.y..locationOnScreen.y + height
        if (eventPointOnScreen.x !in xRange || eventPointOnScreen.y !in yRange) {
          mouseEnteredTime = 0
          latestMouseWheelEventTime = 0
          isScrollCaptured = false
        }
      }
      super.mouseExited(e)
    }

    override fun mouseClicked(e: MouseEvent) {
      isScrollCaptured = true
      super.mouseClicked(e)
    }
  }

  private inner class MyContainerAdapter : ContainerAdapter() {
    override fun componentAdded(e: ContainerEvent) {
      (e.source as? JComponent)?.let {
        recursivelyAddMouseListenerToComponent(it, mouseAdapter)
      }
    }

    override fun componentRemoved(e: ContainerEvent) {
      (e.source as? JComponent)?.let {
        recursivelyRemoveListeners(it)
      }
    }
  }
}

private fun recursivelyAddMouseListenerToComponent(comp: JComponent, listener: MouseListener) {
  comp.addMouseListener(listener)
  for (c in comp.components) {
    if (c is JComponent) {
      recursivelyAddMouseListenerToComponent(c, listener)
    }
  }
}

private fun recursivelyAddContainerListenerToComponent(comp: JComponent, listener: ContainerListener) {
  comp.addContainerListener(listener)
  for (c in comp.components) {
    if (c is JComponent) {
      recursivelyAddContainerListenerToComponent(c, listener)
    }
  }
}

private fun recursivelyRemoveListeners(comp: JComponent) {
  val queue = mutableListOf<JComponent>()
  queue.add(comp)
  while (queue.isNotEmpty()) {
    val c = queue.removeAt(queue.lastIndex)
    c.containerListeners.forEach { c.removeContainerListener(it) }
    c.mouseListeners.forEach { c.removeMouseListener(it) }
    c.components.filterIsInstance<JComponent>().forEach { queue.add(it) }
  }
}
