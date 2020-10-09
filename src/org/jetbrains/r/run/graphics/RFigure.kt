package org.jetbrains.r.run.graphics

import java.awt.image.BufferedImage

sealed class RFigure {
  data class Circle(val center: RAffinePoint, val radius: Double, val strokeIndex: Int, val colorIndex: Int, val fillIndex: Int) : RFigure()

  data class Line(val from: RAffinePoint, val to: RAffinePoint, val strokeIndex: Int, val colorIndex: Int) : RFigure()

  data class Polygon(val points: List<RAffinePoint>, val strokeIndex: Int, val colorIndex: Int, val fillIndex: Int) : RFigure()

  data class Polyline(val points: List<RAffinePoint>, val strokeIndex: Int, val colorIndex: Int) : RFigure()

  data class Raster(val image: BufferedImage, val from: RAffinePoint, val to: RAffinePoint, val angle: Double, val interpolate: Boolean) :
    RFigure()

  data class Rectangle(val from: RAffinePoint, val to: RAffinePoint, val strokeIndex: Int, val colorIndex: Int, val fillIndex: Int) : RFigure()

  data class Text(val text: String, val position: RAffinePoint, val angle: Double, val anchor: Double, val fontIndex: Int, val colorIndex: Int) :
    RFigure()
}
