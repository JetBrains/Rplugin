package org.jetbrains.plugins.notebooks.editor.outputs.impl

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.util.castSafelyTo
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import javax.swing.JComponent
import javax.swing.SwingUtilities

internal class OutputCollapsingGutterMouseListener(
  private val gutterComponentEx: EditorGutterComponentEx,
) : MouseListener, MouseMotionListener {
  companion object {
    @JvmStatic
    fun ensureInstalled(gutterComponentEx: EditorGutterComponentEx) {
      if (gutterComponentEx.getClientProperty(OutputCollapsingGutterMouseListener::class.java) == null) {
        gutterComponentEx.putClientProperty(OutputCollapsingGutterMouseListener::class.java, Unit)

        val instance = OutputCollapsingGutterMouseListener(gutterComponentEx)
        gutterComponentEx.addMouseListener(instance)
        gutterComponentEx.addMouseMotionListener(instance)
      }
    }

    @JvmStatic
    private val EditorGutterComponentEx.editor: Editor?
      get() = PlatformDataKeys.EDITOR.getData(DataManager.getInstance().getDataContext(this))
  }

  override fun mouseClicked(e: MouseEvent) {
    if (e.mouseButton != MouseButton.Left) return

    val point = e.point
    if (!isAtCollapseVerticalStripe(point)) return
    val component = gutterComponentEx.hoveredCollapsingComponentRect ?: return

    component.isSeen = !component.isSeen
    e.consume()
    SwingUtilities.invokeLater {  // Being invoked without postponing, it would access old states of layouts and get the same results.
      if (gutterComponentEx.editor?.isDisposed == false) {
        updateState(point)
      }
    }
  }

  override fun mousePressed(e: MouseEvent): Unit = Unit

  override fun mouseReleased(e: MouseEvent): Unit = Unit

  override fun mouseEntered(e: MouseEvent): Unit = Unit

  override fun mouseExited(e: MouseEvent) {
    updateState(null)
  }

  override fun mouseMoved(e: MouseEvent) {
    updateState(e.point)
  }

  private fun updateState(point: Point?) {
    if (point == null || !isAtCollapseVerticalStripe(point)) {
      IdeGlassPaneImpl.forgetPreProcessedCursor(gutterComponentEx)
      gutterComponentEx.cursor = @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") null  // Huh? It's a valid operation!
      updateHoveredComponent(null)
    }
    else {
      val collapsingComponent = getCollapsingComponent(point)
      updateHoveredComponent(collapsingComponent)
      if (collapsingComponent != null) {
        val cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        IdeGlassPaneImpl.savePreProcessedCursor(gutterComponentEx, cursor)
        gutterComponentEx.cursor = cursor
      }
      else {
        IdeGlassPaneImpl.forgetPreProcessedCursor(gutterComponentEx)
      }
    }
  }

  override fun mouseDragged(e: MouseEvent): Unit = Unit

  private fun isAtCollapseVerticalStripe(point: Point): Boolean =
    CollapsingComponent.collapseRectHorizontalLeft(gutterComponentEx.editor as EditorEx).let {
      point.x in it until it + CollapsingComponent.COLLAPSING_RECT_WIDTH
    }

  private fun getCollapsingComponent(point: Point): CollapsingComponent? {
    val editor = gutterComponentEx.editor ?: return null

    val surroundingComponent: SurroundingComponent =
      editor.contentComponent.getComponentAt(0, point.y).castSafelyTo<JComponent>()?.getComponent(0)?.castSafelyTo()
      ?: return null

    val innerComponent: InnerComponent =
      (surroundingComponent.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER).castSafelyTo()
      ?: return null

    val y = point.y - SwingUtilities.convertPoint(innerComponent, 0, 0, editor.contentComponent).y

    val collapsingComponent: CollapsingComponent =
      innerComponent.getComponentAt(0, y).castSafelyTo()
      ?: return null

    if (!collapsingComponent.isWorthCollapsing) return null
    return collapsingComponent
  }

  private fun updateHoveredComponent(collapsingComponent: CollapsingComponent?) {
    val old = gutterComponentEx.hoveredCollapsingComponentRect
    if (old !== collapsingComponent) {
      gutterComponentEx.hoveredCollapsingComponentRect = collapsingComponent
      gutterComponentEx.repaint()
    }
  }
}