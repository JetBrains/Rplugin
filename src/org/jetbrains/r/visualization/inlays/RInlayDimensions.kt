package org.jetbrains.r.visualization.inlays

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.notebooks.visualization.r.inlays.InlayDimensions
import com.intellij.notebooks.visualization.r.inlays.InlayDimensions.bottomBorder
import com.intellij.notebooks.visualization.r.inlays.InlayDimensions.topBorder
import java.awt.Dimension
import java.awt.Font
import kotlin.math.max

object RInlayDimensions {

  val topOffset = JBUI.scale(InlayDimensions.topOffsetUnscaled)
  val bottomOffset = JBUI.scale(InlayDimensions.bottomOffsetUnscaled)

  const val leftBorderUnscaled = 0
  const val rightBorderUnscaled = 0

  val cornerRadius = JBUI.scale(10)

  /** editor.lineHeight */
  @Volatile
  var lineHeight: Int = JBUI.scale(10)
    private set

  @Volatile
  var rightBorder: Int = 5
    private set

  /** Width of space character ib current editor (editor.getFontMetrics(Font.PLAIN).charWidth(' ')) */
  private var spaceWidth: Int = JBUI.scale(5)

  @Volatile
  var smallHeight: Int = lineHeight
    private set

  @Volatile
  var previewHeight: Int = lineHeight
    private set

  @Volatile
  var defaultHeight: Int = lineHeight
    private set

  @Volatile
  var tableHeight: Int = lineHeight
    private set

  @Volatile
  var width: Int = spaceWidth * 120
    private set

  @Volatile
  var minWidth: Int = spaceWidth * 10
    private set

  @Volatile
  var minHeight: Int = spaceWidth * 10
    private set

  private var initialized = false

  fun init(editor: EditorImpl) {

    if (initialized) {
      return
    }

    lineHeight = editor.lineHeight + JBUI.scale(5)
    spaceWidth = editor.getFontMetrics(Font.PLAIN).charWidth(' ')

    smallHeight = lineHeight + topBorder + bottomBorder
    defaultHeight = lineHeight * 25 + topBorder + bottomBorder
    tableHeight = lineHeight * 25 + topBorder + bottomBorder
    previewHeight = lineHeight * 4 + topBorder + bottomBorder

    width = spaceWidth * 120
    minWidth = spaceWidth * 10
    minHeight = smallHeight

    rightBorder = spaceWidth * 3
  }

  fun calculateInlayWidth(editor: EditorEx): Int {
    return max(0, editor.scrollPane.let { it.viewport.width - it.verticalScrollBar.width })
  }

  fun calculateInlayHeight(maxWidth: Int, maxHeight: Int, editor: Editor): Int {
    val scaleMultiplier = if (UIUtil.isRetina()) 2 else 1
    val editorWidth = editor.contentComponent.width
    return if (maxWidth * scaleMultiplier <= editorWidth) {
      maxHeight * scaleMultiplier
    }
    else {
      maxHeight * editorWidth / maxWidth
    }
  }

  fun calculateInlayContentSize(editor: EditorEx, inlayHeight: Int = defaultHeight): Dimension {
    val inlayWidth = calculateInlayWidth(editor)
    val contentWidth = max(inlayWidth - JBUI.scale(rightBorderUnscaled) - JBUI.scale(leftBorderUnscaled), 0)
    val contentHeight = max(inlayHeight - topBorder - topOffset - bottomBorder - bottomOffset, 0)
    return Dimension(contentWidth, contentHeight)
  }
}