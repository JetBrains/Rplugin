/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import org.intellij.datavis.r.VisualizationBundle
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.JTable

// TODO: Some problems with getting "event.isControlDown" on component placed on top of Idea Editor content.
/** Clipboard utils to realize Ctrl+C functionality in Table and Plots. */
object ClipboardUtils {

  const val LINE_BREAK = "\r"

  private const val CELL_BREAK = "\t"

  private fun copyAllToString(table: JTable) : String {
    if (table.rowCount == 0 || table.columnCount == 0) {
      // The code should be compatible with 193 and 201 so, so we cannot use NotificationGroup.createIdWithTitle yet
      // NotificationGroup.createIdWithTitle("Notebook Table", VisualizationBundle.message("inlay.output.table.notification.group.name"))
      Notifications.Bus.notify(Notification(VisualizationBundle.message("inlay.output.table.notification.group.name"),
                                            VisualizationBundle.message("clipboard.utils.error"),
                                            VisualizationBundle.message("clipboard.utils.no.columns.or.rows"),
                                            NotificationType.ERROR))
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
      Notifications.Bus.notify(Notification("InlayTable",
                                            VisualizationBundle.message("clipboard.utils.error"),
                                            VisualizationBundle.message("clipboard.utils.no.selection"),
                                            NotificationType.ERROR))
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
    return cell.toString().replace(LINE_BREAK, " ").replace(
      CELL_BREAK, " ")
  }

  fun copyImageToClipboard(image: Image) {
    val transferable = ImageTransferable(image)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
  }

  private class ImageTransferable(private val image: Image) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> {
      return arrayOf(DataFlavor.imageFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
      return flavor == DataFlavor.imageFlavor
    }

    override fun getTransferData(flavor: DataFlavor): Any {
      if (!isDataFlavorSupported(flavor)) {
        throw UnsupportedFlavorException(flavor)
      }
      return image
    }
  }

}