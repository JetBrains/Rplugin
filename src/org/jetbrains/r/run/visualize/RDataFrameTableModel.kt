/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import org.jetbrains.concurrency.isRejected
import javax.swing.table.AbstractTableModel

class RDataFrameTableModel(var viewer: RDataFrameViewer) : AbstractTableModel() {
  override fun getRowCount() = viewer.nRows

  override fun getColumnCount() = viewer.nColumns

  override fun getColumnName(index: Int) = viewer.getColumnName(index)

  override fun getColumnClass(index: Int) = viewer.getColumnType(index).java

  override fun getValueAt(row: Int, col: Int): Any? {
    val promise = viewer.ensureLoaded(row, col) { fireTableDataChanged() }
    return when {
      promise.isSucceeded -> {
        when (val value = viewer.getValueAt(row, col)) {
          is Float -> "%g".format(value)
          is Double -> "%g".format(value)
          else -> value
        }
      }
      promise.isRejected -> "<error>"
      else -> "<loading>"
    }
  }

  override fun setValueAt(value: Any?, row: Int, col: Int) = throw NotImplementedError("Table is immutable")
}
