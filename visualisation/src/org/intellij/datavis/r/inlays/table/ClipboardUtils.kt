/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.table

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JTable

// TODO: Some problems with getting "event.isControlDown" on component placed on top of Idea Editor content.
/** Clipboard utils to realize Ctrl+C functionality in Table. */
object ClipboardUtils {

  const val LINE_BREAK = "\r"

  private const val CELL_BREAK = "\t"

  fun copyAllToString(table: JTable) : String {
    if (table.rowCount == 0 || table.columnCount == 0) {
      Notifications.Bus.notify(Notification("InlayTable", "Error", "No columns or rows in table.", NotificationType.ERROR))
      return ""
    }

    val builder = StringBuilder()
    for (i in 0 until table.rowCount) {
      for (j in 0 until table.columnCount) {
        table.getValueAt(i, j)?.let { builder.append(escape(it)) }

        if (j < table.columnCount - 1) {
          builder.append(CELL_BREAK)
        }
      }
      builder.append(LINE_BREAK)
    }

    return builder.toString()
  }

  fun copyAllToClipboard(table: JTable) {
    val sel = StringSelection(copyAllToString(table))
    Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
  }

  fun copySelectedToString(table: JTable) : String {

    val selectedColumnCount = table.selectedColumnCount
    val selectedRowCount = table.selectedRowCount
    val selectedRows = table.selectedRows
    val selectedColumns = table.selectedColumns

    if (selectedColumnCount == 0 || selectedRowCount == 0) {
      Notifications.Bus.notify(Notification("InlayTable", "Error", "No columns or rows selected.", NotificationType.ERROR))
      return ""
    }

    val builder = StringBuilder()
    for (i in 0 until selectedRowCount) {
      for (j in 0 until selectedColumnCount) {
        table.getValueAt(selectedRows[i], selectedColumns[j])?.let { builder.append(escape(it)) }

        if (j < selectedColumnCount - 1) {
          builder.append(CELL_BREAK)
        }
      }
      builder.append(LINE_BREAK)
    }
    return builder.toString()
  }

  fun copySelectedToClipboard(table: JTable) {
    val sel = StringSelection(copySelectedToString(table))
    Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
  }

  fun escape(cell: Any): String {
    return cell.toString().replace(LINE_BREAK, " ").replace(CELL_BREAK, " ")
  }

}