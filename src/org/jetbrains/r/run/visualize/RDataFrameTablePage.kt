/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.icons.AllIcons
import com.intellij.ide.CopyProvider
import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.TextTransferable
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.plugins.notebooks.visualization.VisualisationIcons
import org.jetbrains.plugins.notebooks.visualization.r.inlays.ClipboardUtils
import org.jetbrains.plugins.notebooks.visualization.r.inlays.table.filters.gui.TableFilterHeader
import org.jetbrains.plugins.notebooks.visualization.r.ui.MaterialTableUtils
import org.jetbrains.r.RBundle
import java.awt.BorderLayout
import java.awt.Event
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import javax.swing.*
import kotlin.math.min

class RDataFrameTablePage(val viewer: RDataFrameViewer) : JPanel(BorderLayout()) {
  private val table = RVisualizeTableUtil.createMaterialTableFromViewer(viewer)
  private val tableModel = table.model as RDataFrameTableModel

  private val scrollPane = JBScrollPane(table)
  private var paginator: RDataFrameTablePaginator? = null
  private var filterHeader: TableFilterHeader? = null
  private var filtersHandler: RDataFrameFiltersHandler? = null
  private lateinit var filterTableButton: ActionButton

  val preferredHeight: Int
    get() = table.preferredSize.height
  var autoRefresh = viewer.canRefresh

  class TableCopyProvider(private val table: JBTable) : CopyProvider {
    override fun performCopy(dataContext: DataContext) {
      val copySelectedToString: CharSequence = ClipboardUtils.copySelectedToString(table)
      CopyPasteManager.getInstance().setContents(TextTransferable(copySelectedToString))
    }

    override fun isCopyEnabled(dataContext: DataContext) = table.selectedRowCount > 0

    override fun isCopyVisible(dataContext: DataContext) = true
  }

  init {
    table.putClientProperty("AuxEditorComponent", true)
    scrollPane.border = IdeBorderFactory.createBorder(SideBorder.RIGHT)
    scrollPane.viewportBorder = IdeBorderFactory.createBorder(SideBorder.BOTTOM or SideBorder.LEFT or SideBorder.RIGHT)
    scrollPane.columnHeader = JViewport().also { it.view = table.tableHeader }

    setupTablePopupMenu()
    setupSelectAllAction()
    setupClearSelectionAction()

    DataManager.registerDataProvider(table) { dataId ->
      if (PlatformDataKeys.COPY_PROVIDER.`is`(dataId)) TableCopyProvider(table) else null
    }

    add(scrollPane, BorderLayout.CENTER)

    createActionsPanel()

    var loadedPromise = resolvedPromise<Unit>()
    for (i in 0 until min(viewer.nRows, FIT_WIDTH_MAX_ROWS)) {
      loadedPromise = loadedPromise.thenAsync { viewer.ensureLoaded(i, 0) }
    }
    loadedPromise.then {
      invokeLater {
        MaterialTableUtils.fitColumnsWidth(table, maxRows = FIT_WIDTH_MAX_ROWS)
      }
    }
  }

  private fun setupTablePopupMenu() {
    val filterByValue = JMenuItem(RBundle.message("dataframe.viewer.menu.item.filter.by.value"))
    filterByValue.addActionListener {
      if (filterHeader == null) {
        filterTableButton.click()
      }
      val col = table.selectedColumn
      val row = table.selectedRow
      if (col != -1 && row != -1) {
        val value = table.model.getValueAt(row, col)
        if (value == null) {
          filterHeader?.getFilterEditor(col)?.content = "_"
        } else {
          filterHeader?.getFilterEditor(col)?.content = "=$value"
        }
      }
    }

    val copyAll = JMenuItem(RBundle.message("dataframe.viewer.menu.copy.all"))
    copyAll.addActionListener { ClipboardUtils.copyAllToClipboard(table) }

    val copySelected = JMenuItem(RBundle.message("dataframe.viewer.menu.copy.selected"))
    copySelected.addActionListener { ClipboardUtils.copySelectedToClipboard(table) }

    val popupMenu = JPopupMenu()
    popupMenu.add(filterByValue)
    popupMenu.add(copyAll)
    popupMenu.add(copySelected)

    table.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        if (e == null) return
        if (e.mouseButton != MouseButton.Right) return
        val row = table.rowAtPoint(e.point)
        val col = table.columnAtPoint(e.point)
        if (row == -1 || col == -1) return
        table.setRowSelectionInterval(row, row)
        table.setColumnSelectionInterval(col, col)
        JBPopupMenu.showByEvent(e, popupMenu)
      }
    })
  }

  private fun setupSelectAllAction() {
    val actionName = "TABLE_SELECT_ALL" //NON-NLS
    val action = object : AbstractAction(actionName) {
      override fun actionPerformed(e: ActionEvent) {
        table.setRowSelectionInterval(0, table.rowCount - 1)
        table.setColumnSelectionInterval(0, table.columnCount - 1)
      }
    }
    table.actionMap.put(actionName, action)
    table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), actionName)
  }

  private fun setupClearSelectionAction() {
    val actionName = "TABLE_CLEAR_SELECTION" //NON-NLS
    val action = object : AbstractAction(actionName) {
      override fun actionPerformed(e: ActionEvent) {
        table.clearSelection()
      }
    }
    table.actionMap.put(actionName, action)
    table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionName)
  }

  private fun addTableFilterHeader() {
    if (filterHeader != null) return
    filterHeader = TableFilterHeader(RDataFrameFiltersHandler().also { filtersHandler = it }).also {
      it.isAdaptiveChoices = false
      it.table = table
    }
  }

  private fun removeTableFilterHeader() {
    filterHeader?.table = null
    filterHeader = null
    filtersHandler = null
  }

  private fun createActionsPanel() {
    val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    fun createButton(action: AnAction): ActionButton {
      val button = ActionButton(action, action.templatePresentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
      actionsPanel.add(button)
      return button
    }

    val actionSaveAsCsv = object : DumbAwareAction(RBundle.message("action.dataframe.viewer.export.as.text"),
                                                   RBundle.message("action.dataframe.viewer.export.as.description"), AllIcons.ToolbarDecorator.Export) {
      override fun actionPerformed(e: AnActionEvent) {
        saveAsCsv()
      }
    }
    createButton(actionSaveAsCsv)
    table.selectionModel.addListSelectionListener {
      val presentation = actionSaveAsCsv.templatePresentation
      if (table.selectedColumnCount == 0 || table.selectedRowCount == 0) {
        presentation.text = RBundle.message("action.dataframe.viewer.export.as.text")
        presentation.description = RBundle.message("action.dataframe.viewer.export.as.description")
      } else {
        presentation.text = RBundle.message("action.dataframe.viewer.export.selection.as.text")
        presentation.description = RBundle.message("action.dataframe.viewer.export.selection.as.description")
      }
    }

    val filterTable = object : DumbAwareToggleAction(FILTER_TOOLTIP_ESCAPED, RBundle.message("action.dataframe.viewer.filter.description"), AllIcons.Actions.Find) {
      override fun isSelected(e: AnActionEvent) = filterHeader != null

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
          addTableFilterHeader()
        } else {
          removeTableFilterHeader()
        }
      }
    }
    filterTableButton = createButton(filterTable)

    val paginateTable = object : DumbAwareToggleAction(RBundle.message("action.dataframe.viewer.pagination.name"),
                                                       RBundle.message("action.dataframe.viewer.pagination.description"),
                                                       VisualisationIcons.Table.Pagination) {
      override fun isSelected(e: AnActionEvent): Boolean {
        return paginator != null
      }

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
          if (paginator == null) {
            paginator = RDataFrameTablePaginator(table.rowSorter as RDataFrameRowSorter, this@RDataFrameTablePage)
          }
        } else {
          paginator?.cleanUp()
          paginator = null
        }
      }
    }
    createButton(paginateTable)

    val autoRefreshAction = object : DumbAwareToggleAction(RBundle.message("action.dataframe.viewer.auto.refresh.name"),
                                                           RBundle.message("action.dataframe.viewer.auto.refresh.description"),
                                                           AllIcons.Actions.Refresh) {
      override fun isSelected(e: AnActionEvent): Boolean {
        return autoRefresh
      }

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        autoRefresh = state
        if (state) {
          refreshTable()
        }
      }

      override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = viewer.canRefresh
      }
    }
    createButton(autoRefreshAction)

    add(actionsPanel, BorderLayout.NORTH)
  }

  /** Save the file as tsv (tab separated values) via intellij SaveFileDialog. */
  private fun saveAsCsv() {
    val descriptor = FileSaverDescriptor(
      RBundle.message("dialog.title.dataframe.viewer.export.as"),
      RBundle.message("dialog.title.dataframe.viewer.export.as.description"),
      "csv", "tsv"
    )
    val chooser: FileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, this)
    val virtualBaseDir = LocalFileSystem.getInstance().findFileByIoFile(File(ProjectManager.getInstance().openProjects[0].basePath!!))
    val fileWrapper = chooser.save(virtualBaseDir, "table.csv") ?: return

    fun save(onlySelected: Boolean, out: BufferedWriter, cellBreak: String, escaper: (String) -> String, pi: ProgressIndicator) {
      val rows: Iterable<Int>
      val columns: Iterable<Int>
      val rowCount: Int
      val columnCount: Int
      if (onlySelected) {
        rows = table.selectedRows.asIterable()
        rowCount = table.selectedRowCount
        columns = table.selectedColumns.asIterable()
        columnCount = table.selectedColumnCount
      } else {
        rowCount = table.rowCount
        rows = 0 until rowCount
        columnCount = table.columnCount
        columns = 0 until columnCount
      }

      columns.forEachIndexed { j, column ->
        tableModel.viewer.getColumnName(column).let { out.write(escaper(it)) }
        if (j < columnCount - 1) out.write(cellBreak)
        pi.checkCanceled()
      }
      rows.forEachIndexed { i, row ->
        out.append(ClipboardUtils.LINE_BREAK)
        columns.forEachIndexed { j, column ->
          tableModel.viewer.ensureLoaded(row, column).blockingGet(Int.MAX_VALUE)
          tableModel.viewer.getValueAt(row, column)?.let { out.write(escaper(it.toString())) }
          if (j < columnCount - 1) out.write(cellBreak)
          pi.checkCanceled()
        }
        pi.fraction = (i + 1).toDouble() / rowCount
      }
    }

    runBackgroundableTask(RBundle.message("data.frame.export.title", fileWrapper.file.path), viewer.project) { pi ->
      try {
        fileWrapper.file.bufferedWriter().use { out ->
          val cellBreak: String
          val escaper: (String) -> String
          if (fileWrapper.file.extension == "csv") {
            cellBreak = ","
            val regex = Regex("[,;\"'\\t\\n ]")
            escaper = { s ->
              val quoted = s.contains(regex)
              if (quoted) "\"${s.replace("\"", "\"\"")}\"" else s
            }
          } else {
            cellBreak = "\t"
            escaper = { s ->
              val result = StringBuilder()
              s.forEach { c ->
                when (c) {
                  '\n' -> result.append("\\n")
                  '\t' -> result.append("\\t")
                  '\r' -> result.append("\\r")
                  '\\' -> result.append("\\\\")
                  else -> result.append(c)
                }
              }
              result.toString()
            }
          }
          if (table.selectedColumnCount == 0 || table.selectedRowCount == 0) {
            save(false, out, cellBreak, escaper, pi)
          } else {
            save(true, out, cellBreak, escaper, pi)
          }
        }
      } catch (e: IOException) {
        val details = e.message?.takeIf { it.isNotEmpty() }?.let { ":\n$it" }.orEmpty()
        Notification("RDataFrameViewer", RBundle.message("data.frame.viewer.error.title"), RBundle.message("data.frame.export.error.message") + details, NotificationType.ERROR)
          .notify(viewer.project)
      }
    }
  }

  fun refreshTable() {
    val oldColumns = List(viewer.nColumns) { viewer.getColumnName(it) }
    viewer.refresh().onSuccess { refreshed ->
      if (!refreshed) return@onSuccess
      val rowSorter = table.rowSorter as RDataFrameRowSorter
      rowSorter.updatesSuspended = true
      try {
        val newColumns = List(viewer.nColumns) { viewer.getColumnName(it) }
        val columnMapping = getColumnMapping(oldColumns, newColumns)
        val newSortKeys = rowSorter.sortKeys.mapNotNull {
          columnMapping[it.column]?.let { newColumn ->
            RowSorter.SortKey(newColumn, it.sortOrder)
          }
        }
        val newFilters = filterHeader?.let { header ->
          val filters = MutableList(newColumns.size) { "" }
          columnMapping.forEachIndexed { old, new ->
            if (new != null) {
              filters[new] = (header.getFilterEditor(old).content as? String).orEmpty()
            }
          }
          filters
        }

        removeTableFilterHeader()

        table.columnModel = RVisualizeTableUtil.createColumnModel(viewer)
        (table.model as RDataFrameTableModel).apply {
          fireTableStructureChanged()
          fireTableDataChanged()
        }
        rowSorter.restore()

        if (newFilters != null) {
          addTableFilterHeader()
          newFilters.forEachIndexed { index, it -> filterHeader?.getFilterEditor(index)?.content = it }
          filtersHandler?.updateTableFilter()
        }

        rowSorter.setSortKeys(newSortKeys.takeIf { it.isNotEmpty() }?.toMutableList())
        paginator?.updateShownRange()
      } finally {
        rowSorter.updatesSuspended = false
      }
      rowSorter.update()

      repaint()
      filterTableButton.update()
    }
  }

  companion object {
    private const val FIT_WIDTH_MAX_ROWS = 1024

    private val FILTER_TOOLTIP_ESCAPED = RDataFrameTablePage::class.java.getResource("/visualizer/TableViewFilterTooltip.html")
                                           ?.readText()?.replace("&", "&&")
                                           ?.replace("_", "__") ?: "Filter"

    private fun getColumnMapping(oldColumns: List<String>, newColumns: List<String>): List<Int?> {
      val newColumnsMap = newColumns.mapIndexed { index, it -> it to index }.reversed().toMap()
      val mapping = MutableList<Int?>(oldColumns.size) { null }
      val used = MutableList(newColumns.size) { false }
      oldColumns.forEachIndexed { index, it ->
        newColumnsMap[it]?.let {
          newIndex -> mapping[index] = newIndex
          used[newIndex] = true
        }
      }
      for (i in 0 until min(oldColumns.size, newColumns.size)) {
        if (mapping[i] == null && !used[i]) {
          mapping[i] = i
        }
      }
      return mapping
    }
  }
}
