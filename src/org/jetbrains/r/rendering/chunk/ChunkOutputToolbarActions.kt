package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.InlayOutput


sealed class ChunkImageInlayOutputAction() : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    val output = getOutput(e)
    e.presentation.isEnabledAndVisible = (output != null && isEnabled(output))
  }

  override fun actionPerformed(e: AnActionEvent) {
    getOutput(e)?.let(::actionPerformed)
  }

  protected open fun isEnabled(output: ChunkImageInlayOutput): Boolean = true

  protected abstract fun actionPerformed(output: ChunkImageInlayOutput)

  private fun getOutput(e: AnActionEvent): ChunkImageInlayOutput? =
    InlayOutput.getToolbarPaneOrNull(e)?.inlayOutput as? ChunkImageInlayOutput

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.EDT
}


internal class ExportImageAction private constructor() : ChunkImageInlayOutputAction() {
  override fun actionPerformed(output: ChunkImageInlayOutput) {
    output.saveAs()
  }

  companion object {
    val ID = "org.jetbrains.r.rendering.chunk.ExportImageAction"
  }
}


internal class ZoomImageAction private constructor() : ChunkImageInlayOutputAction() {
  override fun isEnabled(output: ChunkImageInlayOutput): Boolean =
    output.canZoomImage()

  override fun actionPerformed(output: ChunkImageInlayOutput) {
    output.zoomImage()
  }

  companion object {
    val ID = "org.jetbrains.r.rendering.chunk.ZoomImageAction"
  }
}


internal class ImageSettingsAction private constructor() : ChunkImageInlayOutputAction() {
  override fun actionPerformed(output: ChunkImageInlayOutput) {
    output.openImageSettings()
  }

  companion object {
    val ID = "org.jetbrains.r.rendering.chunk.ImageSettingsAction"
  }
}
