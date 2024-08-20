package org.jetbrains.r.visualization.inlays.components

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.util.ColorProgressBar
import com.intellij.ui.scale.JBUIScale
import org.jetbrains.plugins.notebooks.ui.visualization.notebookAppearance
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.progress.ProgressStatus
import java.awt.BorderLayout
import javax.swing.*

object InlayOutputProgressStatus {
  private val TEXT_BORDER = JBUIScale.scale(5)

  val PROGRESS_BAR_DEFAULT_WIDTH = JBUIScale.scale(4)

  fun buildProgressStatusComponent(progressStatus: InlayProgressStatus, editor: Editor): JComponent? {
    if (progressStatus.progress == ProgressStatus.STOPPED_OK && progressStatus.statusText.isEmpty()) return null
    val progressPanel = JPanel(BorderLayout()).apply {
      background = editor.notebookAppearance.getTextOutputBackground(editor.colorsScheme)
    }
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
}