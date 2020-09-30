package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.Disposer
import org.intellij.datavis.r.inlays.ClipboardUtils
import org.intellij.datavis.r.inlays.InlayDimensions
import org.intellij.datavis.r.inlays.components.*
import org.intellij.datavis.r.inlays.runAsyncInlay
import org.intellij.datavis.r.ui.ToolbarUtil
import org.jetbrains.r.run.graphics.ui.RGraphicsExportDialog
import org.jetbrains.r.run.graphics.ui.RGraphicsPanelWrapper
import org.jetbrains.r.run.graphics.ui.RChunkGraphicsSettingsDialog
import org.jetbrains.r.run.graphics.ui.RGraphicsZoomDialog
import java.io.File
import javax.swing.SwingUtilities

class ChunkImageInlayOutput(private val parent: Disposable, editor: Editor, clearAction: () -> Unit) :
  InlayOutput(parent, editor, clearAction)
{
  private val wrapper = RGraphicsPanelWrapper(project, parent).apply {
    isVisible = false
  }

  private val path2Checks = mutableMapOf<String, Boolean>()
  private val manager = ChunkGraphicsManager(project)

  @Volatile
  private var globalResolution: Int? = null

  override val useDefaultSaveAction = false
  override val extraActions = createExtraActions()

  init {
    toolbarPane.dataComponent = wrapper.component
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
    wrapper.addImage(File(data), RGraphicsPanelWrapper.RescaleMode.LEFT_AS_IS, ::runAsyncInlay).onSuccess {
      SwingUtilities.invokeLater {
        val maxHeight = wrapper.maximumHeight ?: 0
        val maxWidth = wrapper.maximumWidth ?: 0
        val height = InlayDimensions.calculateInlayHeight(maxWidth, maxHeight, editor)
        onHeightCalculated?.invoke(height)
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
      RGraphicsExportDialog(project, parent, imagePath, wrapper.preferredImageSize).show()
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
      ToolbarUtil.createAnActionButton<ExportImageAction>(this::saveAs),
      ToolbarUtil.createAnActionButton<CopyImageToClipboardAction>(this::copyImageToClipboard),
      ToolbarUtil.createAnActionButton<ZoomImageAction>(this::canZoomImage, this::zoomImage),
      ToolbarUtil.createAnActionButton<ImageSettingsAction>(this::openImageSettings)
    )
  }

  private fun copyImageToClipboard() {
    wrapper.image?.let { image ->
      ClipboardUtils.copyImageToClipboard(image)
    }
  }

  private fun zoomImage() {
    TODO()
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
    val dialog = RChunkGraphicsSettingsDialog(initialSettings) { newSettings ->
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

  private fun getInitialSettings(isDarkModeEnabled: Boolean?) = RChunkGraphicsSettingsDialog.Settings(
    wrapper.isAutoResizeEnabled,
    isDarkModeEnabled,
    globalResolution,
    wrapper.localResolution
  )
}
