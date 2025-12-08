package com.intellij.r.psi.run.graphics

import java.awt.*
import java.awt.geom.*

class RCanvasPlotterProvider(override val parameters: RGraphicsUtils.ScreenParameters, private val graphics: Graphics2D) : RPlotterProvider {
  override fun create(fonts: List<RFont>, colors: List<Color>, strokes: List<RStroke>): RPlotter {
    return RCanvasPlotter(fonts.map { convert(it) }, colors, strokes.map { convert(it) }, graphics)
  }

  private fun convert(font: RFont): Font {
    val baseFont = if (font.name == null) graphics.font else Font(font.name, Font.PLAIN, 12)  // `size` has no effect
    return baseFont.deriveFont(font.size).deriveFont(convert(font.style))
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
    return BasicStroke(stroke.width, convert(stroke.cap), convert(stroke.join), stroke.miterLimit, stroke.pattern, 0.0f)
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
  private val cachedRectangle = Rectangle2D.Float()
  private val cachedEllipse = Ellipse2D.Float()
  private val cachedLine = Line2D.Float()

  override fun setClippingArea(area: Rectangle2D.Float) {
    graphics.clip = area
  }

  override fun getWidthOf(text: String, fontIndex: Int): Int {
    selectFont(fontIndex)
    return graphics.fontMetrics.stringWidth(text)
  }

  override fun drawCircle(x: Float, y: Float, radius: Float, strokeIndex: Int, colorIndex: Int, fillIndex: Int) {
    withCircle(x, y, radius) { circle ->
      withColor(fillIndex) {
        graphics.fill(circle)
      }
      withColor(colorIndex) {
        selectStroke(strokeIndex)
        graphics.draw(circle)
      }
    }
  }

  override fun drawLine(xFrom: Float, yFrom: Float, xTo: Float, yTo: Float, strokeIndex: Int, colorIndex: Int) {
    withLine(xFrom, yFrom, xTo, yTo) { line ->
      withColor(colorIndex) {
        selectStroke(strokeIndex)
        graphics.draw(line)
      }
    }
  }

  override fun drawPath(subPaths: List<Pair<FloatArray, FloatArray>>, winding: RWinding, strokeIndex: Int, colorIndex: Int, fillIndex: Int) {
    val path = createPath(subPaths, winding)
    withColor(fillIndex) {
      graphics.fill(path)
    }
    withColor(colorIndex) {
      selectStroke(strokeIndex)
      graphics.draw(path)
    }
  }

  override fun drawPolygon(xs: FloatArray, ys: FloatArray, strokeIndex: Int, colorIndex: Int, fillIndex: Int) {
    val polygon = createPolyline(xs, ys, isClosed = true)
    withColor(fillIndex) {
      graphics.fill(polygon)
    }
    withColor(colorIndex) {
      selectStroke(strokeIndex)
      graphics.draw(polygon)
    }
  }

  override fun drawPolyline(xs: FloatArray, ys: FloatArray, strokeIndex: Int, colorIndex: Int) {
    val polyline = createPolyline(xs, ys, isClosed = false)
    withColor(colorIndex) {
      selectStroke(strokeIndex)
      graphics.draw(polyline)
    }
  }

  override fun drawRaster(image: Image, x: Float, y: Float, angle: Float) {
    val transform = AffineTransform().apply {
      translate(x.toDouble(), y.toDouble())
      rotate(Math.toRadians(angle.toDouble()))
    }
    graphics.drawImage(image, transform, null)
  }

  override fun drawRectangle(x: Float, y: Float, width: Float, height: Float, strokeIndex: Int, colorIndex: Int, fillIndex: Int) {
    withRectangle(x, y, width, height) { rectangle ->
      withColor(fillIndex) {
        graphics.fill(rectangle)
      }
      withColor(colorIndex) {
        selectStroke(strokeIndex)
        graphics.draw(rectangle)
      }
    }
  }

  override fun drawText(text: String, x: Float, y: Float, angle: Float, anchor: Float, fontIndex: Int, colorIndex: Int) {
    withColor(colorIndex) {
      selectFont(fontIndex)
      val width = graphics.fontMetrics.stringWidth(text)
      doRotated(x.toDouble(), y.toDouble(), angle.toDouble()) {
        val offset = (-width * anchor).toInt()
        graphics.drawString(text, offset, 0)
      }
    }
  }

  private inline fun doRotated(x: Double, y: Double, angle: Double, task: () -> Unit) {
    val radians = Math.toRadians(angle)
    graphics.translate(x, y)
    graphics.rotate(-radians)
    task()
    graphics.rotate(radians)
    graphics.translate(-x, -y)
  }

  private inline fun withRectangle(x: Float, y: Float, width: Float, height: Float, task: (Rectangle2D.Float) -> Unit) {
    cachedRectangle.x = x
    cachedRectangle.y = y
    cachedRectangle.width = width
    cachedRectangle.height = height
    task(cachedRectangle)
  }

  private inline fun withCircle(x: Float, y: Float, radius: Float, task: (Ellipse2D.Float) -> Unit) {
    val diameter = radius * 2.0f
    cachedEllipse.x = x - radius
    cachedEllipse.y = y - radius
    cachedEllipse.width = diameter
    cachedEllipse.height = diameter
    task(cachedEllipse)
  }

  private inline fun withLine(xFrom: Float, yFrom: Float, xTo: Float, yTo: Float, task: (Line2D.Float) -> Unit) {
    cachedLine.x1 = xFrom
    cachedLine.y1 = yFrom
    cachedLine.x2 = xTo
    cachedLine.y2 = yTo
    task(cachedLine)
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

  companion object {
    private fun createPolyline(xs: FloatArray, ys: FloatArray, isClosed: Boolean): Path2D.Float {
      return Path2D.Float().also { path ->
        addSubPath(path, xs, ys, isClosed)
      }
    }

    private fun createPath(subPaths: List<Pair<FloatArray, FloatArray>>, winding: RWinding): Path2D.Float {
      val rule = when (winding) {
        RWinding.EVEN_ODD -> Path2D.WIND_EVEN_ODD
        RWinding.NON_ZERO -> Path2D.WIND_NON_ZERO
      }
      return Path2D.Float(rule).also { path ->
        for ((xs, ys) in subPaths) {
          addSubPath(path, xs, ys, isClosed = true)
        }
      }
    }

    private fun addSubPath(path: Path2D.Float, xs: FloatArray, ys: FloatArray, isClosed: Boolean) {
      path.moveTo(xs[0], ys[0])
      for (i in 1 until xs.size) {
        path.lineTo(xs[i], ys[i])
      }
      if (isClosed) {
        path.closePath()
      }
    }
  }
}
