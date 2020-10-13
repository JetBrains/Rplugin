package org.jetbrains.r.run.graphics

import java.awt.Color

data class RAffinePoint(val xScale: Double, val xOffset: Double, val yScale: Double, val yOffset: Double)

enum class RFontStyle {
  PLAIN,
  BOLD,
  ITALIC,
  BOLD_ITALIC,
}

data class RFont(val name: String?, val size: Double, val style: RFontStyle)

data class RStroke(val width: Double)

data class RLayer(val viewportIndex: Int, val clippingAreaIndex: Int, val figures: List<RFigure>, val isAxisText: Boolean)

data class RPlot(
  val number: Int,
  val fonts: List<RFont>,
  val colors: List<Color>,
  val strokes: List<RStroke>,
  val viewports: List<RViewport>,
  val layers: List<RLayer>
)
