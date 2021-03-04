package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.Messages
import org.intellij.datavis.r.inlays.ClipboardUtils
import org.intellij.datavis.r.inlays.InlayDimensions
import org.intellij.datavis.r.inlays.components.*
import org.intellij.datavis.r.inlays.runAsyncInlay
import org.intellij.datavis.r.ui.ToolbarUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.run.graphics.RPlot
import org.jetbrains.r.run.graphics.RPlotUtil
import org.jetbrains.r.run.graphics.RSnapshot
import org.jetbrains.r.run.graphics.ui.*
import java.awt.Dimension
import java.io.File
import javax.swing.SwingUtilities

class ChunkImageInlayOutput(private val parent: Disposable, editor: Editor, clearAction: () -> Unit) :
  InlayOutput(parent, editor, clearAction)
{
  private val wrapper = RGraphicsPanelWrapper(project, parent).apply {
    isVisible = false
  }

  private val manager = ChunkGraphicsManager(project)

  override val useDefaultSaveAction = false
  override val extraActions = createExtraActions()

  private var overridesGlobal = false

  init {
    toolbarPane.dataComponent = wrapper.component
    wrapper.targetResolution = manager.globalResolution
    manager.addGlobalResolutionListener(parent) { newGlobalResolution ->
      if (!overridesGlobal) {
        wrapper.targetResolution = newGlobalResolution
      }
    }
    wrapper.isStandalone = manager.isStandalone
    manager.addStandaloneListener(parent) { newStandalone ->
      if (!overridesGlobal) {
        wrapper.isStandalone = newStandalone
      }
    }
  }

  override fun addToolbar() {
    super.addToolbar()
    wrapper.overlayComponent = toolbarPane.toolbarComponent
  }

  override fun addData(data: String, type: String) {
    runAsyncInlay {
      val maxSize = addGraphics(File(data))
      SwingUtilities.invokeLater {
        val height = maxSize?.let { calculateHeight(it) }
        onHeightCalculated?.invoke(height ?: InlayDimensions.defaultHeight)
      }
    }
  }

  private fun calculateHeight(maxSize: Dimension): Int {
    return InlayDimensions.calculateInlayHeight(maxSize.width, maxSize.height, editor)
  }

  private fun addGraphics(file: File): Dimension? {
    val snapshot = RSnapshot.from(file)
    return if (snapshot != null) {
      val plot = findPlotFor(snapshot)
      wrapper.addGraphics(snapshot, plot)
      null
    } else {
      wrapper.addImage(file)
      wrapper.maximumSize
    }
  }

  private fun findPlotFor(snapshot: RSnapshot): RPlot? {
    return RPlotUtil.readFrom(snapshot.file.parentFile, snapshot.number)?.let { plot ->
      RPlotUtil.convert(plot, snapshot.number)
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
    if (wrapper.hasGraphics) {
      if (wrapper.isStandalone) {
        wrapper.plot?.let { plot ->
          RGraphicsExportDialog.show(project, parent, plot, wrapper.preferredImageSize, wrapper.localResolution)
        }
      } else {
        wrapper.snapshot?.let { snapshot ->
          RGraphicsExportDialog.show(project, parent, snapshot, wrapper.preferredImageSize)
        }
      }
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
    if (wrapper.isStandalone) {
      wrapper.plot?.let { plot ->
        RGraphicsZoomDialog.show(project, parent, plot, wrapper.localResolution)
      }
    } else {
      wrapper.snapshot?.let { snapshot ->
        RGraphicsZoomDialog.show(project, parent, snapshot)
      }
    }
  }

  private fun canZoomImage(): Boolean {
    return wrapper.hasGraphics
  }

  private fun openImageSettings() {
    val isDarkEditor = EditorColorsManager.getInstance().isDarkEditor
    val isDarkModeEnabled = if (isDarkEditor) manager.isDarkModeEnabled else null
    val initialSettings = getInitialSettings(isDarkModeEnabled)
    val dialog = RChunkGraphicsSettingsDialog(initialSettings) { newSettings ->
      wrapper.isAutoResizeEnabled = newSettings.isAutoResizedEnabled
      overridesGlobal = newSettings.overridesGlobal
      wrapper.targetResolution = if (overridesGlobal) newSettings.localResolution else newSettings.globalResolution
      val newStandalone = if (overridesGlobal) newSettings.localStandalone else newSettings.globalStandalone
      wrapper.isStandalone = newStandalone
      if (newStandalone && wrapper.isStandalone != newStandalone) {
        Messages.showErrorDialog(project, SWITCH_ERROR_DESCRIPTION, SWITCH_ERROR_TITLE)
      }
      manager.isStandalone = newSettings.globalStandalone
      newSettings.isDarkModeEnabled?.let { newDarkModeEnabled ->
        manager.isDarkModeEnabled = newDarkModeEnabled
      }
      newSettings.globalResolution?.let { newGlobalResolution ->
        manager.globalResolution = newGlobalResolution
      }
    }
    dialog.show()
  }

  private fun getInitialSettings(isDarkModeEnabled: Boolean?) = RChunkGraphicsSettingsDialog.Settings(
    wrapper.isAutoResizeEnabled,
    isDarkModeEnabled,
    overridesGlobal,
    manager.globalResolution,
    wrapper.localResolution,
    manager.isStandalone,
    wrapper.isStandalone
  )

  companion object {
    private val SWITCH_ERROR_TITLE = RBundle.message("plot.viewer.cannot.switch.to.standalone.title")
    private val SWITCH_ERROR_DESCRIPTION = RBundle.message("plot.viewer.cannot.switch.to.standalone.description")
  }
}
