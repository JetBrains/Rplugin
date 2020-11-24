package org.jetbrains.r.run.graphics

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.ImageUtil
import org.intellij.datavis.r.inlays.components.ImageInverter
import org.jetbrains.r.RBundle
import org.jetbrains.r.rinterop.*
import org.jetbrains.r.rinterop.Font
import org.jetbrains.r.rinterop.Stroke
import java.awt.*
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Paths
import javax.swing.JLabel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object RPlotUtil {
  private const val PROTOCOL_VERSION = 11

  private val UNKNOWN_PARSING_ERROR_TEXT = RBundle.message("plot.viewer.unknown.parsing.error")
  private val GENERIC_PARSING_ERROR_TEXT = RBundle.message("plot.viewer.figure.parsing.failure")
  private val GROWING_TEXT_ERROR_TEXT = RBundle.message("plot.viewer.growing.text.error")
  private val TOO_COMPLEX_ERROR_TEXT = RBundle.message("plot.viewer.too.complex.error")

  fun writeTo(directory: File, plot: Plot, number: Int) {
    val plotFile = getPlotFile(directory, number)
    plot.writeTo(plotFile.outputStream())
  }

  fun readFrom(directory: File, number: Int): Plot? {
    val plotFile = getPlotFile(directory, number)
    if (!plotFile.exists()) {
      return null
    }
    return Plot.parseFrom(plotFile.inputStream())
  }

  private fun getPlotFile(directory: File, number: Int): File {
    val plotFileName = createPlotFileName(number)
    return Paths.get(directory.absolutePath, plotFileName).toFile()
  }

  private fun createPlotFileName(number: Int): String {
    return "recorded_v${PROTOCOL_VERSION}_${number}.plot"
  }

  fun getErrorDescription(error: RPlotError): String {
    return when (error) {
      RPlotError.UNKNOWN -> UNKNOWN_PARSING_ERROR_TEXT
      RPlotError.GROWING_TEXT -> GROWING_TEXT_ERROR_TEXT
      RPlotError.TOO_COMPLEX -> TOO_COMPLEX_ERROR_TEXT
      else -> GENERIC_PARSING_ERROR_TEXT
    }
  }

  fun convert(plot: Plot, number: Int): RPlot {
    val fonts = plot.fontList.map { convert(it) }
    val colors = plot.colorList.map { convert(it) }
    val strokes = plot.strokeList.map { convert(it) }
    val viewports = plot.viewportList.map { convert(it) }
    val layers = plot.layerList.map { convert(it) }
    val error = convertError(plot.error)
    return RPlot(number, fonts, colors, strokes, viewports, layers, plot.previewComplexity, plot.totalComplexity, error)
  }

  private fun convert(font: Font): RFont {
    val style = RFontStyle.values()[font.style]
    val name = font.name.takeIf { it.isNotBlank() }
    return RFont(name, font.size, style)
  }

  private fun convert(color: Int): Color {
    val alpha = color ushr 24
    val blue = (color ushr 16) and 0xff
    val green = (color ushr 8) and 0xff
    val red = color and 0xff
    return Color(red, green, blue, alpha)
  }

  private fun convert(stroke: Stroke): RStroke {
    val cap = RLineCap.values()[stroke.cap]
    val join = RLineJoin.values()[stroke.join]
    return RStroke(stroke.width, cap, join, stroke.miterLimit, convertPattern(stroke.pattern))
  }

  private fun convertPattern(packed: Int): FloatArray? {
    val dashCount = getDashCount(packed)
    if (dashCount == 0) {
      return null
    }
    return FloatArray(dashCount).also { pattern ->
      extractDashes(packed, pattern)
    }
  }

  private fun getDashCount(packed: Int): Int {
    return forEachDash(packed) { _, _ ->
      // do nothing
    }
  }

  private fun extractDashes(packed: Int, pattern: FloatArray) {
    forEachDash(packed) { index, dash ->
      pattern[index] = dash.toFloat()
    }
  }

  /**
   * @return count of dashes in a packed pattern
   */
  private inline fun forEachDash(packed: Int, task: (Int, Int) -> Unit): Int {
    var copy = packed
    var index = 0
    while (copy != 0) {
      task(index, copy and 0xf)
      copy = copy ushr 4
      index++
    }
    return index
  }

  private fun convert(viewport: Viewport): RViewport {
    return when (val case = viewport.kindCase) {
      Viewport.KindCase.FIXED -> convert(viewport.fixed)
      Viewport.KindCase.FREE -> convert(viewport.free)
      else -> throw RuntimeException("Unsupported viewport kind: $case")
    }
  }

  private fun convert(viewport: FixedViewport): RViewport {
    return RViewport.Fixed(viewport.ratio, viewport.delta, viewport.parentIndex)
  }

  private fun convert(viewport: FreeViewport): RViewport {
    return RViewport.Free(viewport.from, viewport.to, viewport.parentIndex)
  }

  private fun convert(layer: Layer): RLayer {
    val figures = layer.figureList.map { convert(it) }
    return RLayer(layer.viewportIndex, layer.clippingAreaIndex, figures, layer.isAxisText)
  }

  private fun convert(figure: Figure): RFigure {
    return when (val case = figure.kindCase) {
      Figure.KindCase.CIRCLE -> convert(figure.circle)
      Figure.KindCase.LINE -> convert(figure.line)
      Figure.KindCase.PATH -> convert(figure.path)
      Figure.KindCase.POLYGON -> convert(figure.polygon)
      Figure.KindCase.POLYLINE -> convert(figure.polyline)
      Figure.KindCase.RASTER -> convert(figure.raster)
      Figure.KindCase.RECTANGLE -> convert(figure.rectangle)
      Figure.KindCase.TEXT -> convert(figure.text)
      else -> throw RuntimeException("Unsupported figure kind: $case")
    }
  }

  private fun convert(circle: CircleFigure): RFigure {
    return RFigure.Circle(circle.center, circle.radius, circle.strokeIndex, circle.colorIndex, circle.fillIndex)
  }

  private fun convert(line: LineFigure): RFigure {
    return RFigure.Line(line.from, line.to, line.strokeIndex, line.colorIndex)
  }

  private fun convert(path: PathFigure): RFigure {
    val subPaths = path.subPathList.map { convert(it) }
    val winding = if (path.winding) RWinding.NON_ZERO else RWinding.EVEN_ODD
    return RFigure.Path(subPaths, winding, path.strokeIndex, path.colorIndex, path.fillIndex)
  }

  private fun convert(polygon: PolygonFigure): RFigure {
    return RFigure.Polygon(convert(polygon.polyline), polygon.strokeIndex, polygon.colorIndex, polygon.fillIndex)
  }

  private fun convert(polyline: PolylineFigure): RFigure {
    return RFigure.Polyline(convert(polyline.polyline), polyline.strokeIndex, polyline.colorIndex)
  }

  private fun convert(raster: RasterFigure): RFigure {
    return RFigure.Raster(convert(raster.image), raster.from, raster.to, raster.angle, raster.interpolate)
  }

  private fun convert(rectangle: RectangleFigure): RFigure {
    return RFigure.Rectangle(rectangle.from, rectangle.to, rectangle.strokeIndex, rectangle.colorIndex, rectangle.fillIndex)
  }

  private fun convert(text: TextFigure): RFigure {
    return RFigure.Text(text.text, text.position, text.angle, text.anchor, text.fontIndex, text.colorIndex)
  }

  private fun convert(image: RasterImage): BufferedImage {
    return ImageUtil.createImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB).also { outputImage ->
      val data = image.data
      for (y in 0 until image.height) {
        for (x in 0 until image.width) {
          // Note: extract ARGB color from little-endian byte string
          val baseIndex = ((y * image.width) + x) * 4
          val b = data.byteAt(baseIndex).toInt()
          val g = data.byteAt(baseIndex + 1).toInt()
          val r = data.byteAt(baseIndex + 2).toInt()
          val a = data.byteAt(baseIndex + 3).toInt()
          val argb = (a shl 24) or ((r and 0xff) shl 16) or ((g and 0xff) shl 8) or (b and 0xff)
          outputImage.setRGB(x, y, argb)
        }
      }
    }
  }

  private fun convert(polyline: Polyline): RPolyline {
    val points = LongArray(polyline.pointCount) { i ->
      polyline.getPoint(i)
    }
    return RPolyline(points, polyline.previewCount)
  }

  private fun convertError(error: Int): RPlotError? {
    return if (error > 0) RPlotError.values()[error - 1] else null
  }

  fun createImage(plot: RPlot, parameters: RGraphicsUtils.ScreenParameters, darkMode: Boolean, isPreview: Boolean): BufferedImage {
    return ImageUtil.createImage(parameters.width, parameters.height, BufferedImage.TYPE_INT_ARGB).also { image ->
      val graphics = image.graphics as Graphics2D
      graphics.font = JLabel().font
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
      val provider = RCanvasPlotterProvider(parameters, graphics)
      replay(plot, provider, darkMode, isPreview)
    }
  }

  fun replay(plot: RPlot, provider: RPlotterProvider, darkMode: Boolean, isPreview: Boolean) {
    val helper = ReplayHelper(plot, provider, darkMode, isPreview)
    helper.replay()
  }

  private class ReplayHelper(val plot: RPlot, provider: RPlotterProvider, private val darkMode: Boolean, private val isPreview: Boolean) {
    private val width = provider.parameters.width
    private val height = provider.parameters.height
    private val resolution = provider.parameters.resolution ?: RGraphicsUtils.DEFAULT_RESOLUTION

    private val editorColorsManager = EditorColorsManager.getInstance()
    private val colorScheme = editorColorsManager.globalScheme
    private val inverter = ImageInverter(colorScheme.defaultForeground, colorScheme.defaultBackground)

    private val plotter = provider.create(plot.fonts.map { scale(it) }, fitTheme(plot.colors), plot.strokes.map { scale(it) })
    private val clippingAreas = Array(plot.viewports.size) { Rectangle2D.Float() }
    private val gapWidths = IntArray(plot.fonts.size) { 0 }

    private var currentViewport = Rectangle2D.Float()

    init {
      clippingAreas[0] = Rectangle2D.Float(0.0f, 0.0f, width.toFloat(), height.toFloat())
      for (index in 1 until plot.viewports.size) {
        clippingAreas[index] = calculateViewport(plot.viewports[index])
      }
    }

    fun replay() {
      if (plot.error == null) {
        if (fitsDisplay()) {
          for (layer in plot.layers) {
            replay(layer)
          }
        } else {
          showMessage(MARGINS_TEXT)
        }
      } else {
        showMessage(getErrorDescription(plot.error))
      }
    }

    private fun fitsDisplay(): Boolean {
      return clippingAreas.find { it.width <= 0 || it.height <= 0 } == null
    }

    private fun showMessage(text: String) {
      val displayArea = clippingAreas.first()
      val x = displayArea.centerX.toFloat()
      val y = displayArea.centerY.toFloat()
      plotter.drawRectangle(0.0f, 0.0f, displayArea.width, displayArea.height, TRANSPARENT_INDEX, TRANSPARENT_INDEX, WHITE_COLOR_INDEX)
      plotter.drawText(text, x, y, 0.0f, 0.5f, DEFAULT_FONT_INDEX, BLACK_COLOR_INDEX)
    }

    private fun replay(layer: RLayer) {
      currentViewport = clippingAreas[layer.viewportIndex]
      plotter.setClippingArea(clippingAreas[layer.clippingAreaIndex])
      if (!layer.isAxisText) {
        for (figure in layer.figures) {
          replay(figure)
        }
      } else {
        replayAxisText(layer.figures)
      }
    }

    private fun replayAxisText(figures: List<RFigure>) {
      var previousWidth = 0
      var previousX = 0.0f
      var previousY = 0.0f
      var isFirst = true
      for (figure in figures) {
        val currentText = figure as RFigure.Text
        val currentWidth = plotter.getWidthOf(currentText.text, currentText.fontIndex)
        val currentX = calculateX(currentText.position)
        val currentY = calculateY(currentText.position)
        if (!isFirst) {
          val distance = calculateDistance(previousX, previousY, currentX, currentY)
          val gapWidth = getGapWidth(currentText.fontIndex)
          if (previousWidth + currentWidth > 2 * (distance - gapWidth)) {
            continue
          }
        }
        // Okay, there is enough space for this text
        replay(currentText, currentX, currentY)
        previousWidth = currentWidth
        previousX = currentX
        previousY = currentY
        isFirst = false
      }
    }

    private fun getGapWidth(fontIndex: Int): Int {
      if (gapWidths[fontIndex] == 0) {
        gapWidths[fontIndex] = plotter.getWidthOf(GAP_TEXT, fontIndex)
      }
      return gapWidths[fontIndex]
    }

    private fun replay(figure: RFigure) {
      when (figure) {
        is RFigure.Circle -> replay(figure)
        is RFigure.Line -> replay(figure)
        is RFigure.Path -> replay(figure)
        is RFigure.Polygon -> replay(figure)
        is RFigure.Polyline -> replay(figure)
        is RFigure.Raster -> replay(figure)
        is RFigure.Rectangle -> replay(figure)
        is RFigure.Text -> replay(figure)
      }
    }

    private fun replay(circle: RFigure.Circle) {
      if (isPreview && (circle.center and BIT_63 != 0L)) {
        return
      }
      val x = calculateX(circle.center)
      val y = calculateY(circle.center)
      val radius = calculate(circle.radius, currentViewport.height)
      val colorIndex = filterColorIndex(circle.strokeIndex, circle.colorIndex, circle.fillIndex)
      plotter.drawCircle(x, y, radius, circle.strokeIndex, colorIndex, circle.fillIndex)
    }

    private fun replay(line: RFigure.Line) {
      val xFrom = calculateX(line.from)
      val yFrom = calculateY(line.from)
      val xTo = calculateX(line.to)
      val yTo = calculateY(line.to)
      plotter.drawLine(xFrom, yFrom, xTo, yTo, line.strokeIndex, line.colorIndex)
    }

    private fun replay(path: RFigure.Path) {
      val subPaths = path.subPaths.map { points ->
        val xs = calculateXs(points)
        val ys = calculateYs(points)
        Pair(xs, ys)
      }
      val colorIndex = filterColorIndex(path.strokeIndex, path.colorIndex, path.fillIndex)
      plotter.drawPath(subPaths, path.winding, path.strokeIndex, colorIndex, path.fillIndex)
    }

    private fun replay(polygon: RFigure.Polygon) {
      val xs = calculateXs(polygon.polyline)
      val ys = calculateYs(polygon.polyline)
      val colorIndex = filterColorIndex(polygon.strokeIndex, polygon.colorIndex, polygon.fillIndex)
      plotter.drawPolygon(xs, ys, polygon.strokeIndex, colorIndex, polygon.fillIndex)
    }

    private fun replay(polyline: RFigure.Polyline) {
      val xs = calculateXs(polyline.polyline)
      val ys = calculateYs(polyline.polyline)
      plotter.drawPolyline(xs, ys, polyline.strokeIndex, polyline.colorIndex)
    }

    private fun replay(raster: RFigure.Raster) {
      val xFrom = calculateX(raster.from)
      val yFrom = calculateY(raster.from)
      val xTo = calculateX(raster.to)
      val yTo = calculateY(raster.to)
      val multiplier = if (RGraphicsUtils.isHiDpi) 2 else 1
      val width = ((xTo - xFrom) * multiplier).toInt()
      val height = ((yTo - yFrom) * multiplier).toInt()
      if (width > 0 && height > 0) {
        val original = fitTheme(raster.image)
        val mode = if (raster.interpolate) Image.SCALE_SMOOTH else Image.SCALE_FAST
        val scaled = original.getScaledInstance(width, height, mode)
        plotter.drawRaster(scaled, xFrom, yFrom, raster.angle)
      }
    }

    private fun replay(rectangle: RFigure.Rectangle) {
      val xFrom = calculateX(rectangle.from)
      val yFrom = calculateY(rectangle.from)
      val xTo = calculateX(rectangle.to)
      val yTo = calculateY(rectangle.to)
      val colorIndex = filterColorIndex(rectangle.strokeIndex, rectangle.colorIndex, rectangle.fillIndex)
      plotter.drawRectangle(xFrom, yFrom, xTo - xFrom, yTo - yFrom, rectangle.strokeIndex, colorIndex, rectangle.fillIndex)
    }

    private fun replay(text: RFigure.Text) {
      val x = calculateX(text.position)
      val y = calculateY(text.position)
      replay(text, x, y)
    }

    private fun replay(text: RFigure.Text, x: Float, y: Float) {
      plotter.drawText(text.text, x, y, text.angle, text.anchor, text.fontIndex, text.colorIndex)
    }

    private fun filterColorIndex(strokeIndex: Int, colorIndex: Int, fillIndex: Int): Int {
      val skipStroke = isPreview && strokeIndex >= 0 && fillIndex >= 0 && plot.strokes[strokeIndex].width < STROKE_WIDTH_THRESHOLD
      return if (skipStroke) -1 else colorIndex
    }

    private fun calculateXs(polyline: RPolyline): FloatArray {
      return calculateArray(polyline, this::calculateX)
    }

    private fun calculateYs(polyline: RPolyline): FloatArray {
      return calculateArray(polyline, this::calculateY)
    }

    private inline fun calculateArray(polyline: RPolyline, mapper: (Long) -> Float): FloatArray {
      val points = polyline.points
      return if (isPreview) {
        FloatArray(polyline.previewCount).also { coordinates ->
          var index = 0
          for (point in points) {
            if (point and BIT_63 == 0L) {
              coordinates[index] = mapper(point)
              index++
            }
          }
        }
      } else {
        FloatArray(points.size) { i ->
          mapper(points[i])
        }
      }
    }

    private fun calculateViewport(viewport: RViewport): Rectangle2D.Float {
      currentViewport = clippingAreas[viewport.parentIndex]
      return when (viewport) {
        is RViewport.Fixed -> calculateViewport(viewport)
        is RViewport.Free -> calculateRectangle(viewport.from, viewport.to)
      }
    }

    private fun calculateViewport(viewport: RViewport.Fixed): Rectangle2D.Float {
      val delta = viewport.delta * resolution
      val w1 = currentViewport.width
      val h1 = currentViewport.height
      val w2 = min(w1, (h1 - delta) / viewport.ratio)
      val h2 = min(h1, viewport.ratio * w1 + delta)
      val dx = (w1 - w2) / 2.0f
      val dy = (h1 - h2) / 2.0f
      return Rectangle2D.Float(currentViewport.x + dx, currentViewport.y + dy, w2, h2)
    }

    private fun calculateRectangle(from: Long, to: Long): Rectangle2D.Float {
      val xFrom = calculateX(from)
      val yFrom = calculateY(from)
      val xTo = calculateX(to)
      val yTo = calculateY(to)
      return Rectangle2D.Float(xFrom, yFrom, xTo - xFrom, yTo - yFrom)
    }

    private fun calculateX(point: Long): Float {
      return calculate((point ushr 32).toInt(), currentViewport.width) + currentViewport.x
    }

    private fun calculateY(point: Long): Float {
      return calculate(point.toInt(), currentViewport.height) + currentViewport.y
    }

    private fun calculate(coordinate: Int, side: Float): Float {
      // Note: left shift is used for sign extension as the most significant bit is reserved
      val scale = ((coordinate shl 1) shr 17).toShort() / 8192.0f
      val offset = coordinate.toShort() / 1024.0f
      return calculate(scale, offset, side)
    }

    private fun calculate(scale: Float, offset: Float, side: Float): Float {
      return scale * side + offset * resolution
    }

    private fun scale(font: RFont): RFont {
      return RFont(font.name, scale(font.size), font.style)
    }

    private fun scale(stroke: RStroke): RStroke {
      return RStroke(scale(stroke.width) / STROKE_WIDTH_SCALE, stroke.cap, stroke.join, stroke.miterLimit, stroke.pattern?.let { scale(it) })
    }

    private fun scale(pattern: FloatArray): FloatArray {
      return FloatArray(pattern.size) { index ->
        // Each element represents a dash length in points (1/72 of inch)
        scale(pattern[index] / 72.0f)
      }
    }

    private fun scale(value: Float): Float {
      return value * resolution
    }

    private fun fitTheme(image: BufferedImage): BufferedImage {
      return if (darkMode && editorColorsManager.isDarkEditor) inverter.invert(image) else image
    }

    private fun fitTheme(colors: List<Color>): List<Color> {
      return if (darkMode && editorColorsManager.isDarkEditor) colors.map { inverter.invert(it) } else colors
    }

    companion object {
      private const val DEFAULT_FONT_INDEX = 0
      private const val BLACK_COLOR_INDEX = 0
      private const val WHITE_COLOR_INDEX = 1
      private const val TRANSPARENT_INDEX = -1
      private const val BIT_63 = 1L shl 63

      private const val STROKE_WIDTH_THRESHOLD = 1.0f / 72.0f  // 1 px (in inches)
      private const val STROKE_WIDTH_SCALE = 1.25f

      private const val GAP_TEXT = "m"  // Not to be translated

      private val MARGINS_TEXT = RBundle.message("plot.viewer.figure.margins.too.large")

      private fun calculateDistance(xFrom: Float, yFrom: Float, xTo: Float, yTo: Float): Float {
        val xDelta = abs(xFrom - xTo)
        val yDelta = abs(yFrom - yTo)
        return max(xDelta, yDelta)
      }
    }
  }
}
