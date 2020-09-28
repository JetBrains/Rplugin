package org.jetbrains.r.run.graphics.ui

import org.jetbrains.r.run.graphics.RCanvasPlotterProvider
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.RPlot
import org.jetbrains.r.run.graphics.RPlotUtil
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

class RPlotViewer : JComponent() {
  var plot: RPlot? = null
    set(value) {
      if (field !== value) {
        field = value
        repaint()
      }
    }

  var resolution: Int? = null
    set(value) {
      if (field != value) {
        field = value
        repaint()
      }
    }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    plot?.let { plot ->
      if (g is Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val parameters = RGraphicsUtils.ScreenParameters(size, resolution)
        val provider = RCanvasPlotterProvider(parameters, g)
        RPlotUtil.replay(plot, provider)
      }
    }
  }
}
