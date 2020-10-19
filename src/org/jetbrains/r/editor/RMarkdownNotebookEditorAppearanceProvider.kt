package org.jetbrains.r.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.ColorKey
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

//TODO add keys
object RMarkdownNotebookEditorAppearance: NotebookEditorAppearance, NotebookEditorAppearanceSizes by DefaultNotebookEditorAppearanceSizes {
  // TODO Sort everything lexicographically.

  private val RMARKDOWN_CHUNK = TextAttributesKey.createTextAttributesKey("RMARKDOWN_CHUNK")
  override fun getCodeCellBackground(scheme: EditorColorsScheme): Color? = scheme.getAttributes(RMARKDOWN_CHUNK).backgroundColor

  override val GUTTER_INPUT_EXECUTION_COUNT = TextAttributesKey.createTextAttributesKey("")
  override val GUTTER_OUTPUT_EXECUTION_COUNT = TextAttributesKey.createTextAttributesKey("")
  override val PROGRESS_STATUS_RUNNING_COLOR = ColorKey.createColorKey("")
  override val CELL_UNDER_CARET_COMMAND_MODE_STRIPE_COLOR = ColorKey.createColorKey("")
  override val CELL_UNDER_CARET_EDITOR_MODE_STRIPE_COLOR = ColorKey.createColorKey("")
  override val SAUSAGE_BUTTON_APPEARANCE = TextAttributesKey.createTextAttributesKey("")
  override val SAUSAGE_BUTTON_SHORTCUT_COLOR = ColorKey.createColorKey("")
  override val SAUSAGE_BUTTON_BORDER_COLOR = ColorKey.createColorKey("")

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