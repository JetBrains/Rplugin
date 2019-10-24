/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.impl.view.EditorPainter
import com.intellij.openapi.editor.impl.view.FontLayoutService
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.lang.invoke.MethodHandles
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.abs

/** Inlay editor component displaying text output, table data and charts for notebook paragraphs. */
open class InlayComponent : JPanel(BorderLayout()), EditorCustomElementRenderer {

  /** Inlay, associated with this component. Our swing component positioned and sized according inlay. */
  var inlay: Inlay<*>? = null

  private var resizeController: ResizeController? = null

  override fun paint(g: Graphics) {
    // We need this fix with AlphaComposite.SrcOver to resolve problem of black background on transparent images such as icons.
    //ToDo: - We need to make some tests on mac and linux for this, maybe this is applicable only to windows platform.
    //      - And also we need to check this on different Windows versions (definitely we have problems on Windows) .
    //      - Definitely we have problems on new macBook
    val oldComposite = (g as Graphics2D).composite
    g.composite = AlphaComposite.SrcOver
    super<JPanel>.paint(g)
    g.composite = oldComposite
  }

  companion object {
    val LOG = Logger.getInstance(MethodHandles.lookup().lookupClass())
  }

  /** Realizes resize of InlayComponent by dragging resize icon in right bottom corner of component. */
  class ResizeController(private val inlayComponent: InlayComponent) : MouseAdapter() {

    private var prevPoint: Point? = null

    private enum class ScaleMode { NONE, N /*, W, NW*/ }

    private var scaleMode = ScaleMode.NONE

    private val nResizeCursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
    private val defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)

    private fun setCursor(cursor: Cursor) {
      if (inlayComponent.cursor != cursor) {
        inlayComponent.cursor = cursor
      }
    }

    override fun mouseReleased(e: MouseEvent?) {

      val editor = (inlayComponent.inlay!!.editor as EditorImpl)

      // Snapping to right margin.
      if (EditorPainter.isMarginShown(editor) && prevPoint != null) {

        val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        val context = FontInfo.getFontRenderContext(editor.contentComponent)
        val fm = FontInfo.getFontMetrics(font, context)

        val width = FontLayoutService.getInstance().charWidth2D(fm, ' '.toInt())

        val rightMargin = editor.settings.getRightMargin(editor.project) * width

        SwingUtilities.convertPointFromScreen(prevPoint!!, editor.contentComponent)

        if (abs(prevPoint!!.x - rightMargin) < JBUI.scale(40)) {
          inlayComponent.deltaSize(rightMargin.toInt() - prevPoint!!.x, 0)
          SwingUtilities.invokeLater {
            inlayComponent.revalidate()
            inlayComponent.repaint()
          }
        }
      }

      prevPoint = null
      scaleMode = ScaleMode.NONE
    }

    override fun mousePressed(e: MouseEvent) {

      val correctedHeight = inlayComponent.height - InlayDimensions.bottomBorder

      scaleMode = if (e.point.y > correctedHeight) {
        ScaleMode.N
      }
      else {
        return
      }

      prevPoint = e.locationOnScreen
    }

    override fun mouseDragged(e: MouseEvent?) {

      if (prevPoint == null) {
        return
      }

      val locationOnScreen = e!!.locationOnScreen

      val dy = if (scaleMode == ScaleMode.N) locationOnScreen.y - prevPoint!!.y else 0

      inlayComponent.deltaSize(0, dy)
      prevPoint = locationOnScreen
    }

    override fun mouseMoved(e: MouseEvent) {

      if (scaleMode != ScaleMode.NONE) {
        return
      }

      val correctedHeight = inlayComponent.height - InlayDimensions.bottomBorder
      setCursor(if (e.point.y > correctedHeight) nResizeCursor else defaultCursor)
    }

    override fun mouseExited(e: MouseEvent) {
      if (scaleMode == ScaleMode.NONE) {
        setCursor(defaultCursor)
      }
    }
  }

  var resizable: Boolean
    get() {
      return resizeController != null
    }
    set(value) {
      if (value && resizeController == null) {
        resizeController = ResizeController(this)
        addMouseMotionListener(resizeController)
        addMouseListener(resizeController)
      }
      else if (!value && resizeController != null) {
        removeMouseListener(resizeController)
        removeMouseMotionListener(resizeController)
        resizeController = null
      }
    }

  //region EditorCustomElementRenderer
  override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {

    // Actually bounds will be updated only when they are changed what happens relatively rarely.
    updateComponentBounds(inlay)

    // A try to resolve Code Lens problem, when our inlay paints in strange places.
    //        ApplicationManager.getApplication().invokeLater {
    //            updateComponentBounds(inlay)
    //        }
  }

  /** Updates position and size of linked component. */
  fun updateComponentBounds(inlay: Inlay<*>) {
    inlay.bounds?.let { updateComponentBounds(it) }
  }

  /** Returns width of component. */
  override fun calcWidthInPixels(inlay: Inlay<*>): Int {
    return size.width
  }

  /** Returns height of component. */
  override fun calcHeightInPixels(inlay: Inlay<*>): Int {
    return size.height
  }

  /** Changes size of component and also calls inlay.updateSize. */
  open fun deltaSize(dx: Int, dy: Int) {

    if ((dx == 0 && dy == 0) /* || size.width + dx < 32 || size.height + dy < 32*/) {
      return
    }

    size = Dimension(size.width + dx, size.height + dy)

    inlay?.updateSize()

    revalidate()
    repaint()
  }
  //endregion

  //region InlayToComponentSynchronizer

  /** Normally this should happens directly after getting inlay with editor.inlayModel.addBlockElement. */
  // And this should only happens once.
  open fun assignInlay(inlay: Inlay<*>) {
    this.inlay = inlay

    // This method force inlay to query the size from us.
    inlay.updateSize()
  }

  /** Fits size and position of component to inlay's size and position. */
  protected fun updateComponentBounds(targetRegion: Rectangle) {
    if (bounds == targetRegion) {
      return
    }

    bounds = targetRegion

    revalidate()
    repaint()
  }

  /** Deleted inlay. This component itself should be removed manually (like: comp.parent?.remove(comp)). */
  open fun disposeInlay() {

    if (inlay == null) {
      return
    }

    inlay!!.dispose()
    inlay = null
  }
  //endregion
}