package org.jetbrains.r.run.graphics

import java.awt.Color
import java.awt.Image
import java.awt.geom.Rectangle2D

interface RPlotterProvider {
  val parameters: RGraphicsUtils.ScreenParameters
  fun create(fonts: List<RFont>, colors: List<Color>, strokes: List<RStroke>): RPlotter
}

interface RPlotter {
  fun setClippingArea(area: Rectangle2D.Float)
  fun getWidthOf(text: String, fontIndex: Int): Int
  fun drawCircle(x: Float, y: Float, radius: Float, strokeIndex: Int, colorIndex: Int, fillIndex: Int)
  fun drawLine(xFrom: Float, yFrom: Float, xTo: Float, yTo: Float, strokeIndex: Int, colorIndex: Int)
  fun drawPolygon(xs: FloatArray, ys: FloatArray, strokeIndex: Int, colorIndex: Int, fillIndex: Int)
  fun drawPolyline(xs: FloatArray, ys: FloatArray, strokeIndex: Int, colorIndex: Int)
  fun drawRaster(image: Image, x: Float, y: Float, angle: Float)
  fun drawRectangle(x: Float, y: Float, width: Float, height: Float, strokeIndex: Int, colorIndex: Int, fillIndex: Int)
  fun drawText(text: String, x: Float, y: Float, angle: Float, anchor: Float, fontIndex: Int, colorIndex: Int)
}
