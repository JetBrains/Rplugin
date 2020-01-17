/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.BinaryLightVirtualFile
import com.intellij.util.messages.Topic
import com.intellij.util.ui.UIUtil
import org.intellij.images.editor.ImageEditor
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.editor.impl.ImageEditorImpl
import org.intellij.images.ui.ImageComponent
import java.awt.Dimension
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.math.max
import kotlin.math.min

val CHANGE_DARK_MODE_TOPIC = Topic.create("Graphics Panel Dark Mode Topic", DarkModeNotifier::class.java)

interface DarkModeNotifier {
  fun onDarkModeChanged(isEnabled: Boolean)
}

class GraphicsPanel(private val project: Project, private val disposableParent: Disposable) {
  private val label = JLabel(NO_GRAPHICS, JLabel.CENTER)
  private val rootPanel = EmptyComponentPanel(label)

  private var currentFile: File? = null

  private val advancedModeComponent: JComponent?
    get() = currentEditor?.component

  private val basicModeComponent: JComponent?
    get() = UIUtil.findComponentOfType(currentEditor?.contentComponent, ImageComponent::class.java)

  private val internalComponent: JComponent?
    get() = if (isAdvancedMode) advancedModeComponent else basicModeComponent

  private val toolPanelHeight: Int
    get() = if (isAdvancedMode) getAdvancedModeToolPanelHeight() else 0

  private var currentImageFile: VirtualFile? = null
  private var currentEditor: ImageEditor? = null
  private var lastToolPanelHeight: Int = 0

  val component = rootPanel.component

  val imageSize: Dimension?
    get() = currentEditor?.document?.value?.let { image ->
      Dimension(image.width / scaleMultiplier, image.height / scaleMultiplier)
    }

  val imageComponentSize: Dimension
    get() = calculateImageSizeForRegion(component.size, toolPanelHeight)

  val maximumSize: Dimension?
    get() = imageSize?.let { size ->
      val insets = imageInsets
      return Dimension(size.width + insets * 2, size.height + toolPanelHeight + insets * 2)
    }

  /**
   * Enables or disables toolbar at the top of graphics panel.
   * Also in advanced mode panel keeps aspect ratio of image.
   * Use it when displaying images which don't fit panel's size
   */
  var isAdvancedMode: Boolean = false
    set(mode) {
      if (field != mode) {
        field = mode
        currentImageFile?.let { file ->
          openEditor(file)
        }
      }
    }

  @Volatile
  private var darkMode = true

  init {
    val connect = project.messageBus.connect()
    connect.subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
      currentFile?.let { showImage(it) }
    })
    connect.subscribe(CHANGE_DARK_MODE_TOPIC, object : DarkModeNotifier {
      override fun onDarkModeChanged(isEnabled: Boolean) {
        darkMode = isEnabled
        currentFile?.let { showImage(it) }
      }
    })
  }

  fun showImage(imageFile: File) {
    if (!tryShowImage(imageFile)) {
      closeEditor(GRAPHICS_COULD_NOT_BE_LOADED)
    }
  }

  fun showMessage(message: String) {
    closeEditor(message)
  }

  fun reset() {
    closeEditor(NO_GRAPHICS)
  }

  private fun tryShowImage(imageFile: File): Boolean {
    try {
      if (!imageFile.exists()) return false
      val editorColorsManager = EditorColorsManager.getInstance()
      val content = if (editorColorsManager.isDarkEditor && darkMode)
        createInvertedImage(imageFile.readBytes(), editorColorsManager.globalScheme)
      else imageFile.readBytes()
      currentFile = imageFile
      var result = true
      invokeAndWaitIfNeeded {
        if (Disposer.isDisposed(disposableParent)) {
          result = false
          return@invokeAndWaitIfNeeded
        }
        openEditor(BinaryLightVirtualFile(imageFile.name, content))
      }
      return result
    } catch (e: Exception) {
      LOGGER.error("Failed to load graphics", e)
    }
    return false
  }

  private fun createInvertedImage(content: ByteArray, globalScheme: EditorColorsScheme): ByteArray {
    val defaultForeground = globalScheme.defaultForeground
    val defaultBackground  = globalScheme.defaultBackground
    val rgb = FloatArray(3)
    val whiteHSL = FloatArray(3)
    val blackHSL = FloatArray(3)
    val currentHSL = FloatArray(3)
    val saturation = 1
    val luminance = 2

    defaultForeground.getRGBColorComponents(rgb)
    convertRGBtoHSL(rgb, whiteHSL)
    defaultBackground.getRGBColorComponents(rgb)
    convertRGBtoHSL(rgb, blackHSL)

    val bufferedImage = ImageIO.read(ByteArrayInputStream(content))
    for (x in 0 until bufferedImage.getWidth()) {
      for (y in 0 until bufferedImage.getHeight()) {
        val rgba: Int = bufferedImage.getRGB(x, y)
        val alpha = ((rgba shr 24) and 255) / 255f
        rgb[0] = ((rgba shr 16) and 255) / 255f
        rgb[1] = ((rgba shr 8) and 255) / 255f
        rgb[2] = ((rgba) and 255) / 255f
        convertRGBtoHSL(rgb, currentHSL)
        currentHSL[saturation] = currentHSL[saturation] * (50.0f + whiteHSL[saturation]) / 1.5f / 100f
        currentHSL[luminance] = (100 - currentHSL[luminance]) * (whiteHSL[luminance] - blackHSL[luminance]) / 100f  + blackHSL[luminance]
        bufferedImage.setRGB(x, y, convertHCLtoRGB(currentHSL, alpha))
      }
    }
    val baos = ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", baos)
    baos.flush()
    val output = baos.toByteArray()
    baos.close()
    return output
  }

  private fun invert(color: Int, white: Int, black: Int) = (255 - color) * (white - black) / 255 + black

  private fun openEditor(file: VirtualFile) {
    closeEditor(NO_GRAPHICS)
    val editor: ImageEditor = ImageEditorImpl(project, file)  // Note: explicit cast prevents compiler warnings
    adjustImageZoom(editor.zoomModel)
    currentImageFile = file
    currentEditor = editor
    rootPanel.contentComponent = internalComponent
    Disposer.register(disposableParent, editor)
  }

  private fun closeEditor(message: String) {
    label.text = message
    rootPanel.contentComponent = null
    currentImageFile = null
    currentEditor = null
  }

  private fun adjustImageZoom(zoomModel: ImageZoomModel) {
    if (!isAdvancedMode) {
      zoomModel.zoomFactor = 1.0 / scaleMultiplier
    } else {
      zoomModel.fitZoomToWindow()
    }
  }

  private fun getAdvancedModeToolPanelHeight(): Int {
    if (lastToolPanelHeight == 0) {
      currentEditor?.let { editor ->
        lastToolPanelHeight = editor.component.components[0].preferredSize.height
      }
    }
    return lastToolPanelHeight
  }

  companion object {
    private val LOGGER = Logger.getInstance(GraphicsPanel::class.java)
    private val isRetina = SystemInfo.isMac && UIUtil.isRetina()
    private val scaleMultiplier = if (!isRetina) 1 else 2
    private const val imageInsets = ImageComponent.IMAGE_INSETS
    private const val NO_GRAPHICS = "No graphics available"
    private const val GRAPHICS_COULD_NOT_BE_LOADED = "Graphics couldn't be loaded"

    fun calculateImageSizeForRegion(region: Dimension, topOffset: Int = 0): Dimension {
      return Dimension(region.width - imageInsets * 2, region.height - imageInsets * 2 - topOffset)
    }
  }
}

/**
 * Convert RGB floats to HSL floats.
 *
 * @param rgb input rgb floats from 0 to 1.
 * @param result output hsl floats, Hue is from 0 to 360, s and l are from 0 to 100.
 */
fun convertRGBtoHSL(rgb: FloatArray, result: FloatArray) { //  Get RGB values in the range 0 - 1
  val r = rgb[0]
  val g = rgb[1]
  val b = rgb[2]
  //	Minimum and Maximum RGB values are used in the HSL calculations
  val min = min(r, min(g, b))
  val max = max(r, max(g, b))
  //  Calculate the Hue
  var h = 0f
  if (max == min) h = 0f else if (max == r) h = (60 * (g - b) / (max - min) + 360) % 360 else if (max == g) h = 60 * (b - r) / (max - min) + 120 else if (max == b) h = 60 * (r - g) / (max - min) + 240
  //  Calculate the Luminance
  val l = (max + min) / 2
  //  Calculate the Saturation
  var s = 0f
  s = if (max == min) 0f else if (l <= .5f) (max - min) / (max + min) else (max - min) / (2 - max - min)
  result[0] = h
  result[1] = s * 100
  result[2] = l * 100
}


/**
 * Convert HSL values to an RGB value.
 *
 * @param hslFloats  hsl floats, Hue is from 0 to 360, s and l are from 0 to 100.
 * @param alpha  the alpha value between 0 - 1
 *
 * @returns the RGB Color object
 */
fun convertHCLtoRGB(hslFloats: FloatArray, alpha: Float): Int {
  var h = hslFloats[0]
  var s = hslFloats[1]
  var l = hslFloats[2]
  if (s < 0.0f || s > 100.0f) {
    val message = "Color parameter outside of expected range - Saturation"
    throw IllegalArgumentException(message)
  }
  if (l < 0.0f || l > 100.0f) {
    val message = "Color parameter outside of expected range - Luminance"
    throw IllegalArgumentException(message)
  }
  if (alpha < 0.0f || alpha > 1.0f) {
    val message = "Color parameter outside of expected range - Alpha"
    throw IllegalArgumentException(message)
  }
  //  Formula needs all values between 0 - 1.
  h = h % 360.0f
  h /= 360f
  s /= 100f
  l /= 100f
  var q = 0f
  q = if (l < 0.5) l * (1 + s) else l + s - s * l
  val p = 2 * l - q
  var r = max(0f, hueToRGB(p, q, h + 1.0f / 3.0f))
  var g = max(0f, hueToRGB(p, q, h))
  var b = max(0f, hueToRGB(p, q, h - 1.0f / 3.0f))
  r = min(r, 1.0f)
  g = min(g, 1.0f)
  b = min(b, 1.0f)
  return ((alpha * 255).toInt() shl 24) + ((r * 255).toInt() shl 16) + ((g * 255).toInt() shl 8) + (b * 255).toInt()
}

private fun hueToRGB(p: Float, q: Float, h: Float): Float {
  var h = h
  if (h < 0) h += 1f
  if (h > 1) h -= 1f
  if (6 * h < 1) {
    return p + (q - p) * 6 * h
  }
  if (2 * h < 1) {
    return q
  }
  return if (3 * h < 2) {
    p + (q - p) * 6 * (2.0f / 3.0f - h)
  } else p
}