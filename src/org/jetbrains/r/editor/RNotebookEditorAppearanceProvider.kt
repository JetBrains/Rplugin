package org.jetbrains.r.editor

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.util.ui.JBUI
import java.awt.Color


internal object RMarkdownEditorAppearance {

  // colors
  private val CODE_CELL_BACKGROUND: ColorKey = ColorKey.createColorKey("RMARKDOWN_CODE_BACKGROUND_COLOR")

  fun getInlayBackgroundColor(scheme: EditorColorsScheme): Color = getCodeCellBackgroundColor(scheme)
  fun getTextOutputBackground(scheme: EditorColorsScheme): Color = scheme.defaultBackground
  fun getCodeCellBackgroundColor(scheme: EditorColorsScheme): Color = scheme.getColor(CODE_CELL_BACKGROUND) ?: scheme.defaultBackground

  // sizes
  fun getToolbarHeight(): Int = JBUI.scale(28)
  fun getLeftBorderWidth(): Int = JBUI.scale(9)

  val CELL_SPACERS_INLAY_PRIORITY: Int = 10
}
