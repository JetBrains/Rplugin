package org.jetbrains.r.visualization.inlays.components

import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI
import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.JComponent

class InlayProgressBarUI(private val status: RProgressStatus) : DarculaProgressBarUI() {
  override fun getFinishedColor(c: JComponent): Color = if (status == RProgressStatus.STOPPED_OK) {
    JBColor.GRAY
  }
  else {
    JBColor.RED
  }
}