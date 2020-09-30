package org.jetbrains.r.run.graphics

import com.intellij.util.ui.ImageUtil
import org.jetbrains.r.rinterop.*
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.image.BufferedImage

object RPlotUtil {
  fun convert(plot: Plot, number: Int): RPlot {
    val fonts = plot.fontList.map { convert(it) }
    val colors = plot.colorList.map { convert(it) }
    val strokes = plot.strokeList.map { convert(it) }
    val viewports = plot.viewportList.map { convert(it) }
    val layers = plot.layerList.map { convert(it) }
    return RPlot(number, fonts, colors, strokes, viewports, layers)
  }

  private fun convert(font: Font): RFont {
    return RFont(font.name.takeIf { it.isNotBlank() }, font.size)
  }

  private fun convert(color: Int): Color {
    val alpha = color ushr 24
    val blue = (color ushr 16) and 0xff
    val green = (color ushr 8) and 0xff
    val red = color and 0xff
    return Color(red, green, blue, alpha)
  }

  private fun convert(stroke: Stroke): RStroke {
    return RStroke(stroke.width)
  }

  private fun convert(viewport: Viewport): RViewport {
    return RViewport(convert(viewport.from), convert(viewport.to))
  }

  private fun convert(layer: Layer): RLayer {
    val figures = layer.figureList.map { convert(it) }
    return RLayer(layer.viewportIndex, figures)
  }

  private fun convert(figure: Figure): RFigure {
    return when (val case = figure.kindCase) {
      Figure.KindCase.CIRCLE -> convert(figure.circle)
      Figure.KindCase.LINE -> convert(figure.line)
      Figure.KindCase.POLYGON -> convert(figure.polygon)
      Figure.KindCase.POLYLINE -> convert(figure.polyline)
      Figure.KindCase.RECTANGLE -> convert(figure.rectangle)
      Figure.KindCase.TEXT -> convert(figure.text)
      else -> throw RuntimeException("Unsupported figure kind: $case")
    }
  }

  private fun convert(circle: CircleFigure): RFigure {
    return RFigure.Circle(convert(circle.center), circle.radius, circle.strokeIndex, circle.colorIndex, circle.fillIndex)
  }

  private fun convert(line: LineFigure): RFigure {
    return RFigure.Line(convert(line.from), convert(line.to), line.strokeIndex, line.colorIndex)
  }

  private fun convert(polygon: PolygonFigure): RFigure {
    val points = polygon.pointList.map { convert(it) }
    return RFigure.Polygon(points, polygon.strokeIndex, polygon.colorIndex, polygon.fillIndex)
  }

  private fun convert(polyline: PolylineFigure): RFigure {
    val points = polyline.pointList.map { convert(it) }
    return RFigure.Polyline(points, polyline.strokeIndex, polyline.colorIndex)
  }

  private fun convert(rectangle: RectangleFigure): RFigure {
    return RFigure.Rectangle(convert(rectangle.from), convert(rectangle.to), rectangle.strokeIndex, rectangle.colorIndex, rectangle.fillIndex)
  }

  private fun convert(text: TextFigure): RFigure {
    return RFigure.Text(text.text, convert(text.position), text.angle, text.anchor, text.fontIndex, text.colorIndex)
  }

  private fun convert(point: AffinePoint): RAffinePoint {
    return RAffinePoint(point.xScale, point.xOffset, point.yScale, point.yOffset)
  }

  fun createImage(plot: RPlot, parameters: RGraphicsUtils.ScreenParameters): BufferedImage {
    return ImageUtil.createImage(parameters.width, parameters.height, BufferedImage.TYPE_INT_ARGB).also { image ->
      val graphics = image.graphics as Graphics2D
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val provider = RCanvasPlotterProvider(parameters, graphics)
      replay(plot, provider)
    }
  }

  fun replay(plot: RPlot, provider: RPlotterProvider) {
    val helper = ReplayHelper(plot, provider)
    helper.replay()
  }

  private class ReplayHelper(val plot: RPlot, provider: RPlotterProvider) {
    private val width = provider.parameters.width
    private val height = provider.parameters.height
    private val resolution = provider.parameters.resolution ?: RGraphicsUtils.DEFAULT_RESOLUTION

    private val plotter = provider.create(plot.fonts.map { scale(it) }, plot.colors, plot.strokes.map { scale(it) })
    private val clippingAreas = plot.viewports.map { calculateRectangle(it.from, it.to) }

    fun replay() {
      for (layer in plot.layers) {
        plotter.setClippingArea(clippingAreas[layer.viewportIndex])
        for (figure in layer.figures) {
          replay(figure)
        }
      }
    }

    private fun replay(figure: RFigure) {
      when (figure) {
        is RFigure.Circle -> replay(figure)
        is RFigure.Line -> replay(figure)
        is RFigure.Polygon -> replay(figure)
        is RFigure.Polyline -> replay(figure)
        is RFigure.Rectangle -> replay(figure)
        is RFigure.Text -> replay(figure)
      }
    }

    private fun replay(circle: RFigure.Circle) {
      val x = calculateX(circle.center)
      val y = calculateY(circle.center)
      val radius = scale(circle.radius).toInt()
      plotter.drawCircle(x, y, radius, circle.strokeIndex, circle.colorIndex, circle.fillIndex)
    }

    private fun replay(line: RFigure.Line) {
      val xFrom = calculateX(line.from)
      val yFrom = calculateY(line.from)
      val xTo = calculateX(line.to)
      val yTo = calculateY(line.to)
      plotter.drawLine(xFrom, yFrom, xTo, yTo, line.strokeIndex, line.colorIndex)
    }

    private fun replay(polygon: RFigure.Polygon) {
      val xs = calculateXs(polygon.points)
      val ys = calculateYs(polygon.points)
      plotter.drawPolygon(xs, ys, polygon.strokeIndex, polygon.colorIndex, polygon.fillIndex)
    }

    private fun replay(polyline: RFigure.Polyline) {
      val xs = calculateXs(polyline.points)
      val ys = calculateYs(polyline.points)
      plotter.drawPolyline(xs, ys, polyline.strokeIndex, polyline.colorIndex)
    }

    private fun replay(rectangle: RFigure.Rectangle) {
      val xFrom = calculateX(rectangle.from)
      val yFrom = calculateY(rectangle.from)
      val xTo = calculateX(rectangle.to)
      val yTo = calculateY(rectangle.to)
      plotter.drawRectangle(xFrom, yFrom, xTo - xFrom, yTo - yFrom, rectangle.strokeIndex, rectangle.colorIndex, rectangle.fillIndex)
    }

    private fun replay(text: RFigure.Text) {
      val x = calculateX(text.position)
      val y = calculateY(text.position)
      plotter.drawText(text.text, x, y, text.angle, text.anchor, text.fontIndex, text.colorIndex)
    }

    private fun calculateXs(points: List<RAffinePoint>): IntArray {
      return IntArray(points.size) { i ->
        calculateX(points[i])
      }
    }

    private fun calculateYs(points: List<RAffinePoint>): IntArray {
      return IntArray(points.size) { i ->
        calculateY(points[i])
      }
    }

    private fun calculateRectangle(from: RAffinePoint, to: RAffinePoint): Rectangle {
      val xFrom = calculateX(from)
      val yFrom = calculateY(from)
      val xTo = calculateX(to)
      val yTo = calculateY(to)
      return Rectangle(xFrom, yFrom, xTo - xFrom, yTo - yFrom)
    }

    private fun calculateX(point: RAffinePoint): Int {
      return calculate(point.xScale, point.xOffset, width)
    }

    private fun calculateY(point: RAffinePoint): Int {
      return calculate(point.yScale, point.yOffset, height)
    }

    private fun calculate(scale: Double, offset: Double, side: Int): Int {
      return (scale * side + offset * resolution).toInt()
    }

    private fun scale(font: RFont): RFont {
      return RFont(font.name, scale(font.size))
    }

    private fun scale(stroke: RStroke): RStroke {
      return RStroke(scale(stroke.width))
    }

    private fun scale(value: Double): Double {
      return value * resolution
    }
  }
}
