/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays.components

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.visualization.inlays.components.CHANGE_DARK_MODE_TOPIC
import com.intellij.r.psi.visualization.inlays.components.DarkModeNotifier
import com.intellij.r.psi.visualization.inlays.components.EmptyComponentPanel
import com.intellij.r.psi.visualization.inlays.components.RImageInverter
import com.intellij.testFramework.BinaryLightVirtualFile
import com.intellij.ui.JreHiDpiUtil
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.UIUtil
import org.intellij.images.editor.ImageEditor
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.editor.impl.ImageEditorImpl
import org.intellij.images.ui.ImageComponent
import org.jetbrains.annotations.Nls
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.rendering.chunk.ChunkGraphicsManager
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readBytes

class GraphicsPanel(private val project: Project, private val disposableParent: Disposable) {
  private val graphicsManager = ChunkGraphicsManager(project)
  private val label = JLabel(RBundle.message("graphics.not.available"), JLabel.CENTER)
  private val rootPanel = EmptyComponentPanel(label)

  private val loadingPanel = JBLoadingPanel(BorderLayout(), disposableParent).apply {
    startLoading()
  }

  @Volatile
  private var currentFile: Path? = null

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
  private var darkMode = graphicsManager.isDarkModeEnabled

  init {
    val connect = project.messageBus.connect(disposableParent)
    connect.subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
      currentFile?.let { showImageAsync(it) }
    })
    connect.subscribe(CHANGE_DARK_MODE_TOPIC, object : DarkModeNotifier {
      override fun onDarkModeChanged(isEnabled: Boolean) {
        if (darkMode != isEnabled) {
          darkMode = isEnabled
          currentFile?.let { showImageAsync(it) }
        }
      }
    })
  }

  private fun showImageAsync(imageFile: Path) {
    runAsync {
      showImage(imageFile)
    }
  }

  fun showImage(imageFile: Path) {
    if (!tryShowImage(imageFile)) {
      closeEditor(RBundle.message("graphics.could.not.be.loaded"))
    }
  }

  fun showBufferedImage(image: BufferedImage) {
    val content = ByteArrayOutputStream().use { outputStream ->
      ImageIO.write(image, "png", outputStream)
      outputStream.flush()
      outputStream.toByteArray()
    }
    invokeAndWaitIfNeeded {
      if (Disposer.isDisposed(disposableParent)) {
        return@invokeAndWaitIfNeeded
      }
      currentFile = null
      openEditor(BinaryLightVirtualFile("image.png", content))
    }
  }

  fun showImageBase64(data: String) {
    isAdvancedMode = true
    ApplicationManager.getApplication().invokeAndWait {
      if (Disposer.isDisposed(disposableParent)) {
        return@invokeAndWait
      }
      currentFile = null
      openEditor(BinaryLightVirtualFile("image", Base64.getMimeDecoder().decode(data)))
    }
  }

  fun showMessage(@Nls message: String) {
    reset(message)
  }

  fun showLoadingMessage(@Nls message: String? = null) {
    reset()
    rootPanel.contentComponent = loadingPanel
    loadingPanel.setLoadingText(message ?: RBundle.message("graphics.loading"))
  }

  fun showMessageWithLink(@Nls message: String, @Nls linkText: String, linkAction: () -> Unit) {
    reset()
    rootPanel.contentComponent = createInfoPanelWithLink(message, linkText, linkAction)
  }

  fun reset(@Nls message: String? = null) {
    currentFile = null
    closeEditor(message ?: RBundle.message("graphics.not.available"))
  }

  private fun tryShowImage(imageFile: Path): Boolean {
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

  private fun readImageContent(imageFile: Path): ByteArray {
    val bytes = imageFile.readBytes()
    val editorColorsManager = EditorColorsManager.getInstance()
    return if (editorColorsManager.isDarkEditor && darkMode && graphicsManager.isInvertible(imageFile)) {
      tryCreateInvertedImage(bytes, editorColorsManager.globalScheme)
    } else {
      bytes
    }
  }

  private fun tryCreateInvertedImage(content: ByteArray, globalScheme: EditorColorsScheme): ByteArray {
    return try {
      createInvertedImage(content, globalScheme)
    } catch (e: Exception) {
      LOGGER.warn("Failed to invert image", e)
      content
    }
  }

  private fun createInvertedImage(content: ByteArray, globalScheme: EditorColorsScheme): ByteArray {
    val inverter = RImageInverter(globalScheme.defaultForeground, globalScheme.defaultBackground)
    return inverter.invert(content)
  }

  private fun openEditor(file: VirtualFile) {
    runInEdt {
      if (Disposer.isDisposed(disposableParent)) {
        return@runInEdt
      }
      closeEditor(RBundle.message("graphics.not.available"))
      val editor = createImageEditor(project, file, this)
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

  private fun closeEditor(@Nls message: String) {
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
        lastToolPanelHeight = editor.component.components.firstOrNull()?.preferredSize?.height ?: 0
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

  private fun createInfoPanelWithLink(@Nls message: String, @Nls linkText: String, linkAction: () -> Unit): JPanel {
    return JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      add(Box.createVerticalGlue())
      val infoLabel = JLabel(message).apply {
        alignmentX = Component.CENTER_ALIGNMENT
      }
      add(infoLabel)
      add(Box.createRigidArea(Dimension(0, JBUIScale.scale(6))))
      val linkLabel = ActionLink(linkText) { linkAction() }.apply {
        alignmentX = Component.CENTER_ALIGNMENT
      }
      add(linkLabel)
      add(Box.createVerticalGlue())
    }
  }

  companion object {
    private val LOGGER = Logger.getInstance(GraphicsPanel::class.java)
    private val isHiDpi = JreHiDpiUtil.isJreHiDPI(null as GraphicsConfiguration?) || UIUtil.isRetina()
    private val scaleMultiplier = if (!isHiDpi) 1 else 2
    private const val imageInsets = ImageComponent.IMAGE_INSETS

    fun calculateImageSizeForRegion(region: Dimension, topOffset: Int = 0): Dimension {
      return Dimension(region.width - imageInsets * 2, region.height - imageInsets * 2 - topOffset)
    }

    fun calculateRegionForImageSize(imageSize: Dimension, topOffset: Int = 0): Dimension {
      return Dimension(imageSize.width + imageInsets * 2, imageSize.height + imageInsets * 2 + topOffset)
    }

    private fun createImageEditor(project: Project, file: VirtualFile, graphicsPanel: GraphicsPanel): ImageEditor {
      val imageEditor = ImageEditorImpl(project, file, true, false)
      val parentEditor = DataManager.getInstance().getDataContext(graphicsPanel.component).getData(CommonDataKeys.EDITOR)
      if (parentEditor != null) {
        imageEditor.setEditorBackground(parentEditor.colorsScheme.defaultBackground)
      }
      return imageEditor
    }
  }
}