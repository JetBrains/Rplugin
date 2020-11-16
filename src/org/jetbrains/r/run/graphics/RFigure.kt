package org.jetbrains.r.run.graphics

import java.awt.image.BufferedImage

enum class RWinding {
  EVEN_ODD,
  NON_ZERO,
}

class RPolyline(val points: LongArray, val previewCount: Int)

sealed class RFigure {
  data class Circle(val center: Long, val radius: Int, val strokeIndex: Int, val colorIndex: Int, val fillIndex: Int) : RFigure()

  data class Line(val from: Long, val to: Long, val strokeIndex: Int, val colorIndex: Int) : RFigure()

  data class Path(val subPaths: List<RPolyline>, val winding: RWinding, val strokeIndex: Int, val colorIndex: Int,
                  val fillIndex: Int) : RFigure()

  class Polygon(val polyline: RPolyline, val strokeIndex: Int, val colorIndex: Int, val fillIndex: Int) : RFigure()

  class Polyline(val polyline: RPolyline, val strokeIndex: Int, val colorIndex: Int) : RFigure()

  data class Raster(val image: BufferedImage, val from: Long, val to: Long, val angle: Float, val interpolate: Boolean) : RFigure()

  data class Rectangle(val from: Long, val to: Long, val strokeIndex: Int, val colorIndex: Int, val fillIndex: Int) : RFigure()

  data class Text(val text: String, val position: Long, val angle: Float, val anchor: Float, val fontIndex: Int, val colorIndex: Int) :
    RFigure()
}
