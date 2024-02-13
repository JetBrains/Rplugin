package org.jetbrains.r.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.ui.editor.DefaultNotebookEditorAppearance
import org.jetbrains.plugins.notebooks.ui.visualization.DefaultNotebookEditorAppearanceSizes
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookEditorAppearance
import org.jetbrains.plugins.notebooks.ui.visualization.NotebookEditorAppearanceSizes
import org.jetbrains.plugins.notebooks.visualization.NotebookEditorAppearanceProvider
import org.jetbrains.r.rendering.editor.RMarkdownEditorFactoryListener
import java.awt.Color

class RMarkdownNotebookEditorAppearanceProvider : NotebookEditorAppearanceProvider {
  override fun create(editor: Editor): NotebookEditorAppearance? {
    val virtualFile = RMarkdownEditorFactoryListener.getVirtualFile(editor)
    if (virtualFile != null && RMarkdownEditorFactoryListener.isRMarkdownOrQuarto(virtualFile)) {
      return RMarkdownNotebookEditorAppearance
    }
    return null
  }
}

object RMarkdownNotebookEditorAppearance : NotebookEditorAppearance, NotebookEditorAppearanceSizes by DefaultNotebookEditorAppearanceSizes {
  // TODO Sort everything lexicographically.

  private val RMARKDOWN_CHUNK = TextAttributesKey.createTextAttributesKey("RMARKDOWN_CHUNK")
  override fun getCaretRowColor(scheme: EditorColorsScheme): Color? = DefaultNotebookEditorAppearance.getCaretRowColor(scheme)

  override fun getCodeCellBackground(scheme: EditorColorsScheme): Color? = scheme.getAttributes(RMARKDOWN_CHUNK).backgroundColor
  override fun getInlayBackgroundColor(scheme: EditorColorsScheme): Color? = getCodeCellBackground(scheme)
  override fun shouldShowCellLineNumbers(): Boolean = false
  override fun shouldShowExecutionCounts(): Boolean = true

  override fun shouldShowOutExecutionCounts(): Boolean = true

  /**
   * Takes lines of the cell and returns a color for the stripe that will be drawn behind the folding markers.
   * Currently only code cells are supported.
   */
  override fun getCellStripeColor(editor: EditorImpl, lines: IntRange): Color? {
    val underCaret = editor.caretModel.logicalPosition.line in lines
    if (underCaret) {
      return getCodeCellBackground(editor.colorsScheme)
    }
    return null
  }
}