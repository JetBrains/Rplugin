package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.InlayOutput
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.InlayOutputProvider

class ChunkInlayOutputProvider : InlayOutputProvider {
  override fun acceptType(type: String): Boolean {
    return type == "IMG"
  }

  override fun create(parent: Disposable, editor: Editor, clearAction: () -> Unit): InlayOutput {
    return ChunkImageInlayOutput(parent, editor, clearAction)
  }
}
