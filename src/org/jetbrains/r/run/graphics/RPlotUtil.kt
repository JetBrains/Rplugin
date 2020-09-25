package org.jetbrains.r.run.graphics

import org.jetbrains.r.rinterop.*
import java.awt.Color

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
}
