package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.r.psi.RBundle
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

private const val INPUT_COLUMN_COUNT = 7
private val INPUT_RANGE = IntRange(1, 9999)

class RGraphicsSettingsDialogEx(private var resolution: Int, private val onOk: (Int) -> Unit) :
  DialogWrapper(null, true)
{
  init {
    isResizable = false
    title = RBundle.message("graphics.panel.settings.dialog.title")
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      group(RBundle.message("chunk.graphics.settings.dialog.for.all.plots")) {
        row(RBundle.message("graphics.panel.settings.dialog.resolution")) {
          intTextField(INPUT_RANGE)
            .columns(INPUT_COLUMN_COUNT)
            .gap(RightGap.SMALL)
            .bindIntText(::resolution)
          label(RBundle.message("graphics.panel.settings.dialog.dpi"))
        }
      }
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    onOk(resolution)
  }

  companion object {

    fun show(resolution: Int, onOk: (Int) -> Unit) {
      RGraphicsSettingsDialogEx(resolution, onOk).show()
    }
  }
}
