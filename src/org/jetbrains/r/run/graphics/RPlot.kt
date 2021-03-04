package org.jetbrains.r.run.graphics

import java.awt.Color

enum class RFontStyle {
  PLAIN,
  BOLD,
  ITALIC,
  BOLD_ITALIC,
}

data class RFont(val name: String?, val size: Float, val style: RFontStyle)

enum class RLineCap {
  ROUND,
  BUTT,
  SQUARE,
}

enum class RLineJoin {
  ROUND,
  MITER,
  BEVEL,
}

class RStroke(val width: Float, val cap: RLineCap, val join: RLineJoin, val miterLimit: Float, val pattern: FloatArray?)

data class RLayer(val viewportIndex: Int, val clippingAreaIndex: Int, val figures: List<RFigure>, val isAxisText: Boolean)

enum class RPlotError {
  UNKNOWN,
  TOO_COMPLEX,
  GROWING_TEXT,
  UNSUPPORTED_ACTION,
  MISMATCHING_ACTIONS,
}

data class RPlot(
  val number: Int,
  val fonts: List<RFont>,
  val colors: List<Color>,
  val strokes: List<RStroke>,
  val viewports: List<RViewport>,
  val layers: List<RLayer>,
  val previewComplexity: Int,
  val totalComplexity: Int,
  val error: RPlotError?
)
