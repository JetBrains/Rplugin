/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.table

import javax.swing.table.DefaultTableCellRenderer

class DoubleTableCellRenderer : DefaultTableCellRenderer() {
  override fun setValue(value: Any?) {
    if((value as Double).isNaN()) {
      text = "<null>"
    } else {
      text = value.toString()
    }
  }
}
