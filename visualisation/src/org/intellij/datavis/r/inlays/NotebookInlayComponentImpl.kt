package org.intellij.datavis.r.inlays

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.PsiElement
import com.intellij.util.ui.UIUtil
import java.awt.Graphics

class NotebookInlayComponentImpl(cell: PsiElement, editor: EditorImpl) : NotebookInlayComponent(cell, editor) {
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
}