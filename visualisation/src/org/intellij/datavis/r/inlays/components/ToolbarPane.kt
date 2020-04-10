/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.actionSystem.DataContext
import org.intellij.datavis.r.ui.UiCustomizer
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.JPanel

/**
 * ToolbarPane - special component with central part which is set by
 * setCentralComponent() - this component fill the entire ToolbarPane
 * setToolbarComponent() - preserves initial size and stays in top right corner
 */
class ToolbarPane(dataContext: DataContext) : JLayeredPane(), DataContext by dataContext {
  private var mainPanel: JPanel? = null

  var centralComponent: JComponent? = null
    set(value) {
      field = value
      updateMainComponent()
      updateChildrenBounds()
    }

  var progressComponent: JComponent? = null
    set(value) {
      field = value
      updateMainComponent()
      updateChildrenBounds()
      UiCustomizer.instance.toolbarPaneProgressComponentChanged(this, value)
    }

  var toolbarComponent: JComponent? = null
    set(value) {
      field = value
      add(value, DEFAULT_LAYER)
      updateChildrenBounds()
      UiCustomizer.instance.toolbarPaneToolbarComponentChanged(this, value)
    }

  private fun updateMainComponent() {
    if (mainPanel == null) {
      mainPanel = JPanel(BorderLayout())
      UiCustomizer.instance.toolbarPaneMainPanelCreated(this, mainPanel)
      add(mainPanel, PALETTE_LAYER)
    }
    mainPanel?.let { main ->
      main.removeAll()
      progressComponent?.let { progress ->
        main.add(progress, BorderLayout.PAGE_START)
      }
      centralComponent?.let { central ->
        main.add(central, BorderLayout.CENTER)
      }
    }
  }

  fun updateChildrenBounds() {
    mainPanel?.setBounds(0, 0, width, height)
    val progressBarWidth = if (progressComponent != null) PROGRESS_BAR_DEFAULT_WIDTH else 0
    toolbarComponent?.setBounds(width - toolbarComponent!!.preferredSize.width, progressBarWidth, toolbarComponent!!.preferredSize.width,
                                toolbarComponent!!.preferredSize.height)
  }

  override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
    super.setBounds(x, y, width, height)

    updateChildrenBounds()
  }
}