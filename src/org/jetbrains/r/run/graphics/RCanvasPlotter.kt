package org.jetbrains.r.run.graphics

import java.awt.*
import java.awt.geom.AffineTransform

class RCanvasPlotterProvider(override val parameters: RGraphicsUtils.ScreenParameters, private val graphics: Graphics2D) : RPlotterProvider {
  override fun create(fonts: List<RFont>, colors: List<Color>, strokes: List<RStroke>): RPlotter {
    return RCanvasPlotter(fonts.map { convert(it) }, colors, strokes.map { convert(it) }, graphics)
  }

  private fun convert(font: RFont): Font {
    val baseFont = if (font.name == null) graphics.font else Font(font.name, Font.PLAIN, 12)  // `size` has no effect
    return baseFont.deriveFont(font.size.toFloat()).deriveFont(convert(font.style))
  }

  private fun convert(style: RFontStyle): Int {
    return when (style) {
      RFontStyle.PLAIN -> Font.PLAIN
      RFontStyle.BOLD -> Font.BOLD
      RFontStyle.ITALIC -> Font.ITALIC
      RFontStyle.BOLD_ITALIC -> Font.BOLD + Font.ITALIC
    }
  }

  private fun convert(stroke: RStroke): Stroke {
    return BasicStroke(stroke.width.toFloat(), convert(stroke.cap), convert(stroke.join), stroke.miterLimit.toFloat(), stroke.pattern, 0.0f)
  }

  private fun convert(cap: RLineCap): Int {
    return when (cap) {
      RLineCap.ROUND -> BasicStroke.CAP_ROUND
      RLineCap.BUTT -> BasicStroke.CAP_BUTT
      RLineCap.SQUARE -> BasicStroke.CAP_SQUARE
    }
  }

  private fun convert(join: RLineJoin): Int {
    return when (join) {
      RLineJoin.ROUND -> BasicStroke.JOIN_ROUND
      RLineJoin.MITER -> BasicStroke.JOIN_MITER
      RLineJoin.BEVEL -> BasicStroke.JOIN_BEVEL
    }
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
    withColor(fillIndex) {
      graphics.fillOval(xFrom, yFrom, diameter, diameter)
    }
    withColor(colorIndex) {
      selectStroke(strokeIndex)
      graphics.drawOval(xFrom, yFrom, diameter, diameter)
    }
  }

  override fun drawLine(xFrom: Int, yFrom: Int, xTo: Int, yTo: Int, strokeIndex: Int, colorIndex: Int) {
    withColor(colorIndex) {
      selectStroke(strokeIndex)
      graphics.drawLine(xFrom, yFrom, xTo, yTo)
    }
  }

  override fun drawPolygon(xs: IntArray, ys: IntArray, strokeIndex: Int, colorIndex: Int, fillIndex: Int) {
    withColor(fillIndex) {
      graphics.fillPolygon(xs, ys, xs.size)
    }
    withColor(colorIndex) {
      selectStroke(strokeIndex)
      graphics.drawPolygon(xs, ys, xs.size)
    }
  }

  override fun drawPolyline(xs: IntArray, ys: IntArray, strokeIndex: Int, colorIndex: Int) {
    withColor(colorIndex) {
      selectStroke(strokeIndex)
      graphics.drawPolyline(xs, ys, xs.size)
    }
  }

  override fun drawRaster(image: Image, x: Int, y: Int, angle: Double) {
    val transform = AffineTransform().apply {
      translate(x.toDouble(), y.toDouble())
      rotate(Math.toRadians(angle))
    }
    graphics.drawImage(image, transform, null)
  }

  override fun drawRectangle(x: Int, y: Int, width: Int, height: Int, strokeIndex: Int, colorIndex: Int, fillIndex: Int) {
    withColor(fillIndex) {
      graphics.fillRect(x, y, width, height)
    }
    withColor(colorIndex) {
      selectStroke(strokeIndex)
      graphics.drawRect(x, y, width, height)
    }
  }

  override fun drawText(text: String, x: Int, y: Int, angle: Double, anchor: Double, fontIndex: Int, colorIndex: Int) {
    withColor(colorIndex) {
      selectFont(fontIndex)
      val width = graphics.fontMetrics.stringWidth(text)
      doRotated(x, y, angle) {
        val offset = (-width * anchor).toInt()
        graphics.drawString(text, offset, 0)
      }
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

  private inline fun withColor(colorIndex: Int, task: () -> Unit) {
    if (colorIndex >= 0) {
      graphics.color = colors[colorIndex]
      task()
    }
  }

  private fun selectFont(index: Int) {
    graphics.font = fonts[index]
  }

  private fun selectStroke(index: Int) {
    graphics.stroke = strokes[index]
  }
}
