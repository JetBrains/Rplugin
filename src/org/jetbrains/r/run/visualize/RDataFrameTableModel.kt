/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import javax.swing.table.AbstractTableModel

class RDataFrameTableModel(var viewer: RDataFrameViewer) : AbstractTableModel() {
  override fun getRowCount() = viewer.nRows

  override fun getColumnCount() = viewer.nColumns

  override fun getColumnName(index: Int) = viewer.getColumnName(index)

  override fun getColumnClass(index: Int) = viewer.getColumnType(index).java

  override fun getValueAt(row: Int, col: Int): Any? {
    viewer.ensureLoaded(row, col) { fireTableDataChanged() } ?: return viewer.getValueAt(row, col)
    return "<loading>"
  }

  override fun setValueAt(value: Any?, row: Int, col: Int) = throw NotImplementedError("Table is immutable")
}
