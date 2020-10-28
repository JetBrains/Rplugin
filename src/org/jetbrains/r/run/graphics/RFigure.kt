package org.jetbrains.r.run.graphics

import java.awt.image.BufferedImage

enum class RWinding {
  EVEN_ODD,
  NON_ZERO,
}

sealed class RFigure {
  data class Circle(val center: RAffinePoint, val radiusScale: Float, val radiusOffset: Float, val strokeIndex: Int,
                    val colorIndex: Int, val fillIndex: Int) : RFigure()

  data class Line(val from: RAffinePoint, val to: RAffinePoint, val strokeIndex: Int, val colorIndex: Int) : RFigure()

  data class Path(val subPaths: List<List<RAffinePoint>>, val winding: RWinding, val strokeIndex: Int, val colorIndex: Int,
                  val fillIndex: Int) : RFigure()

  data class Polygon(val points: List<RAffinePoint>, val strokeIndex: Int, val colorIndex: Int, val fillIndex: Int) : RFigure()

  data class Polyline(val points: List<RAffinePoint>, val strokeIndex: Int, val colorIndex: Int) : RFigure()

  data class Raster(val image: BufferedImage, val from: RAffinePoint, val to: RAffinePoint, val angle: Float, val interpolate: Boolean) :
    RFigure()

  data class Rectangle(val from: RAffinePoint, val to: RAffinePoint, val strokeIndex: Int, val colorIndex: Int, val fillIndex: Int) : RFigure()

  data class Text(val text: String, val position: RAffinePoint, val angle: Float, val anchor: Float, val fontIndex: Int, val colorIndex: Int) :
    RFigure()
}
