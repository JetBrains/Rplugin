package org.jetbrains.r.visualization.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.r.RPluginCoroutineScope
import org.jetbrains.r.visualization.inlays.InlayExecutor
import org.jetbrains.r.visualization.inlays.RClipboardUtils
import org.jetbrains.r.visualization.inlays.RInlayDimensions
import java.io.File
import javax.swing.JComponent

class InlayOutputImg(parent: Disposable, editor: Editor)
  : InlayOutput(editor, loadActions(CopyImageToClipboardAction.ID, SaveOutputAction.ID)), InlayOutput.WithCopyImageToClipboard, InlayOutput.WithSaveAs {
  private val graphicsPanel = GraphicsPanel(project, parent).apply {
    isAdvancedMode = true
  }

  override val isFullWidth = false

  init {
    toolbarPane.dataComponent = graphicsPanel.component
  }

  override fun addToolbar() {
    super.addToolbar()
    graphicsPanel.overlayComponent = toolbarPane.toolbarComponent
  }

  override fun addData(data: String, type: String) {
    RPluginCoroutineScope.getScope(project).launch( ModalityState.defaultModalityState().asContextElement() + Dispatchers.IO) {
      InlayExecutor.mutex.withLock {
        when (type) {
          "IMGBase64" -> graphicsPanel.showImageBase64(data)
          "IMGSVG" -> graphicsPanel.showSvgImage(data)
          "IMG" -> graphicsPanel.showImage(File(data))
          else -> Unit
        }
      }

      withContext(Dispatchers.EDT) {
        val maxHeight = graphicsPanel.maximumSize?.height ?: 0
        val maxWidth = graphicsPanel.maximumSize?.width ?: 0
        val height = RInlayDimensions.calculateInlayHeight(maxWidth, maxHeight, editor)
        onHeightCalculated?.invoke(height)
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
    graphicsPanel.image?.let { image ->
      InlayOutputUtil.saveImageWithFileChooser(project, image)
    }
  }

  override fun acceptType(type: String): Boolean {
    return type == "IMG" || type == "IMGBase64" || type == "IMGSVG"
  }

  override fun copyImageToClipboard() {
    graphicsPanel.image?.let { image ->
      RClipboardUtils.copyImageToClipboard(image)
    }
  }

  override fun toolbarPaneChanged(component: JComponent?) {
    component?.background = editor.colorsScheme.defaultBackground
  }
}
