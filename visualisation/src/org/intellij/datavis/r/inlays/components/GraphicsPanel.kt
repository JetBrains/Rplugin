/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.BinaryLightVirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.messages.Topic
import com.intellij.util.ui.UIUtil
import org.intellij.datavis.r.VisualizationBundle
import org.intellij.datavis.r.ui.UiCustomizer
import org.intellij.images.editor.ImageEditor
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.ui.ImageComponent
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JLabel

val CHANGE_DARK_MODE_TOPIC = Topic.create("Graphics Panel Dark Mode Topic", DarkModeNotifier::class.java)

interface DarkModeNotifier {
  fun onDarkModeChanged(isEnabled: Boolean)
}

class GraphicsPanel(private val project: Project, private val disposableParent: Disposable) {
  private val graphicsManager = GraphicsManager.getInstance(project)
  private val label = JLabel(NO_GRAPHICS, JLabel.CENTER)
  private val rootPanel = EmptyComponentPanel(label)

  private val loadingPanel = JBLoadingPanel(BorderLayout(), disposableParent).apply {
    startLoading()
  }

  @Volatile
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

  val image: BufferedImage?
    get() = currentEditor?.document?.value

  val imageSize: Dimension?
    get() = image?.let { image ->
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
  var overlayComponent: JComponent? = null

  @Volatile
  private var darkMode = graphicsManager?.isDarkModeEnabled ?: true

  init {
    val connect = project.messageBus.connect()
    Disposer.register(disposableParent, connect)
    connect.subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
      currentFile?.let { showImage(it) }
    })
    connect.subscribe(CHANGE_DARK_MODE_TOPIC, object : DarkModeNotifier {
      override fun onDarkModeChanged(isEnabled: Boolean) {
        if (darkMode != isEnabled) {
          darkMode = isEnabled
          currentFile?.let { showImage(it) }
        }
      }
    })
  }

  fun showImage(imageFile: File) {
    if (!tryShowImage(imageFile)) {
      closeEditor(GRAPHICS_COULD_NOT_BE_LOADED)
    }
  }

  fun showImageBase64(data: String) {
    isAdvancedMode = true
    invokeAndWaitIfNeeded {
      if (Disposer.isDisposed(disposableParent)) {
        return@invokeAndWaitIfNeeded
      }
      openEditor(BinaryLightVirtualFile("image", Base64.getMimeDecoder().decode(data)))
    }
  }

  fun showSvgImage(data: String) {
    isAdvancedMode = true
    invokeAndWaitIfNeeded {
      if (Disposer.isDisposed(disposableParent)) {
        return@invokeAndWaitIfNeeded
      }
      openEditor(LightVirtualFile("image.svg", data))
    }
  }

  fun showMessage(message: String) {
    closeEditor(message)
  }

  fun showLoadingMessage(message: String? = null) {
    closeEditor("")
    rootPanel.contentComponent = loadingPanel
    loadingPanel.setLoadingText(message ?: LOADING_MESSAGE)
  }

  fun reset() {
    closeEditor(NO_GRAPHICS)
  }

  private fun tryShowImage(imageFile: File): Boolean {
    try {
      if (!imageFile.exists()) return false
      val content = readImageContent(imageFile)
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

  private fun readImageContent(imageFile: File): ByteArray {
    val bytes = imageFile.readBytes()
    val editorColorsManager = EditorColorsManager.getInstance()
    return if (editorColorsManager.isDarkEditor && darkMode && imageFile.isInvertible) {
      tryCreateInvertedImage(bytes, editorColorsManager.globalScheme)
    } else {
      bytes
    }
  }

  private val File.isInvertible: Boolean
    get() = graphicsManager?.canRescale(absolutePath) ?: false

  private fun tryCreateInvertedImage(content: ByteArray, globalScheme: EditorColorsScheme): ByteArray {
    return try {
      createInvertedImage(content, globalScheme)
    } catch (e: Exception) {
      LOGGER.warn("Failed to invert image", e)
      content
    }
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

    val bufferedImage = ImageIO.read(ByteArrayInputStream(content)) ?: return content
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
    ByteArrayOutputStream().use { outputStream ->
      ImageIO.write(bufferedImage, "png", outputStream)
      outputStream.flush()
      return outputStream.toByteArray()
    }
  }

  private fun openEditor(file: VirtualFile) {
    runInEdt {
      closeEditor(NO_GRAPHICS)
      val editor = UiCustomizer.instance.createImageEditor(project, file, this)
      adjustImageZoom(editor.zoomModel)
      removeImageInfoLabelAndActionToolBar(editor)
      currentImageFile = file
      currentEditor = editor
      withOverlayComponentPreserved {
        rootPanel.contentComponent = internalComponent
      }
      Disposer.register(disposableParent, editor)
    }
  }

  private fun withOverlayComponentPreserved(task: () -> Unit) {
    val component = overlayComponent
    if (component != null && component.isVisible) {
      component.isVisible = false
      task()
      component.isVisible = true
    } else {
      task()
    }
  }

  private fun closeEditor(message: String) {
    label.text = message
    currentEditor?.let { Disposer.dispose(it) }
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

  private fun removeImageInfoLabelAndActionToolBar(editor: ImageEditor) {
    val labels = UIUtil.findComponentsOfType(editor.component, JLabel::class.java)
    for (label in labels) {
      label.text?.let { text ->
        if (text.contains("color", ignoreCase = true) && !text.contains("<html>")) {
          label.parent.remove(label)
        }
      }
    }
    UIUtil.findComponentOfType(editor.component, ActionToolbarImpl::class.java)?.let { toolbar ->
      toolbar.parent.remove(toolbar)
    }
  }

  companion object {
    private val LOGGER = Logger.getInstance(GraphicsPanel::class.java)
    private val isRetina = SystemInfo.isMac && UIUtil.isRetina()
    private val scaleMultiplier = if (!isRetina) 1 else 2
    private const val imageInsets = ImageComponent.IMAGE_INSETS
    private const val NO_GRAPHICS = "No graphics available"
    private const val GRAPHICS_COULD_NOT_BE_LOADED = "Graphics couldn't be loaded"

    private val LOADING_MESSAGE = VisualizationBundle.message("graphics.loading")

    fun calculateImageSizeForRegion(region: Dimension, topOffset: Int = 0): Dimension {
      return Dimension(region.width - imageInsets * 2, region.height - imageInsets * 2 - topOffset)
    }

    fun calculateRegionForImageSize(imageSize: Dimension, topOffset: Int = 0): Dimension {
      return Dimension(imageSize.width + imageInsets * 2, imageSize.height + imageInsets * 2 + topOffset)
    }
  }
}