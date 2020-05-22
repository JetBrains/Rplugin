package org.intellij.datavis.r.inlays.components

import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI
import com.intellij.openapi.progress.util.ColorProgressBar
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.*

private val TEXT_BORDER = JBUIScale.scale(5)
val PROGRESS_BAR_DEFAULT_WIDTH = JBUIScale.scale(4)

enum class ProgressStatus {
  RUNNING, STOPPED_OK, STOPPED_ERROR
}

data class InlayProgressStatus(val progress: ProgressStatus, val statusText: String)

fun buildProgressStatusComponent(progressStatus: InlayProgressStatus): JComponent? {
  if (progressStatus.progress == ProgressStatus.STOPPED_OK && progressStatus.statusText.isEmpty()) return null
  val progressPanel = JPanel(BorderLayout())
  var progressBar: JProgressBar? = null
  if (progressStatus.progress != ProgressStatus.STOPPED_OK) {
    progressBar = JProgressBar(0, 100)
    progressBar.setUI(InlayProgressBarUI(progressStatus.progress))
    if (progressStatus.progress == ProgressStatus.RUNNING) {
      progressBar.isIndeterminate = true
      progressBar.foreground = ColorProgressBar.GREEN
    }
    else if (progressStatus.progress == ProgressStatus.STOPPED_ERROR) {
      progressBar.value = 100
    }
  }
  val label = JLabel(progressStatus.statusText)
  label.border = BorderFactory.createEmptyBorder(TEXT_BORDER, TEXT_BORDER, TEXT_BORDER, 0)
  progressBar?.let { progressPanel.add(it, BorderLayout.PAGE_START) }
  progressPanel.add(label, BorderLayout.CENTER)
  return progressPanel
}

class InlayProgressBarUI(private val status: ProgressStatus) : DarculaProgressBarUI() {
  override fun getFinishedColor(): Color {
    if (status == ProgressStatus.STOPPED_OK) {
      return JBColor.GRAY
    }
    else {
      return JBColor.RED
    }
  }
}