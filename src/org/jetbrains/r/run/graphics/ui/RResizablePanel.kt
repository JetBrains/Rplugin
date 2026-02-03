/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.event.MouseInputAdapter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class RResizablePanel(
  private val child: JComponent,
  initialSize: Dimension?,
  private val onSizeRecalculated: (Int, Int, Int, Int) -> Unit
) : JLayeredPane() {

  private val listener = ResizeMouseListener()

  private var visualWidth = 0
  private var visualHeight = 0
  private var targetWidth = initialSize?.width ?: 0
  private var targetHeight = initialSize?.height ?: 0

  val manipulator: JComponent = ResizeManipulator()

  @Volatile
  var aspectRatio: Double? = null

  init {
    minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
    addMouseListener(listener)
    addMouseMotionListener(listener)
    add(child, PALETTE_LAYER as Any)
    add(manipulator, DEFAULT_LAYER as Any)
    updateChildrenBounds(width, height)
  }

  override fun setEnabled(enabled: Boolean) {
    super.setEnabled(enabled)
    manipulator.isVisible = enabled
    targetHeight = height
    targetWidth = width
  }

  override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
    val previous = bounds
    super.setBounds(x, y, width, height)
    if (width != previous.width) {
      targetWidth = width
    }
    if (height != previous.height) {
      targetHeight = height
    }
    updateChildrenBounds(min(width, targetWidth), min(height, targetHeight))
  }

  private fun updateChildrenBounds(w: Int, h: Int) {
    updateVisualBounds(w, h)
    val xOffset = (width - visualWidth) / 2
    val yOffset = (height - visualHeight) / 2
    child.bounds = Rectangle(xOffset, yOffset, visualWidth, visualHeight)
    manipulator.bounds = Rectangle(xOffset + visualWidth - SIDE, yOffset + visualHeight - SIDE, SIDE, SIDE)
  }

  private fun updateVisualBounds(w: Int, h: Int) {
    visualWidth = w
    visualHeight = h
    aspectRatio?.let { ratio ->
      val adjustedVisualWidth = round(visualHeight * ratio).toInt()
      if (adjustedVisualWidth <= w) {
        visualWidth = adjustedVisualWidth
      } else {
        visualHeight = min(h, round(visualWidth / ratio).toInt())
      }
    }
  }

  private fun deltaSize(dx: Int, dy: Int) {
    val ratio = aspectRatio
    if (ratio != null) {
      val adjustedDx = (dx + dy) / 2
      val adjustedDy = round(adjustedDx / ratio).toInt()
      applyDeltaSize(adjustedDx, adjustedDy)
    } else {
      applyDeltaSize(dx, dy)
    }
  }

  private fun applyDeltaSize(dx: Int, dy: Int) {
    targetWidth = max(targetWidth + dx, MIN_WIDTH)
    targetHeight = max(targetHeight + dy, MIN_HEIGHT)
    onSizeRecalculated(targetWidth, targetHeight, dx, dy)
  }

  private fun isAboveManipulator(point: Point): Boolean {
    val xOffset = (width - visualWidth) / 2
    val yOffset = (height - visualHeight) / 2
    val xMax = xOffset + visualWidth
    val yMax = yOffset + visualHeight
    if (point.y > yMax) {
      return false
    }
    if (point.x > xMax) {
      return false
    }
    if (point.y + point.x < xMax + yMax - SIDE) {
      return false
    }
    return true
  }

  private inner class ResizeMouseListener : MouseInputAdapter() {
    private val resizeCursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)
    private val defaultCursor = Cursor.getDefaultCursor()
    private var previousPoint: Point? = null

    override fun mouseReleased(e: MouseEvent) {
      cursor = getCursor(e.point)
      previousPoint = null
    }

    override fun mouseMoved(e: MouseEvent) {
      cursor = getCursor(e.point)
    }

    override fun mouseDragged(e: MouseEvent) {
      previousPoint?.let { previous ->
        val current = e.locationOnScreen
        previousPoint = current
        deltaSize(current.x - previous.x, current.y - previous.y)
      }
    }

    override fun mouseExited(e: MouseEvent) {
      if (previousPoint == null) {
        cursor = defaultCursor
      }
    }

    override fun mousePressed(e: MouseEvent) {
      if (isAboveManipulator(e.point)) {
        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
        previousPoint = e.locationOnScreen
      }
    }

    private fun getCursor(point: Point): Cursor {
      return if (isAboveManipulator(point)) resizeCursor else defaultCursor
    }
  }

  private class ResizeManipulator : JComponent() {
    private val xAnchors = doubleArrayOf(2.5, 3.5, 4.5, 4.5, 5.5, 5.5, 6.5, 6.5, 6.5)
    private val yAnchors = doubleArrayOf(6.5, 5.5, 6.5, 4.5, 5.5, 3.5, 6.5, 4.5, 2.5)
    private val fixedSize = Dimension(SIDE, SIDE)

    override fun getPreferredSize(): Dimension {
      return fixedSize
    }

    override fun paint(g: Graphics) {
      super.paint(g)
      fillTriangle(g)
      fillDotPattern(g)
    }

    private fun fillTriangle(g: Graphics) {
      g.color = JBUI.CurrentTheme.ActionButton.pressedBackground()
      val xPoints = intArrayOf(0, width, width)
      val yPoints = intArrayOf(height, height, 0)
      g.fillPolygon(xPoints, yPoints, 3)
    }

    private fun fillDotPattern(g: Graphics) {
      val xStep = width / 8.0
      val yStep = height / 8.0
      g.color = g.color.increaseContrast()
      for ((xAnchor, yAnchor) in xAnchors.zip(yAnchors)) {
        val x = (xAnchor * xStep).toInt()
        val y = (yAnchor * yStep).toInt()
        g.fillOval(x, y, DOT_SIZE, DOT_SIZE)
      }
    }

    private fun Color.increaseContrast(): Color {
      return if (EditorColorsManager.getInstance().isDarkEditor) brighter() else darker()
    }
  }

  companion object {
    private const val MIN_HEIGHT = 100
    private const val MIN_WIDTH = 100

    private val SIDE = JBUIScale.scale(32)
    private val DOT_SIZE = JBUIScale.scale(2)
  }
}
