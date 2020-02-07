/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import javax.swing.JComponent
import javax.swing.JLayeredPane

/**
 * ToolbarPane - special component with central part which is set by
 * setCentralComponent() - this component fill the entire ToolbarPane
 * setToolbarComponent() - preserves initial size and stays in top right corner
 */
class ToolbarPane : JLayeredPane() {

  var centralComponent: JComponent? = null
    set(value) {
      field = value
      add(value, PALETTE_LAYER)
      updateChildrenBounds()
    }

  var toolbarComponent: JComponent? = null
    set(value) {
      field = value
      add(value, DEFAULT_LAYER)
      updateChildrenBounds()
    }

  fun updateChildrenBounds() {
    centralComponent?.setBounds(0, 0, width, height)
    toolbarComponent?.setBounds(width - toolbarComponent!!.preferredSize.width, 0,
                                toolbarComponent!!.preferredSize.width, toolbarComponent!!.preferredSize.height)
  }

  override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
    super.setBounds(x, y, width, height)

    updateChildrenBounds()
  }
}