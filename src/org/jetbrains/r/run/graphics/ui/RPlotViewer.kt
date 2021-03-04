package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.reference.SoftReference
import com.intellij.util.ui.ImageUtil
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.RPlot
import org.jetbrains.r.run.graphics.RPlotUtil
import org.jetbrains.r.settings.RGraphicsSettings
import java.awt.Dimension
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.util.*
import javax.swing.JComponent

class RPlotViewer(project: Project, parent: Disposable) : JComponent() {
  private val timer = Timer()
  private var timerTask: TimerTask? = null

  @Volatile
  private var cachedImage = SoftReference<BufferedImage>(null)

  @Volatile
  private var darkMode = RGraphicsSettings.isDarkModeEnabled(project)
    set(value) {
      if (field != value) {
        field = value
        refresh()
      }
    }

  @Volatile
  var plot: RPlot? = null
    set(value) {
      if (field !== value) {
        field = value
        refresh()
      }
    }

  @Volatile
  var resolution: Int? = null
    set(value) {
      if (field != value) {
        field = value
        refresh()
      }
    }

  var overlayComponent: JComponent? = null

  val parameters: RGraphicsUtils.ScreenParameters
    get() = RGraphicsUtils.ScreenParameters(size, resolution)

  val image: BufferedImage?
    get() = plot?.let { plot ->
      getOrCreateImage(plot)
    }

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
    image?.let { image ->
      withComponentPreserved(overlayComponent) {
        g.drawImage(image, 0, 0, width, height, null)
      }
    }
  }

  private fun getOrCreateImage(plot: RPlot): BufferedImage? {
    return cachedImage.getIfSameSize() ?: createImage(plot)?.also { image ->
      cachedImage = SoftReference(image)
    }
  }

  private fun createImage(plot: RPlot): BufferedImage? {
    if (!size.isValid) {
      return null
    }
    val isPreview = plot.totalComplexity > TOTAL_COMPLEXITY_THRESHOLD && plot.complexityRatio < COMPLEXITY_RATIO_THRESHOLD
    if (isPreview) {
      scheduleRender(plot)
    }
    return RPlotUtil.createImage(plot, parameters, darkMode, isPreview)
  }

  private fun scheduleRender(plot: RPlot) {
    scheduleTask {
      // Note: the rendering is started with a delay.
      // It's necessary to check whether the viewer still shows the same plot
      if (this.plot === plot && size.isValid) {
        val darkMode = this.darkMode
        val parameters = this.parameters
        val image = RPlotUtil.createImage(plot, parameters, darkMode, isPreview = false)
        // Note: it might take more than 500 ms to render some plots so this check must be performed again
        if (this.plot === plot && this.darkMode == darkMode && this.parameters == parameters) {
          cachedImage = SoftReference(image)
          repaint()
        }
      }
    }
  }

  private fun scheduleTask(task: () -> Unit) {
    timerTask?.cancel()
    timerTask = object : TimerTask() {
      override fun run() {
        task()
      }
    }
    timer.schedule(timerTask, TIMER_DELAY)
  }

  private fun SoftReference<BufferedImage>.getIfSameSize(): BufferedImage? {
    return get()?.takeIf { image ->
      ImageUtil.getUserWidth(image) == width && ImageUtil.getUserHeight(image) == height
    }
  }

  companion object {
    private const val COMPLEXITY_RATIO_THRESHOLD = 0.8
    private const val TOTAL_COMPLEXITY_THRESHOLD = 1000
    private const val TIMER_DELAY = 500L

    private val Dimension.isValid: Boolean
      get() = width > 0 && height > 0

    private val RPlot.complexityRatio: Double
      get() = previewComplexity.toDouble() / totalComplexity.toDouble()

    private inline fun withComponentPreserved(component: JComponent?, task: () -> Unit) {
      if (component != null && component.isVisible) {
        component.isVisible = false
        task()
        component.isVisible = true
      } else {
        task()
      }
    }
  }
}
