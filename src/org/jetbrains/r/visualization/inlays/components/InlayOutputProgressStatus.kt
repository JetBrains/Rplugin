package org.jetbrains.r.visualization.inlays.components

import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.util.ColorProgressBar
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import org.jetbrains.plugins.notebooks.ui.visualization.notebookAppearance
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.*

object InlayOutputProgressStatus {
  private val TEXT_BORDER = JBUIScale.scale(5)

  val PROGRESS_BAR_DEFAULT_WIDTH = JBUIScale.scale(4)

  fun buildProgressStatusComponent(progressStatus: InlayProgressStatus, editor: Editor): JComponent? {
    if (progressStatus.progress == RProgressStatus.STOPPED_OK && progressStatus.statusText.isEmpty()) return null
    val progressPanel = JPanel(BorderLayout()).apply {
      background = editor.notebookAppearance.getTextOutputBackground(editor.colorsScheme)
    }
    val progressBar: JProgressBar? =
      when (progressStatus.progress) {
        RProgressStatus.RUNNING ->
          JProgressBar(0, 100).also { progressBar ->
            progressBar.setUI(InlayProgressBarUI(progressStatus.progress))
            progressBar.isIndeterminate = true
            progressBar.foreground = ColorProgressBar.GREEN
          }
        RProgressStatus.STOPPED_ERROR ->
          JProgressBar(0, 100).also { progressBar ->
            progressBar.setUI(InlayProgressBarUI(progressStatus.progress))
            progressBar.value = 100
          }
        RProgressStatus.STOPPED_OK -> null
      }

    val label = JLabel(progressStatus.statusText)
    label.border = BorderFactory.createEmptyBorder(TEXT_BORDER, TEXT_BORDER, TEXT_BORDER, 0)
    progressBar?.let { progressPanel.add(it, BorderLayout.PAGE_START) }
    progressPanel.add(label, BorderLayout.CENTER)
    return progressPanel
  }
}

private class InlayProgressBarUI(private val status: RProgressStatus) : DarculaProgressBarUI() {
  override fun getFinishedColor(c: JComponent): Color = if (status == RProgressStatus.STOPPED_OK) {
    JBColor.GRAY
  }
  else {
    JBColor.RED
  }
}