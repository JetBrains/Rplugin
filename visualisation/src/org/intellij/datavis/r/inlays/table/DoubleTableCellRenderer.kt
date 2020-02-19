/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.table

import org.jetbrains.annotations.NonNls
import javax.swing.table.DefaultTableCellRenderer

@NonNls
private const val NULL = "<null>"

class DoubleTableCellRenderer : DefaultTableCellRenderer() {
  override fun setValue(value: Any?) {
    text = if((value as Double).isNaN()) NULL else value.toString()
  }
}
