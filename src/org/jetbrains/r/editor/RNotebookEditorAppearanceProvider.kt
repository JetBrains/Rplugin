package org.jetbrains.r.editor

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.util.ui.JBUI
import java.awt.Color


internal object RMarkdownEditorAppearance {

  // colors
  val CODE_CELL_BACKGROUND: ColorKey = ColorKey.createColorKey("RMARKDOWN_CODE_BACKGROUND_COLOR")

  fun getInlayBackgroundColor(scheme: EditorColorsScheme): Color = getCodeCellBackgroundColor(scheme)
  fun getTextOutputBackground(scheme: EditorColorsScheme): Color = scheme.defaultBackground
  fun getCodeCellBackgroundColor(scheme: EditorColorsScheme): Color = scheme.getColor(CODE_CELL_BACKGROUND) ?: scheme.defaultBackground

  // sizes

  // TODO it's hardcoded, but it should be equal to distance between a folding line and an editor.
  val CODE_CELL_LEFT_LINE_PADDING: Int = 5

  // TODO Do the pixel constants need JBUI.scale?
  val INNER_CELL_TOOLBAR_HEIGHT: Int = JBUI.scale(24)
  val SPACE_BELOW_CELL_TOOLBAR: Int = JBUI.scale(4)

  val CELL_SPACERS_INLAY_PRIORITY: Int = 10

  fun getLeftBorderWidth(): Int = JBUI.scale(4) + CODE_CELL_LEFT_LINE_PADDING
}
