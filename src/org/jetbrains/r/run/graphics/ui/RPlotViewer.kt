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
import java.awt.Graphics
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

  private fun getOrCreateImage(plot: RPlot): BufferedImage {
    return cachedImage.getIfSameSize() ?: RPlotUtil.createImage(plot, parameters, darkMode).also { image ->
      cachedImage = SoftReference(image)
    }
  }

  private fun SoftReference<BufferedImage>.getIfSameSize(): BufferedImage? {
    return get()?.takeIf { image ->
      ImageUtil.getUserWidth(image) == width && ImageUtil.getUserHeight(image) == height
    }
  }

  companion object {
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
