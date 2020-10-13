package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.EditorImpl
import java.awt.Color


/**
 * Constants and functions that affects only visual representation, like colors, sizes of elements, etc.
 */
interface NotebookEditorAppearance: NotebookEditorAppearanceColors, NotebookEditorAppearanceSizes


interface NotebookEditorAppearanceSizes {
  val CODE_CELL_LEFT_LINE_PADDING: Int
  val LINE_NUMBERS_MARGIN: Int

  // TODO Do the pixel constants need JBUI.scale?
  val COMMAND_MODE_CELL_LEFT_LINE_WIDTH : Int
  val EDIT_MODE_CELL_LEFT_LINE_WIDTH : Int
  val CODE_AND_CODE_TOP_GRAY_HEIGHT : Int
  val CODE_AND_CODE_BOTTOM_GRAY_HEIGHT : Int
  val INNER_CELL_TOOLBAR_HEIGHT : Int
  val CELL_BORDER_HEIGHT : Int
  val SPACE_BELOW_CELL_TOOLBAR : Int
  val CELL_TOOLBAR_TOTAL_HEIGHT : Int
  val PROGRESS_STATUS_HEIGHT : Int

  val JUPYTER_CELL_SPACERS_INLAY_PRIORITY: Int
  val JUPYTER_BELOW_OUTPUT_CELL_SPACERS_INLAY_PRIORITY: Int
  val JUPYTER_CELL_TOOLBAR_INLAY_PRIORITY: Int

  val EXTRA_PADDING_EXECUTION_COUNT: Int
}


interface NotebookEditorAppearanceColors {
  // TODO Sort everything lexicographically.
  val CODE_CELL_BACKGROUND: ColorKey
  val GUTTER_INPUT_EXECUTION_COUNT: TextAttributesKey
  val GUTTER_OUTPUT_EXECUTION_COUNT: TextAttributesKey
  val PROGRESS_STATUS_RUNNING_COLOR: ColorKey
  val CELL_UNDER_CARET_COMMAND_MODE_STRIPE_COLOR: ColorKey
  val CELL_UNDER_CARET_EDITOR_MODE_STRIPE_COLOR: ColorKey
  val SAUSAGE_BUTTON_APPEARANCE: TextAttributesKey
  val SAUSAGE_BUTTON_SHORTCUT_COLOR: ColorKey
  val SAUSAGE_BUTTON_BORDER_COLOR: ColorKey

  /**
   * Takes lines of the cell and returns a color for the stripe that will be drawn behind the folding markers.
   * Currently only code cells are supported.
   */
  fun getCellStripeColor(editor: EditorImpl, interval: NotebookCellLines.Interval): Color?
}