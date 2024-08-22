package org.jetbrains.r.rendering.editor

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.impl.EditorImpl
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.plugins.notebooks.visualization.NotebookCellInlayManager
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLinesProvider
import org.jetbrains.plugins.notebooks.visualization.NotebookEditorAppearanceProvider
import org.jetbrains.r.rmarkdown.RMarkdownVirtualFile
import org.jetbrains.r.visualization.inlays.RInlayDimensions

class RMarkdownEditorFactoryListener(private val coroutineScope: CoroutineScope) : EditorFactoryListener {

  override fun editorCreated(event: EditorFactoryEvent) {
    val editor = event.editor as? EditorImpl ?: return
    if (!RMarkdownVirtualFile.hasVirtualFile(editor)) return

    RInlayDimensions.init(editor)
    NotebookCellLinesProvider.install(editor)
    NotebookEditorAppearanceProvider.install(editor)
    NotebookCellInlayManager.install(editor, shouldCheckInlayOffsets = false, parentScope = coroutineScope)
  }
}