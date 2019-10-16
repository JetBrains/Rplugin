/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.images.editor.ImageEditor
import org.intellij.images.editor.impl.ImageEditorImpl
import java.io.File
import javax.swing.JLabel

class GraphicsPanel(private val project: Project) {
  private val label = JLabel(NO_GRAPHICS, JLabel.CENTER)
  private val uiParent: Disposable? = Disposer.get("ui")
  private val rootPanel = EmptyComponentPanel(label)

  private var currentEditor: ImageEditor? = null
  private var lastToolPanelHeight: Int = 0

  val component = rootPanel.component

  fun refresh(snapshot: File) {
    try {
      showImage(snapshot)
    } catch (e: Exception) {
      closeEditor(GRAPHICS_COULD_NOT_BE_LOADED)
      LOGGER.error("Failed to load graphics", e)
    }
  }

  fun showMessage(message: String) {
    closeEditor(message)
  }

  fun getImageHeight(): Int = currentEditor?.document?.value?.height ?: 0  // TODO [mine]: Have I break anything by removing delta?

  fun getToolPanelHeight(): Int? {
    if (lastToolPanelHeight == 0) {
      currentEditor?.let { editor ->
        lastToolPanelHeight = editor.component.components[0].preferredSize.height
      }
    }
    return lastToolPanelHeight
  }

  fun reset() {
    closeEditor(NO_GRAPHICS)
  }

  private fun showImage(snapshot: File) {
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(snapshot)
    if (virtualFile != null) {
      closeEditor(null)
      openEditor(virtualFile)
    } else {
      LOGGER.warn("Cannot get virtual file for snapshot: $snapshot")
    }
  }

  private fun openEditor(file: VirtualFile) {
    val editor: ImageEditor = ImageEditorImpl(project, file)  // Note: explicit cast prevents compiler warnings
    rootPanel.contentComponent = editor.component
    currentEditor = editor
    if (uiParent != null) {
      Disposer.register(uiParent, editor)
    }
  }

  private fun closeEditor(text: String?) {
    label.text = text
    rootPanel.contentComponent = null
    currentEditor = null
  }

  companion object {
    private val LOGGER = Logger.getInstance(GraphicsPanel::class.java)
    private const val NO_GRAPHICS = "No graphics available"
    private const val GRAPHICS_COULD_NOT_BE_LOADED = "Graphics couldn't be loaded"
  }
}