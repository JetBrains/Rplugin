package org.jetbrains.r.rendering.editor

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.visualization.NotebookCellInlayManager
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLinesProvider
import org.jetbrains.plugins.notebooks.visualization.NotebookEditorAppearanceProvider
import org.jetbrains.plugins.notebooks.visualization.r.inlays.InlayDimensions
import org.jetbrains.r.rmarkdown.RMarkdownVirtualFile

class RMarkdownEditorFactoryListener : EditorFactoryListener {

  override fun editorCreated(event: EditorFactoryEvent) {
    val editor = event.editor as? EditorImpl ?: return
    if (!RMarkdownVirtualFile.hasVirtualFile(editor)) return

    InlayDimensions.init(editor)
    NotebookCellLinesProvider.install(editor)
    NotebookEditorAppearanceProvider.install(editor)
    NotebookCellInlayManager.install(editor, shouldCheckInlayOffsets = false)
  }
}