package org.jetbrains.r.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.ui.JBColor
import com.intellij.notebooks.ui.editor.DefaultNotebookEditorAppearance
import com.intellij.notebooks.ui.visualization.DefaultNotebookEditorAppearanceSizes
import com.intellij.notebooks.ui.visualization.NotebookEditorAppearance
import com.intellij.notebooks.ui.visualization.NotebookEditorAppearanceSizes
import com.intellij.notebooks.visualization.NotebookEditorAppearanceProvider
import org.jetbrains.r.rmarkdown.RMarkdownVirtualFile
import java.awt.Color

class RMarkdownNotebookEditorAppearanceProvider : NotebookEditorAppearanceProvider {
  override fun create(editor: Editor): NotebookEditorAppearance? {
    if (RMarkdownVirtualFile.hasVirtualFile(editor)) {
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
  override fun shouldShowRunButtonInGutter(): Boolean = false

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

  override fun getCellStripeColor(editor: Editor): Color = JBColor.GRAY
  override fun getCellStripeHoverColor(editor: Editor): Color = JBColor.BLUE

  override fun getCellLeftLineWidth(editor: Editor): Int = 0
}