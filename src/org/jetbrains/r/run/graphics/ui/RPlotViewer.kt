package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.reference.SoftReference
import com.intellij.util.ui.ImageUtil
import org.jetbrains.r.run.graphics.RCanvasPlotterProvider
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.RPlot
import org.jetbrains.r.run.graphics.RPlotUtil
import org.jetbrains.r.settings.RGraphicsSettings
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JComponent

class RPlotViewer(project: Project, parent: Disposable) : JComponent() {
  private var cachedImage = SoftReference<BufferedImage>(null)

  private var darkMode = RGraphicsSettings.isDarkModeEnabled(project)
    set(value) {
      if (field != value) {
        field = value
        refresh()
      }
    }

  var plot: RPlot? = null
    set(value) {
      if (field !== value) {
        field = value
        refresh()
      }
    }

  var resolution: Int? = null
    set(value) {
      if (field != value) {
        field = value
        refresh()
      }
    }

  val parameters: RGraphicsUtils.ScreenParameters
    get() = RGraphicsUtils.ScreenParameters(size, resolution)

  init {
    project.messageBus.connect(parent).subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
      refresh()
    })
    RGraphicsSettings.addDarkModeListener(project, parent) { isEnabled ->
      darkMode = isEnabled
    }
  }

  private fun refresh() {
    cachedImage.clear()
    repaint()
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    plot?.let { plot ->
      val image = getOrDrawImage(plot)
      g.drawImage(image, 0, 0, width, height, null)
    }
  }

  private fun getOrDrawImage(plot: RPlot): BufferedImage {
    return cachedImage.getIfSameSize() ?: drawImage(plot).also { image ->
      cachedImage = SoftReference(image)
    }
  }

  private fun drawImage(plot: RPlot): BufferedImage {
    return ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
      val g = image.graphics as Graphics2D
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
      val provider = RCanvasPlotterProvider(parameters, g)
      RPlotUtil.replay(plot, provider, darkMode)
    }
  }

  private fun SoftReference<BufferedImage>.getIfSameSize(): BufferedImage? {
    return get()?.takeIf { image ->
      ImageUtil.getUserWidth(image) == width && ImageUtil.getUserHeight(image) == height
    }
  }
}
