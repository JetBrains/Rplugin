package org.jetbrains.r.visualization.inlays.components

import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI
import com.intellij.ui.JBColor
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.progress.ProgressStatus
import java.awt.Color
import javax.swing.JComponent

class InlayProgressBarUI(private val status: ProgressStatus) : DarculaProgressBarUI() {
  override fun getFinishedColor(c: JComponent): Color = if (status == ProgressStatus.STOPPED_OK) {
    JBColor.GRAY
  }
  else {
    JBColor.RED
  }
}