package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.Messages
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RPluginCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nls
import org.jetbrains.r.run.graphics.RPlot
import org.jetbrains.r.run.graphics.RPlotUtil
import org.jetbrains.r.run.graphics.RSnapshot
import org.jetbrains.r.run.graphics.ui.RChunkGraphicsSettingsDialog
import org.jetbrains.r.run.graphics.ui.RGraphicsExportDialog
import org.jetbrains.r.run.graphics.ui.RGraphicsPanelWrapper
import org.jetbrains.r.run.graphics.ui.RGraphicsZoomDialog
import org.jetbrains.r.visualization.inlays.InlayExecutor
import org.jetbrains.r.visualization.inlays.InlayOutputData
import org.jetbrains.r.visualization.inlays.RClipboardUtils
import org.jetbrains.r.visualization.inlays.RInlayDimensions
import org.jetbrains.r.visualization.inlays.components.CopyImageToClipboardAction
import org.jetbrains.r.visualization.inlays.components.InlayOutput
import org.jetbrains.r.visualization.inlays.components.InlayOutputUtil
import java.awt.Dimension
import java.nio.file.Path

class ChunkImageInlayOutput(private val parent: Disposable, editor: Editor) :
  InlayOutput(editor, loadActions()), InlayOutput.WithCopyImageToClipboard, InlayOutput.WithSaveAs {

  private val wrapper = RGraphicsPanelWrapper(project, parent).apply {
    isVisible = false
  }

  private val manager = ChunkGraphicsManager(project)

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

  fun addData(data: InlayOutputData.Image) {
    RPluginCoroutineScope.getScope(project).launch(ModalityState.defaultModalityState().asContextElement() + Dispatchers.IO) {
      val maxSize = InlayExecutor.mutex.withLock {
        addGraphics(data.path)
      }
      withContext(Dispatchers.EDT) {
        val height = maxSize?.let { calculateHeight(it) }
        onHeightCalculated?.invoke(height ?: RInlayDimensions.defaultHeight)
      }
    }
  }

  private fun calculateHeight(maxSize: Dimension): Int {
    return RInlayDimensions.calculateInlayHeight(maxSize.width, maxSize.height, editor)
  }

  private fun addGraphics(file: Path): Dimension? {
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
    return RPlotUtil.readFrom(snapshot.file.parent, snapshot.number)?.let { plot ->
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

  override fun copyImageToClipboard() {
    wrapper.image?.let { image ->
      RClipboardUtils.copyImageToClipboard(image)
    }
  }

  /** called by [ZoomImageAction] */
  fun zoomImage() {
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

  /** called by [ZoomImageAction] */
  fun canZoomImage(): Boolean {
    return wrapper.hasGraphics
  }

  /** called by [ImageSettingsAction] */
  fun openImageSettings() {
    val isDarkEditor = EditorColorsManager.getInstance().isDarkEditor
    val isDarkModeEnabled = if (isDarkEditor) manager.isDarkModeEnabled else null
    val initialSettings = getInitialSettings(isDarkModeEnabled)
    val dialog = RChunkGraphicsSettingsDialog(initialSettings) { newSettings ->
      wrapper.isAutoResizeEnabled = newSettings.isAutoResizedEnabled
      overridesGlobal = newSettings.overridesGlobal
      wrapper.targetResolution = if (overridesGlobal) newSettings.localResolution else newSettings.globalResolution
      val newStandalone = if (overridesGlobal) newSettings.localStandalone else newSettings.globalStandalone
      wrapper.isStandalone = newStandalone
      if (newStandalone && !wrapper.isStandalone) {
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
    @Nls
    private val SWITCH_ERROR_TITLE = RBundle.message("plot.viewer.cannot.switch.to.standalone.title")
    @Nls
    private val SWITCH_ERROR_DESCRIPTION = RBundle.message("plot.viewer.cannot.switch.to.standalone.description")

    private fun loadActions() = loadActions(
      ExportImageAction.ID,
      CopyImageToClipboardAction.ID,
      ZoomImageAction.ID,
      ImageSettingsAction.ID,
    )
  }
}
