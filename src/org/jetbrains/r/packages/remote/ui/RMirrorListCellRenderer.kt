/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.r.packages.remote.RMirror
import javax.swing.JList

class RMirrorListCellRenderer : ColoredListCellRenderer<RMirror>() {
  override fun customizeCellRenderer(list: JList<out RMirror>, value: RMirror?, index: Int, selected: Boolean, hasFocus: Boolean) {
    if (value != null) {
      append(value.name)
      append(" ")
      append(value.url, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
    }
  }
}
