package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.UIUtil
import org.intellij.datavis.r.inlays.ClipboardUtils
import org.intellij.datavis.r.inlays.components.*
import org.intellij.datavis.r.inlays.runAsyncInlay
import org.intellij.datavis.r.ui.ToolbarUtil
import org.jetbrains.r.run.graphics.ui.GraphicsExportDialog
import org.jetbrains.r.run.graphics.ui.GraphicsPanelWrapper
import org.jetbrains.r.run.graphics.ui.GraphicsSettingsDialog
import org.jetbrains.r.run.graphics.ui.GraphicsZoomDialog
import java.io.File
import javax.swing.SwingUtilities

class ChunkImageInlayOutput(private val parent: Disposable, editor: Editor, clearAction: () -> Unit) :
  InlayOutput(parent, editor, clearAction)
{
  private val wrapper = GraphicsPanelWrapper(project, parent).apply {
    isVisible = false
  }

  private val path2Checks = mutableMapOf<String, Boolean>()
  private val manager = ChunkGraphicsManager(project)

  @Volatile
  private var globalResolution: Int? = null

  override val useDefaultSaveAction = false
  override val extraActions = createExtraActions()

  init {
    toolbarPane.centralComponent = wrapper.component
    globalResolution = manager.globalResolution
    val connection = manager.addGlobalResolutionListener { newGlobalResolution ->
      wrapper.targetResolution = newGlobalResolution
      globalResolution = newGlobalResolution
    }
    Disposer.register(parent, connection)
  }

  override fun addToolbar() {
    super.addToolbar()
    wrapper.overlayComponent = toolbarPane.toolbarComponent
  }

  override fun addData(data: String, type: String) {
    wrapper.isAutoResizeEnabled = false
    wrapper.addImage(File(data), GraphicsPanelWrapper.RescaleMode.LEFT_AS_IS, ::runAsyncInlay).onSuccess {
      SwingUtilities.invokeLater {
        val maxHeight = wrapper.maximumHeight ?: 0
        val scaleMultiplier = if (UIUtil.isRetina()) 2 else 1
        val maxWidth = wrapper.maximumWidth ?: 0
        val editorWidth = editor.contentComponent.width
        if (maxWidth * scaleMultiplier <= editorWidth) {
          onHeightCalculated?.invoke(maxHeight * scaleMultiplier)
        } else {
          onHeightCalculated?.invoke(maxHeight * editorWidth / maxWidth)
        }
        wrapper.isAutoResizeEnabled = manager.canRescale(data)
      }
    }
  }

  override fun clear() {
  }

  override fun scrollToTop() {
  }

  override fun getCollapsedDescription(): String {
    return "foo"
  }

  override fun saveAs() {
    val imagePath = wrapper.imagePath
    if (imagePath != null && manager.canRescale(imagePath)) {
      GraphicsExportDialog(project, parent, imagePath, wrapper.preferredImageSize).show()
    } else {
      wrapper.image?.let { image ->
        InlayOutputUtil.saveImageWithFileChooser(project, image)
      }
    }
  }

  override fun acceptType(type: String): Boolean {
    return type == "IMG"
  }

  override fun onViewportChange(isInViewport: Boolean) {
    wrapper.isVisible = isInViewport
  }

  private fun createExtraActions(): List<AnAction> {
    return listOf(
      ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.ExportImageAction", this::saveAs),
      ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.CopyImageToClipboardAction", this::copyImageToClipboard),
      ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.ZoomImageAction", this::canZoomImage, this::zoomImage),
      ToolbarUtil.createAnActionButton("org.intellij.datavis.r.inlays.components.ImageSettingsAction", this::openImageSettings)
    )
  }

  private fun copyImageToClipboard() {
    wrapper.image?.let { image ->
      ClipboardUtils.copyImageToClipboard(image)
    }
  }

  private fun zoomImage() {
    wrapper.imagePath?.let { path ->
      GraphicsZoomDialog(project, parent, path).show()
    }
  }

  private fun canZoomImage(): Boolean {
    return canZoomImageOrNull() == true
  }

  private fun canZoomImageOrNull(): Boolean? {
    return wrapper.imagePath?.let { path ->
      path2Checks.getOrPut(path) {  // Note: speedup FS operations caused by `canRescale(path)`
        manager.canRescale(path)
      }
    }
  }

  private fun openImageSettings() {
    val isDarkEditor = EditorColorsManager.getInstance().isDarkEditor
    val isDarkModeEnabled = if (isDarkEditor) manager.isDarkModeEnabled else null
    val initialSettings = getInitialSettings(isDarkModeEnabled)
    val dialog = GraphicsSettingsDialog(initialSettings) { newSettings ->
      wrapper.isAutoResizeEnabled = newSettings.isAutoResizedEnabled
      wrapper.targetResolution = newSettings.localResolution
      newSettings.isDarkModeEnabled?.let { newDarkModeEnabled ->
        if (newDarkModeEnabled != isDarkModeEnabled) {
          manager.isDarkModeEnabled = newDarkModeEnabled
        }
      }
      newSettings.globalResolution?.let { newGlobalResolution ->
        if (newGlobalResolution != globalResolution) {
          // Note: no need to set `this.globalResolution` here: it will be changed automatically by a listener below
          manager.globalResolution = newGlobalResolution
        }
      }
    }
    dialog.show()
  }

  private fun getInitialSettings(isDarkModeEnabled: Boolean?) = GraphicsSettingsDialog.Settings(
    wrapper.isAutoResizeEnabled,
    isDarkModeEnabled,
    globalResolution,
    wrapper.localResolution
  )
}
