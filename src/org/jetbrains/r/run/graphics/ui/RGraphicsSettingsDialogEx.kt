package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import org.jetbrains.r.RBundle
import javax.swing.JComponent

class RGraphicsSettingsDialogEx(private var resolution: Int, private var isStandalone: Boolean, private val onOk: (Int, Boolean) -> Unit) :
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
      titledRow(ENGINE_TEXT) {
        row {
          checkBox(STANDALONE_TEXT, self::isStandalone, STANDALONE_COMMENT)
        }
      }
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    onOk(resolution, isStandalone)
  }

  companion object {
    private const val INPUT_COLUMN_COUNT = 7
    private val INPUT_RANGE = IntRange(1, 9999)

    private val TITLE = RBundle.message("graphics.panel.settings.dialog.title")

    private val PLOTS_TEXT = RBundle.message("chunk.graphics.settings.dialog.for.all.plots")
    private val RESOLUTION_TEXT = RBundle.message("graphics.panel.settings.dialog.resolution")
    private val DPI_TEXT = RBundle.message("graphics.panel.settings.dialog.dpi")

    private val ENGINE_TEXT = RBundle.message("graphics.panel.settings.dialog.engine")
    private val STANDALONE_TEXT = RBundle.message("graphics.panel.settings.dialog.standalone.text")
    private val STANDALONE_COMMENT = RBundle.message("graphics.panel.settings.dialog.standalone.comment")

    fun show(resolution: Int, isStandalone: Boolean, onOk: (Int, Boolean) -> Unit) {
      RGraphicsSettingsDialogEx(resolution, isStandalone, onOk).show()
    }
  }
}
