package org.jetbrains.r.run.graphics

import java.awt.Color
import java.awt.Image
import java.awt.Rectangle

interface RPlotterProvider {
  val parameters: RGraphicsUtils.ScreenParameters
  fun create(fonts: List<RFont>, colors: List<Color>, strokes: List<RStroke>): RPlotter
}

interface RPlotter {
  fun setClippingArea(area: Rectangle)
  fun getWidthOf(text: String, fontIndex: Int): Int
  fun drawCircle(x: Int, y: Int, radius: Int, strokeIndex: Int, colorIndex: Int, fillIndex: Int)
  fun drawLine(xFrom: Int, yFrom: Int, xTo: Int, yTo: Int, strokeIndex: Int, colorIndex: Int)
  fun drawPolygon(xs: IntArray, ys: IntArray, strokeIndex: Int, colorIndex: Int, fillIndex: Int)
  fun drawPolyline(xs: IntArray, ys: IntArray, strokeIndex: Int, colorIndex: Int)
  fun drawRaster(image: Image, x: Int, y: Int, angle: Double)
  fun drawRectangle(x: Int, y: Int, width: Int, height: Int, strokeIndex: Int, colorIndex: Int, fillIndex: Int)
  fun drawText(text: String, x: Int, y: Int, angle: Double, anchor: Double, fontIndex: Int, colorIndex: Int)
}
