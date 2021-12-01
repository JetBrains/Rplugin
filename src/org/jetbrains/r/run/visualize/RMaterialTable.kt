/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.notebooks.visualization.r.ui.MaterialTable
import java.awt.Component
import java.awt.Font
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumnModel
import javax.swing.table.TableModel

class RMaterialTable(model: TableModel, columnModel: TableColumnModel) : MaterialTable(model, columnModel) {
  init {
    setDefaultRenderer(Any::class.java, NaAwareTableCellRenderer())
  }

  override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
    val c = super.prepareRenderer(renderer, row, column)
    if ((isRowSelected(row) && isColumnSelected(column)) || isRollOverRowIndex(row)) {
      c.foreground = getSelectionForeground()
      c.background = getSelectionBackground()
    } else {
      c.foreground = foreground
      c.background = background
    }
    if (model.getValueAt(row, column) == null) {
      c.foreground = NA_COLOR
    }
    return c
  }

  private class NaAwareTableCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
      table: JTable?,
      value: Any?,
      isSelected: Boolean,
      hasFocus: Boolean,
      row: Int,
      column: Int
    ): Component {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
      if (value == null) {
        font = NA_FONT
        text = NA_TEXT
      }
      return this
    }
  }

  companion object {
    private val NA_FONT = JBUI.Fonts.label().deriveFont(Font.ITALIC)
    private val NA_COLOR = JBColor(0xB9B9B9, 0x6A6A6A)
    private const val NA_TEXT = "NA"
  }
}
