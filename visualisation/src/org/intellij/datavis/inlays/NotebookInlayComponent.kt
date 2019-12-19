/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.intellij.datavis.inlays.components.*
import org.intellij.datavis.inlays.dataframe.DataFrame
import org.intellij.datavis.inlays.dataframe.DataFrameCSVAdapter
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min

class NotebookInlayComponent(val cell: PsiElement, private val editor: EditorImpl)
  : InlayComponent(), MouseListener, MouseMotionListener {
  companion object {
    val separatorRenderer = CustomHighlighterRenderer { editor, highlighter1, g ->
      val y1 = editor.offsetToPoint2D(highlighter1.endOffset).y.toInt()

      g.color = editor.colorsScheme.getColor(EditorColors.RIGHT_MARGIN_COLOR)

      g.drawLine(0, y1, editor.component.width, y1)
    }
  }

  lateinit var onHeightChanged: () -> Unit
  var selected = false

  private var state: NotebookInlayState? = null

  /** Settings could be loaded before the data comes and that's why we are storing this settings and trying to apply them later. */
  private var delayedCurrentTab: String? = null

  /** Invoked on any settings change (size or series settings).*/
  var onChange: (() -> Unit)? = null

  // ToDo Currently called even on collapse/expand, but should not.
  var onSizeChange: (() -> Unit)? = null

  private var expandedHeight = 0

  /** Inlay short view, shown in collapsed state or in empty state. */
  private var toolbar: NotebookInlayToolbar? = null

  private var gutter: JComponent? = null

  private var mouseOverNewParagraphArea = false

  private var separatorHighlighter: RangeHighlighter? = null

  private val disposable = Disposer.newDisposable()

  init {
    cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    border = JBUI.Borders.empty(InlayDimensions.topBorderUnscaled,
                                InlayDimensions.leftBorderUnscaled,
                                InlayDimensions.bottomBorderUnscaled,
                                InlayDimensions.rightBorderUnscaled)
    Disposer.register(editor.disposable, disposable)
    addMouseListener(this)
    addMouseMotionListener(this)
  }

  // region MouseListener
  override fun mouseReleased(e: MouseEvent?) {}

  override fun mouseEntered(e: MouseEvent?) {}

  override fun mouseExited(e: MouseEvent?) {
    if (mouseOverNewParagraphArea) {
      mouseOverNewParagraphArea = false
      repaint(0, height - JBUI.scale(20), width, height)
    }
  }

  override fun mousePressed(e: MouseEvent?) {}
  override fun mouseClicked(e: MouseEvent?) {}

  // endregion

  // region MouseMotionListener
  override fun mouseMoved(e: MouseEvent) {
    val value = e.y > height - JBUI.scale(20)
    if (value != mouseOverNewParagraphArea) {
      mouseOverNewParagraphArea = value
      repaint(0, height - JBUI.scale(20), width, height)
    }
  }

  override fun mouseDragged(e: MouseEvent?) {}
  // endregion

  private fun getOrAddToolbar(): NotebookInlayToolbar {
    toolbar?.let { return it }
    return NotebookInlayToolbar().also {
      toolbar = it
      add(it, BorderLayout.CENTER)
    }
  }

  private fun removeToolbar() {
    toolbar?.let { remove(it) }
    toolbar = null
  }

  /** Paints rounded rect panel - background of inlay component. */
  override fun paintComponent(g: Graphics) {

    val g2d = g.create()

    g2d.color = //if (selected) {
      //inlay!!.editor.colorsScheme.getAttributes(RMARKDOWN_CHUNK).backgroundColor
    //}
    //else {
      (inlay!!.editor as EditorImpl).backgroundColor
    //}

    g2d.fillRect(0, 0, width, InlayDimensions.topOffset + InlayDimensions.cornerRadius)
    g2d.fillRect(0, height - InlayDimensions.bottomOffset - InlayDimensions.cornerRadius, width,
                 InlayDimensions.bottomOffset + InlayDimensions.cornerRadius)


    g2d.color = UIUtil.getLabelBackground()
    g2d.fillRoundRect(0, InlayDimensions.topOffset, width,
                      height - InlayDimensions.bottomOffset - InlayDimensions.topOffset,
                      InlayDimensions.cornerRadius, InlayDimensions.cornerRadius)

    g2d.dispose()
  }

  /**
   * Draw separator line below cell. Also fills cell background
   */
  private fun updateCellSeparator() {

    if (separatorHighlighter != null &&
        separatorHighlighter!!.startOffset == cell.textRange.startOffset &&
        separatorHighlighter!!.endOffset == cell.textRange.endOffset) {
      return
    }

    if (separatorHighlighter != null) {
      editor.markupModel.removeHighlighter(separatorHighlighter!!)
    }

    // ToDo This is half-hack. The problem is that updateCellSeparator called from PSI change
    // but if we have a lot of sequential changes in editor.document (line Backspace button is hold)
    // document was updated but PSI update is async and that's why we have this check.
    if(cell.textRange.endOffset > editor.document.textLength) {
      return
    }

    try {
      separatorHighlighter = editor.markupModel.addRangeHighlighter(cell.textRange.startOffset, cell.textRange.endOffset,
                                                                    HighlighterLayer.SYNTAX - 1, null,
                                                                    HighlighterTargetArea.LINES_IN_RANGE).apply {

        customRenderer = separatorRenderer

        lineMarkerRenderer = LineMarkerRenderer { _, g, r ->
          @Suppress("INACCESSIBLE_TYPE")
          val gutterWidth = ((editor as EditorImpl).gutterComponentEx as JComponent).width

          val y = r.y + r.height - editor.lineHeight
          g.color = editor.colorsScheme.getColor(EditorColors.RIGHT_MARGIN_COLOR)
          g.drawLine(0, y, gutterWidth + 10, y)

//          if (selected) {
//            g.color = editor.colorsScheme.getAttributes(RMARKDOWN_CHUNK).backgroundColor
//            g.fillRect(gutterWidth - 10, r.y + 1, 10, r.height - 1 - editor.lineHeight)
//          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun assignInlay(inlay: Inlay<*>) {
    super.assignInlay(inlay)

    updateCellSeparator()

    gutter = editor.gutter as JComponent
  }

  override fun disposeInlay() {
    if (separatorHighlighter != null && inlay != null) {
      editor.markupModel.removeHighlighter(separatorHighlighter!!)
      separatorHighlighter = null
    }
    super.disposeInlay()
  }

  fun dispose() {
    Disposer.dispose(disposable)
  }

  /** Changes size of component  and also called updateSize for inlay. */
  override fun deltaSize(dx: Int, dy: Int) {
    if (dx == 0 && dy == 0) {
      return
    }

    val newWidth = max(size.width + dx, InlayDimensions.minWidth)
    var newHeight = size.height + dy

    // We will not allow to have size below minimal
    //    if (state == null) {
    //      newHeight = InlayDimensions.smallHeight
    //    }
    //    else {
    newHeight = max(InlayDimensions.minHeight, newHeight)
    //}

    val newDx = newWidth - size.width
    val newDy = newHeight - size.height

    // Inside super.deltaSize we also have check for dx == 0 && dy == 0.
    super.deltaSize(newDx, newDy)
    if (newDx == 0 && newDy == 0) {
      return
    }

    // Only height changes is valuable, the width will always be the same as editor width.
    if (newDy != 0) {
      onSizeChange?.invoke()
      onChange?.invoke()
    }
  }

  /** Creates a component for displaying output. */
  private fun getOrCreateOutput(): NotebookInlayOutput {

    if (state !is NotebookInlayOutput) {

      if (state != null) {
        remove(state)
      }

      state = NotebookInlayOutput(editor.project!!, disposable).apply {
        addToolbar()
        onHeightCalculated = { height ->
          ApplicationManager.getApplication().invokeLater {
            adjustSize(height, this)
          }
        }
      }.also { addState(it) }
    }
    resizable = true
    return state as NotebookInlayOutput
  }

  private fun createOrSetInlayData(dataFrame: DataFrame): NotebookInlayData {
    if (state !is NotebookInlayData) {
      if (state != null) {
        remove(state)
      }

      state = NotebookInlayData(editor.project!!, disposable, dataFrame).apply {
        onHeightCalculated = { height ->
          ApplicationManager.getApplication().invokeLater {
            adjustSize(height, this)
          }
        }
      }.also { addState(it) }
    }
    else {
      (state as NotebookInlayData).setDataFrame(dataFrame)
    }
    resizable = true
    return state as NotebookInlayData
  }

  private fun addState(state: NotebookInlayState) {
    add(state, BorderLayout.CENTER)

    // Resizing only if inlay has minimal size or if we have expanded size set.
    if (expandedHeight != 0) {
      deltaSize(0, expandedHeight - size.height)
      expandedHeight = 0
    }
    else if (height < InlayDimensions.minHeight) {
      deltaSize(0, InlayDimensions.defaultHeight - height)
    }

    revalidate()
    repaint()
  }

  /** Adjusts size of notebook output. Method called when success data comes with inlay component desired height. */
  private fun adjustSize(height: Int, output: NotebookInlayState) {

    output.onHeightCalculated = null

    val desiredHeight = min(InlayDimensions.defaultHeight, height + InlayDimensions.topBorder + InlayDimensions.bottomBorder)

    deltaSize(0, desiredHeight - size.height)

    onHeightChanged()
  }

  /**
   * Event from notebook with console output. This output contains intermediate data from console.
   * @param update if true than current data in output will be cleared
   */
  private fun onOutput(data: String, type: String, cleanup: () -> Unit) {
    state?.clear()

    val output = getOrCreateOutput()
    output.clearAction = cleanup
    output.addData(type, data)

    if (size.height == InlayDimensions.smallHeight) {
      deltaSize(0, InlayDimensions.previewHeight - size.height)
    }
  }

  private fun onMultiOutput(inlayOutputs: List<InlayOutput>, cleanup: () -> Unit) {
    state?.clear()
    removeToolbar()

    if (state !is NotebookInlayMultiOutput) {
      if (state != null) {
        remove(state)
      }
      state = NotebookInlayMultiOutput(cell.project, disposable).apply {
        onHeightCalculated = { height ->
          ApplicationManager.getApplication().invokeLater {
            adjustSize(height, this)
          }
        }
        clearAction = cleanup
      }.also { addState(it) }
    }
    resizable = true
    (state as NotebookInlayMultiOutput).onOutputs(inlayOutputs)
  }

  fun addInlayOutputs(inlayOutputs: List<InlayOutput>, cleanup: (() -> Unit)) {
    if (inlayOutputs.size > 1) {
      onMultiOutput(inlayOutputs, cleanup)
    }
    else {
      val (data, type) = inlayOutputs.first()
      if (type == "TABLE") {
        val csv = DataFrameCSVAdapter.fromCsvString(data)
        createOrSetInlayData(csv).clearAction = cleanup
        if (size.height == InlayDimensions.smallHeight) {
          deltaSize(0, InlayDimensions.previewHeight - size.height)
        }
      }
      else {
        onOutput(data, type, cleanup)
      }
    }
  }
}
