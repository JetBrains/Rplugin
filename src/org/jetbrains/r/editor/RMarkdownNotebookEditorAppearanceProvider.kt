package org.jetbrains.r.editor

import com.intellij.notebooks.ui.editor.DefaultNotebookEditorAppearance
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.notebooks.ui.visualization.NotebookEditorAppearance
import com.intellij.notebooks.visualization.NotebookEditorAppearanceProvider
import org.jetbrains.r.rmarkdown.RMarkdownVirtualFile
import java.awt.Color

class RMarkdownNotebookEditorAppearanceProvider : NotebookEditorAppearanceProvider {
  override fun create(editor: Editor): NotebookEditorAppearance? {
    if (RMarkdownVirtualFile.hasVirtualFile(editor)) {
      return RMarkdownNotebookEditorAppearance(editor)
    }
    return null
  }
}

class RMarkdownNotebookEditorAppearance(editor: Editor) : DefaultNotebookEditorAppearance(editor) {
  // TODO Sort everything lexicographically.

  override fun getInlayBackgroundColor(scheme: EditorColorsScheme): Color? = codeCellBackgroundColor.get()
  override fun shouldShowCellLineNumbers(): Boolean = false
  override fun shouldShowExecutionCounts(): Boolean = true

  override fun shouldShowOutExecutionCounts(): Boolean = true
  override fun shouldShowRunButtonInGutter(): Boolean = false

  override fun getCellLeftLineWidth(editor: Editor): Int = 0
}