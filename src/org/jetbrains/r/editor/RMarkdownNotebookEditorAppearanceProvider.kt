package org.jetbrains.r.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.editor.*
import org.jetbrains.r.rendering.editor.RMarkdownEditorFactoryListener
import java.awt.Color

class RMarkdownNotebookEditorAppearanceProvider: NotebookEditorAppearanceProvider{
  override fun create(editor: Editor): NotebookEditorAppearance? {
    if (RMarkdownEditorFactoryListener.isRMarkdown(editor)) {
      return RMarkdownNotebookEditorAppearance
    }
    return null
  }
}

object RMarkdownNotebookEditorAppearance: NotebookEditorAppearance, NotebookEditorAppearanceSizes by DefaultNotebookEditorAppearanceSizes {
  // TODO Sort everything lexicographically.

  private val RMARKDOWN_CHUNK = TextAttributesKey.createTextAttributesKey("RMARKDOWN_CHUNK")
  override fun getCodeCellBackground(scheme: EditorColorsScheme): Color? = scheme.getAttributes(RMARKDOWN_CHUNK).backgroundColor
  override fun getInlayBackgroundColor(scheme: EditorColorsScheme): Color? = getCodeCellBackground(scheme)
  override fun shouldShowCellLineNumbers(): Boolean = false

  /**
   * Takes lines of the cell and returns a color for the stripe that will be drawn behind the folding markers.
   * Currently only code cells are supported.
   */
  override fun getCellStripeColor(editor: EditorImpl, interval: NotebookCellLines.Interval): Color? {
    val underCaret = editor.caretModel.logicalPosition.line in interval.lines
    if (underCaret) {
      return getCodeCellBackground(editor.colorsScheme)
    }
    return null
  }
}