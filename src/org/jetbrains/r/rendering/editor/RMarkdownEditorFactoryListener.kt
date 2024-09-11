package org.jetbrains.r.rendering.editor

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.notebooks.visualization.NotebookCellInlayManager
import com.intellij.notebooks.visualization.NotebookCellLinesProvider
import com.intellij.notebooks.visualization.NotebookEditorAppearanceProvider
import org.jetbrains.r.rmarkdown.RMarkdownVirtualFile
import org.jetbrains.r.visualization.RNotebookCellInlayManager
import org.jetbrains.r.visualization.RNotebookGutterLineMarkerManager
import org.jetbrains.r.visualization.inlays.RInlayDimensions

class RMarkdownEditorFactoryListener : EditorFactoryListener {

  override fun editorCreated(event: EditorFactoryEvent) {
    val editor = event.editor as? EditorImpl ?: return
    if (!RMarkdownVirtualFile.hasVirtualFile(editor)) return

    RInlayDimensions.init(editor)
    NotebookCellLinesProvider.install(editor)
    NotebookEditorAppearanceProvider.install(editor)

    val useStableUI: Boolean = true

    if (useStableUI) {
      RNotebookCellInlayManager.install(editor)
      RNotebookGutterLineMarkerManager.install(editor)
    } else {
      NotebookCellInlayManager.install(editor, shouldCheckInlayOffsets = false)
    }
  }
}