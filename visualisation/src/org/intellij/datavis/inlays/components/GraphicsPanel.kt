/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.BinaryLightVirtualFile
import com.intellij.util.ui.UIUtil
import org.intellij.images.editor.ImageEditor
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.editor.impl.ImageEditorImpl
import org.intellij.images.ui.ImageComponent
import java.awt.Dimension
import java.io.File
import javax.swing.JComponent
import javax.swing.JLabel

class GraphicsPanel(private val project: Project, private val disposableParent: Disposable) {
  private val label = JLabel(NO_GRAPHICS, JLabel.CENTER)
  private val rootPanel = EmptyComponentPanel(label)

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

  val imageInsets = ImageComponent.IMAGE_INSETS

  val imageSize: Dimension?
    get() = currentEditor?.document?.value?.let { image ->
      Dimension(image.width / scaleMultiplier, image.height / scaleMultiplier)
    }

  val imageComponentSize: Dimension
    get() {
      val insets = imageInsets
      val panelDimension = component.size
      return Dimension(panelDimension.width - insets * 2, panelDimension.height - toolPanelHeight - insets * 2)
    }

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

  fun showImage(imageFile: File) {
    try {
      openEditor(imageFile)
    } catch (e: Exception) {
      closeEditor(GRAPHICS_COULD_NOT_BE_LOADED)
      LOGGER.error("Failed to load graphics", e)
    }
  }

  fun showMessage(message: String) {
    closeEditor(message)
  }

  fun reset() {
    closeEditor(NO_GRAPHICS)
  }

  private fun openEditor(imageFile: File) {
    val content = imageFile.readBytes()
    openEditor(BinaryLightVirtualFile(imageFile.name, content))
  }

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
    private const val NO_GRAPHICS = "No graphics available"
    private const val GRAPHICS_COULD_NOT_BE_LOADED = "Graphics couldn't be loaded"
  }
}