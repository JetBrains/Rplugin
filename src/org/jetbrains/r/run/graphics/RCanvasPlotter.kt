package org.jetbrains.r.run.graphics

import java.awt.*

class RCanvasPlotterProvider(override val parameters: RGraphicsUtils.ScreenParameters, private val graphics: Graphics2D) : RPlotterProvider {
  override fun create(fonts: List<RFont>, colors: List<Color>, strokes: List<RStroke>): RPlotter {
    return RCanvasPlotter(fonts.map { convert(it) }, colors, strokes.map { convert(it) }, graphics)
  }

  private fun convert(font: RFont): Font {
    val baseFont = if (font.name == null) graphics.font else Font(font.name, Font.PLAIN, 12)  // `size` has no effect
    return baseFont.deriveFont(font.size.toFloat())
  }

  private fun convert(stroke: RStroke): Stroke {
    return BasicStroke(stroke.width.toFloat())
  }
}

class RCanvasPlotter(
  private val fonts: List<Font>,
  private val colors: List<Color>,
  private val strokes: List<Stroke>,
  private val graphics: Graphics2D
) : RPlotter {
  override fun setClippingArea(area: Rectangle) {
    graphics.clip = area
  }

  override fun getWidthOf(text: String, fontIndex: Int): Int {
    selectFont(fontIndex)
    return graphics.fontMetrics.stringWidth(text)
  }

  override fun drawCircle(x: Int, y: Int, radius: Int, strokeIndex: Int, colorIndex: Int, fillIndex: Int) {
    val xFrom = x - radius
    val yFrom = y - radius
    val diameter = radius * 2
    selectColor(fillIndex)
    graphics.fillOval(xFrom, yFrom, diameter, diameter)
    selectColor(colorIndex)
    selectStroke(strokeIndex)
    graphics.drawOval(xFrom, yFrom, diameter, diameter)
  }

  override fun drawLine(xFrom: Int, yFrom: Int, xTo: Int, yTo: Int, strokeIndex: Int, colorIndex: Int) {
    selectColor(colorIndex)
    selectStroke(strokeIndex)
    graphics.drawLine(xFrom, yFrom, xTo, yTo)
  }

  override fun drawPolygon(xs: IntArray, ys: IntArray, strokeIndex: Int, colorIndex: Int, fillIndex: Int) {
    selectColor(fillIndex)
    graphics.fillPolygon(xs, ys, xs.size)
    selectColor(colorIndex)
    selectStroke(strokeIndex)
    graphics.drawPolygon(xs, ys, xs.size)
  }

  override fun drawPolyline(xs: IntArray, ys: IntArray, strokeIndex: Int, colorIndex: Int) {
    selectColor(colorIndex)
    selectStroke(strokeIndex)
    graphics.drawPolyline(xs, ys, xs.size)
  }

  override fun drawRectangle(x: Int, y: Int, width: Int, height: Int, strokeIndex: Int, colorIndex: Int, fillIndex: Int) {
    selectColor(fillIndex)
    graphics.fillRect(x, y, width, height)
    selectColor(colorIndex)
    selectStroke(strokeIndex)
    graphics.drawRect(x, y, width, height)
  }

  override fun drawText(text: String, x: Int, y: Int, angle: Double, anchor: Double, fontIndex: Int, colorIndex: Int) {
    selectFont(fontIndex)
    selectColor(colorIndex)
    val width = graphics.fontMetrics.stringWidth(text)
    doRotated(x, y, angle) {
      val offset = (-width * anchor).toInt()
      graphics.drawString(text, offset, 0)
    }
  }

  private inline fun doRotated(x: Int, y: Int, angle: Double, task: () -> Unit) {
    val radians = Math.toRadians(angle)
    graphics.translate(x, y)
    graphics.rotate(-radians)
    task()
    graphics.rotate(radians)
    graphics.translate(-x, -y)
  }

  private fun selectFont(index: Int) {
    graphics.font = fonts[index]
  }

  private fun selectColor(index: Int) {
    graphics.color = colors[index]
  }

  private fun selectStroke(index: Int) {
    graphics.stroke = strokes[index]
  }
}
