/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays.components

import com.intellij.icons.AllIcons
import com.intellij.ide.CopyProvider
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.TextTransferable
import icons.RIcons
import org.jetbrains.r.RBundle
import org.jetbrains.r.visualization.inlays.RClipboardUtils
import org.jetbrains.r.visualization.inlays.dataframe.DataFrame
import org.jetbrains.r.visualization.inlays.dataframe.columns.DoubleType
import org.jetbrains.r.visualization.inlays.dataframe.columns.IntType
import org.jetbrains.r.visualization.inlays.table.DataFrameColumnModel
import org.jetbrains.r.visualization.inlays.table.DoubleTableCellRenderer
import org.jetbrains.r.visualization.inlays.table.IntegerTableCellRenderer
import org.jetbrains.r.visualization.inlays.table.StringTableCellRenderer
import org.jetbrains.r.visualization.inlays.table.filters.gui.TableFilterHeader
import org.jetbrains.r.visualization.inlays.table.paging.TablePaginator
import org.jetbrains.r.visualization.ui.MaterialTable
import org.jetbrains.r.visualization.ui.MaterialTableUtils
import java.awt.BorderLayout
import java.awt.Event
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.io.BufferedWriter
import java.io.File
import javax.swing.*
import javax.swing.table.TableRowSorter

/**
 * Table page of notebook inlay component. Hold and serve table.
 * By default table has a context menu with "copy selection" action and toolbar with action "Save as tsv".
 */
class InlayTablePage : JPanel(BorderLayout()), ToolBarProvider {

  var onChange: (() -> Unit)? = null

  private val table = MaterialTable()
  val scrollPane = JBScrollPane(table)
  private var filterHeader: TableFilterHeader? = null
  private var paginator: TablePaginator? = null

  val preferredHeight: Int
    get() {
      return table.preferredSize.height
    }

  class TableCopyProvider(private val table: JBTable) : CopyProvider {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun performCopy(dataContext: DataContext) {
      val copySelectedToString: CharSequence = RClipboardUtils.copySelectedToString(table)
      CopyPasteManager.getInstance().setContents(TextTransferable(copySelectedToString))
    }

    override fun isCopyEnabled(dataContext: DataContext): Boolean {
      return table.selectedRowCount > 0
    }

    override fun isCopyVisible(dataContext: DataContext): Boolean {
      return true
    }
  }

  init {

    // Disposer.register(parent, disposable)

    table.putClientProperty("AuxEditorComponent", true)
    scrollPane.border = IdeBorderFactory.createBorder(SideBorder.RIGHT)
    scrollPane.viewportBorder = IdeBorderFactory.createBorder(SideBorder.BOTTOM or SideBorder.LEFT or SideBorder.RIGHT)

    setupTablePopupMenu(table)
    // setupCopySelectedAction(table)
    setupSelectAllAction(table)

    table.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (filterHeader == null && e.keyChar.isLetterOrDigit()) {
          addTableFilterHeader()
        }
      }
    })

    DataManager.registerDataProvider(table) { dataId ->
      if (PlatformDataKeys.COPY_PROVIDER.`is`(dataId)) TableCopyProvider(table) else null
    }
    add(scrollPane, BorderLayout.CENTER)
  }

  private fun setupTablePopupMenu(table: JTable) {
    val copyAll = JMenuItem(RBundle.message("inlay.table.copy.all"))
    copyAll.addActionListener { RClipboardUtils.copyAllToClipboard(table) }

    val copySelected = JMenuItem(RBundle.message("inlay.table.copy.selected"))
    copySelected.addActionListener { RClipboardUtils.copySelectedToClipboard(table) }

    val popupMenu = JPopupMenu()
    popupMenu.add(copyAll)
    popupMenu.add(copySelected)

    table.componentPopupMenu = popupMenu
  }

  private fun setupSelectAllAction(table: JTable) {
    val actionName = RBundle.message("action.name.table.select.all")
    val action = object : AbstractAction(actionName) {
      override fun actionPerformed(e: ActionEvent) {
        table.setRowSelectionInterval(0, table.rowCount - 1)
        table.setColumnSelectionInterval(0, table.columnCount - 1)
      }
    }
    table.actionMap.put(actionName, action)
    table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), actionName)
  }

  private inner class NumberComparator<T : Comparable<*>> : Comparator<T> {
    override fun compare(o1: T, o2: T): Int {
      return compareValues(o1, o2)
    }
  }

  /**
   * Sets the data frame for displaying in table.
   * also setups tableRowSorter with natural comparators for Int and double columns
   */
  fun setDataFrame(dataFrame: DataFrame) {
    table.columnModel = DataFrameColumnModel(dataFrame)
    table.model = org.jetbrains.r.visualization.inlays.table.DataFrameTableModel(dataFrame)

    for (i in dataFrame.getColumns().indices) {
      table.columnModel.getColumn(i).cellRenderer = when (dataFrame[i].type) {
        IntType -> IntegerTableCellRenderer()
        DoubleType -> DoubleTableCellRenderer()
        else -> StringTableCellRenderer()
      }
    }

    MaterialTableUtils.fitColumnsWidth(table)

    val tableRowSorter = TableRowSorter(table.model)
    tableRowSorter.sortsOnUpdates = true

    for ((i, column) in dataFrame.getColumns().withIndex()) {
      if (column.type is IntType) {
        tableRowSorter.setComparator(i, NumberComparator<Int>())
      }
      else if (column.type is DoubleType) {
        tableRowSorter.setComparator(i, NumberComparator<Double>())
      }
    }
    table.rowSorter = tableRowSorter
  }

  private fun addTableFilterHeader() {
    filterHeader = TableFilterHeader()
    filterHeader!!.installTable(table)
  }

  override fun createActions(): List<AnAction> {

    val actionSaveAsCsv = object : DumbAwareAction(RBundle.message("inlay.table.export.as.text"),
                                                   RBundle.message("inlay.table.export.as.description"),
                                                   AllIcons.ToolbarDecorator.Export) {
      override fun actionPerformed(e: AnActionEvent) {
        saveAsCsv(e.project ?: return)
      }
    }

    val filterTable = object : DumbAwareToggleAction(RBundle.message("inlay.table.filter.text"),
                                                     RBundle.message("inlay.table.filter.description"),
                                                     AllIcons.Actions.Find) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

      override fun isSelected(e: AnActionEvent): Boolean {
        return filterHeader != null
      }

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
          addTableFilterHeader()
        }
        else {
          filterHeader?.installTable(null)
          filterHeader = null
        }
      }
    }

    val paginateTable = object : DumbAwareToggleAction(RBundle.message("inlay.table.pagination.text"),
                                                       null,
                                                       RIcons.Table.Pagination) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

      override fun isSelected(e: AnActionEvent): Boolean {
        return paginator != null
      }

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
          paginator = TablePaginator()
          paginator!!.table = table
        }
        else {
          paginator?.table = null
          paginator = null
        }
      }
    }

    return listOf(actionSaveAsCsv, filterTable, paginateTable)
  }

  /** Save the file as tsv (tab separated values) via intellij SaveFileDialog. */
  private fun saveAsCsv(project: Project) {
    val descriptor = FileSaverDescriptor(RBundle.message("inlay.table.export.as.csv.text"), RBundle.message("inlay.table.export.as.csv.description")).apply {
      withExtensionFilter(RBundle.message("inlay.table.export.as.csv.label"), "csv", "tsv")
    }
    val chooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, this)
    val basePath = project.basePath ?: return
    val virtualBaseDir = LocalFileSystem.getInstance().findFileByIoFile(File(basePath))
    val fileWrapper = chooser.save(virtualBaseDir, "table.csv") ?: return

    fun saveSelection(out: BufferedWriter, cellBreak: String) {
      val selectedColumnCount = table.selectedColumnCount
      val selectedRowCount = table.selectedRowCount
      val selectedRows = table.selectedRows
      val selectedColumns = table.selectedColumns

      for (i in 0 until selectedRowCount) {
        for (j in 0 until selectedColumnCount) {
          out.write(RClipboardUtils.escape(table.getValueAt(selectedRows[i], selectedColumns[j])))

          if (j < selectedColumnCount - 1) {
            out.write(cellBreak)
          }
        }
        if (i < table.rowCount - 1) {
          out.append(RClipboardUtils.LINE_BREAK)
        }
      }
    }

    fun saveAll(out: BufferedWriter, cellBreak: String) {
      for (i in 0 until table.rowCount) {
        for (j in 0 until table.columnCount) {
          out.write(RClipboardUtils.escape(table.getValueAt(i, j)))

          if (j < table.columnCount - 1) {
            out.write(cellBreak)
          }
        }
        if (i < table.rowCount - 1) {
          out.append(RClipboardUtils.LINE_BREAK)
        }
      }
    }

    fileWrapper.file.bufferedWriter().use { out ->

      val cellBreak = if (fileWrapper.file.extension == "csv") ";" else "\t"

      if (table.selectedColumnCount == 0 || table.selectedRowCount == 0) {
        saveAll(out, cellBreak)
      }
      else {
        saveSelection(out, cellBreak)
      }
    }
  }
}
