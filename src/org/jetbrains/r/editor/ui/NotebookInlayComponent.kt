/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.r.RPluginCoroutineScope
import org.jetbrains.r.editor.RMarkdownEditorAppearance
import org.jetbrains.r.visualization.RNotebookIntervalPointer
import org.jetbrains.r.visualization.inlays.InlayComponent
import org.jetbrains.r.visualization.inlays.InlayOutputData
import org.jetbrains.r.visualization.inlays.RInlayDimensions
import org.jetbrains.r.visualization.inlays.components.InlayProgressStatus
import org.jetbrains.r.visualization.inlays.components.InlayStateCustomizer
import org.jetbrains.r.visualization.inlays.components.NotebookInlayMultiOutput
import org.jetbrains.r.visualization.inlays.components.NotebookInlayState
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import kotlin.math.max
import kotlin.math.min


class NotebookInlayComponent(
  val cell: RNotebookIntervalPointer,
  private val editor: EditorImpl,
)
  : InlayComponent(), MouseListener, MouseMotionListener {
  companion object {
    /* DS-625 Those resizes cause scroll flickering. Currently, I am not sure which value is better */
    private const val isResizeOutputToPreviewHeight: Boolean = true
  }

  lateinit var beforeHeightChanged: () -> Unit
  lateinit var afterHeightChanged: () -> Unit
  var selected = false

  private var state: NotebookInlayState? = null

  private var expandedHeight = 0

  private var mouseOverNewParagraphArea = false

  protected var separatorHighlighter: RangeHighlighter? = null

  private val disposable = Disposer.newDisposable()

  /** If the value is `false`, the component won't limit its height and apply the height passed to [adjustSize] method directly. */
  private var shouldLimitMaxHeight = true

  init {
    cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    border = JBUI.Borders.empty(RInlayDimensions.topBorderUnscaled,
                                RInlayDimensions.leftBorderUnscaled,
                                RInlayDimensions.bottomBorderUnscaled,
                                RInlayDimensions.rightBorderUnscaled)
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

  override fun paintComponent(g: Graphics) {
    /** Paints rounded rect panel - background of inlay component. */
    val g2d = g.create()

    g2d.color =
      (inlay!!.editor as EditorImpl).let {
        RMarkdownEditorAppearance.getInlayBackgroundColor(it.colorsScheme)
      }

    g2d.fillRect(0, 0, width, RInlayDimensions.topOffset + RInlayDimensions.cornerRadius)
    g2d.fillRect(0, height - RInlayDimensions.bottomOffset - RInlayDimensions.cornerRadius, width,
                 RInlayDimensions.bottomOffset + RInlayDimensions.cornerRadius)


    g2d.color = UIUtil.getLabelBackground()
    g2d.fillRoundRect(0, RInlayDimensions.topOffset, width,
                      height - RInlayDimensions.bottomOffset - RInlayDimensions.topOffset,
                      RInlayDimensions.cornerRadius, RInlayDimensions.cornerRadius)

    g2d.dispose()
  }

  override fun assignInlay(inlay: Inlay<*>) {
    super.assignInlay(inlay)
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

    val newWidth = max(size.width + dx, RInlayDimensions.minWidth)
    var newHeight = size.height + dy

    newHeight = max(RInlayDimensions.minHeight, newHeight)
    val newDx = newWidth - size.width
    val newDy = newHeight - size.height

    super.deltaSize(newDx, newDy)
  }

  /** Creates a component for displaying output. */
  private fun getOrCreateOutput(): NotebookInlayOutput {

    if (state !is NotebookInlayOutput) {

      if (state != null) {
        remove(state)
      }

      state = NotebookInlayOutput(editor, disposable).apply {
        addToolbar()
        onHeightCalculated = { height ->
          RPluginCoroutineScope.getApplicationScope().launch(Dispatchers.EDT) {
            adjustSize(height)
          }
        }
      }.also { addState(it) }
    }
    resizable = true
    return state as NotebookInlayOutput
  }

  fun updateProgressStatus(progressStatus: InlayProgressStatus) {
    state?.updateProgressStatus(progressStatus)
  }

  private fun addState(state: NotebookInlayState) {
    add(state, BorderLayout.CENTER)

    // Resizing only if inlay has minimal size or if we have expanded size set.
    if (expandedHeight != 0) {
      deltaSize(0, expandedHeight - size.height)
      expandedHeight = 0
    }
    else if (height < RInlayDimensions.minHeight) {
      deltaSize(0, RInlayDimensions.defaultHeight - height)
    }

    revalidate()
    repaint()
  }

  /** Adjusts size of notebook output. Method called when success data comes with inlay component desired height. */
  private fun adjustSize(height: Int) {
    beforeHeightChanged()

    var desiredHeight = height + RInlayDimensions.topBorder + RInlayDimensions.bottomBorder
    if (shouldLimitMaxHeight) {
      desiredHeight = min(RInlayDimensions.defaultHeight, desiredHeight)
    }

    deltaSize(0, desiredHeight - size.height)

    afterHeightChanged()
  }

  /** Event from notebook with console output. This output contains intermediate data from console. */
  private fun onOutput(data: InlayOutputData) {
    state?.clear()

    val output = getOrCreateOutput()
    output.addData(data)

    InlayStateCustomizer.customize(output)

    if (isResizeOutputToPreviewHeight && size.height == RInlayDimensions.smallHeight) {
      deltaSize(0, RInlayDimensions.previewHeight - size.height)
    }
  }

  private fun onMultiOutput(inlayOutputs: List<InlayOutputData>) {
    state?.clear()

    shouldLimitMaxHeight = false

    if (state !is NotebookInlayMultiOutput) {
      if (state != null) {
        remove(state)
      }
      state = TabbedMultiOutput(editor, disposable).also { st ->
        st.onHeightCalculated = { height ->
          ApplicationManager.getApplication().invokeLater {
            adjustSize(height)
          }
        }
        addState(st)
      }
    }

    resizable = true
    (state as? NotebookInlayMultiOutput)?.also { st ->
      st.onOutputs(inlayOutputs)
      InlayStateCustomizer.customize(st)
    }
  }

  fun addInlayOutputs(inlayOutputs: List<InlayOutputData>) {
    if (inlayOutputs.size > 1) {
      onMultiOutput(inlayOutputs)
    }
    else {
      val output = inlayOutputs.first()
      onOutput(output)
    }
  }

  fun addText(message: String, outputType: Key<*>) {
    getOrCreateOutput().addText(message, outputType)
    if (size.height <= RInlayDimensions.previewHeight * 3) {
      deltaSize(0, min(RInlayDimensions.lineHeight * (message.lines().size - 1), RInlayDimensions.previewHeight * 3 - size.height))
    }
  }

  fun onViewportChange(isInViewport: Boolean) {
    state?.onViewportChange(isInViewport)
  }

  fun clearOutputs() {
    state?.clear()
  }
}
