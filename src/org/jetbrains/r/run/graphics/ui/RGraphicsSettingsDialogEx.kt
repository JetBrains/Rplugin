package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import org.jetbrains.r.RBundle
import javax.swing.JComponent

class RGraphicsSettingsDialogEx(private var resolution: Int, private val onOk: (Int) -> Unit) :
  DialogWrapper(null, true)
{
  init {
    setResizable(false)
    title = TITLE
    init()
  }

  override fun createCenterPanel(): JComponent {
    val self = this
    return panel {
      titledRow(PLOTS_TEXT) {
        row(RESOLUTION_TEXT) {
          intTextField(self::resolution, INPUT_COLUMN_COUNT, INPUT_RANGE)
          label(DPI_TEXT)
        }
      }
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    onOk(resolution)
  }

  companion object {
    private const val INPUT_COLUMN_COUNT = 7
    private val INPUT_RANGE = IntRange(1, 9999)

    private val TITLE = RBundle.message("graphics.panel.settings.dialog.title")

    private val PLOTS_TEXT = RBundle.message("chunk.graphics.settings.dialog.for.all.plots")
    private val RESOLUTION_TEXT = RBundle.message("graphics.panel.settings.dialog.resolution")
    private val DPI_TEXT = RBundle.message("graphics.panel.settings.dialog.dpi")

    fun show(resolution: Int, onOk: (Int) -> Unit) {
      RGraphicsSettingsDialogEx(resolution, onOk).show()
    }
  }
}
