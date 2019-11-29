/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.util.ui.JBUI
import java.awt.Font

object InlayDimensions {

  /**
   * Offset for inlay painted round-rect background.
   * We need it to draw visual offsets from surrounding text.
   */
  private const val topOffsetUnscaled = 10
  private const val bottomOffsetUnscaled = 24

  val topOffset = JBUI.scale(topOffsetUnscaled)
  val bottomOffset = JBUI.scale(bottomOffsetUnscaled)

  const val topBorderUnscaled = topOffsetUnscaled + 3
  const val bottomBorderUnscaled = bottomOffsetUnscaled + 5
  const val leftBorderUnscaled = 5


  /** Real borders for inner inlay component */
  val topBorder = JBUI.scale(topBorderUnscaled)
  val bottomBorder = JBUI.scale(bottomBorderUnscaled)
  //val leftBorder = JBUI.scale(leftBorderUnscaled)
  //val rightBorder = JBUI.scale(rightBorderUnscaled)

  val cornerRadius = JBUI.scale(10)

  /** editor.lineHeight */
  var lineHeight: Int = JBUI.scale(10)
    private set

  var rightBorder: Int = 5
    private set

  /** Width of space character ib current editor (editor.getFontMetrics(Font.PLAIN).charWidth(' ')) */
  private var spaceWidth: Int = JBUI.scale(5)

  var smallHeight: Int = lineHeight
    private set

  var previewHeight: Int = lineHeight
    private set

  var defaultHeight: Int = lineHeight
    private set

  var tableHeight: Int = lineHeight
    private set

  var width: Int = spaceWidth * 120
    private set

  var minWidth: Int = spaceWidth * 10
    private set

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

    rightBorder = spaceWidth
  }
}