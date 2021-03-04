package org.jetbrains.r.remote.settings

import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.layout.*
import org.jetbrains.r.remote.RRemoteBundle
import org.jetbrains.r.remote.filesystem.RRemoteHostViewManager
import java.awt.event.ActionEvent
import javax.swing.JCheckBox
import javax.swing.JPanel

class RRemoteSettingsConfigurable(private val project: Project): UnnamedConfigurable {
  private val settings = RRemoteSettings.getInstance(project)
  private val component: JPanel
  private var closeRemoteHostView = settings.closeRemoteHostView

  init {
    component = panel(title = RRemoteBundle.message("project.settings.remote.settings.title")) {
      row {
        checkBox(RRemoteBundle.message("project.settings.close.remote.host.view"),
                 closeRemoteHostView) { _: ActionEvent, checkBox: JCheckBox ->
          closeRemoteHostView = checkBox.isSelected
        }
      }
    }
  }

  override fun createComponent() = component

  override fun isModified(): Boolean {
    return closeRemoteHostView != settings.closeRemoteHostView
  }

  override fun reset() {
    closeRemoteHostView = settings.closeRemoteHostView
  }

  override fun apply() {
    if (!settings.closeRemoteHostView && closeRemoteHostView) {
      RRemoteHostViewManager.getInstance(project).closeExtraViews()
    }
    settings.closeRemoteHostView = closeRemoteHostView
  }
}