/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.run.visualize

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.icons.AllIcons
import com.intellij.ide.CopyProvider
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.TextTransferable
import icons.VisualizationIcons
import org.intellij.datavis.inlays.table.ClipboardUtils
import org.intellij.datavis.inlays.table.filters.gui.TableFilterHeader
import org.intellij.datavis.ui.MaterialTableUtils
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.run.visualize.RDataFrameRowSorter
import org.jetbrains.r.run.visualize.RDataFrameTablePaginator
import org.jetbrains.r.run.visualize.RDataFrameViewer
import org.jetbrains.r.run.visualize.RVisualizeTableUtil
import java.awt.BorderLayout
import java.awt.Event
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.BufferedWriter
import java.io.File
import javax.swing.*
import kotlin.math.min

class RDataFrameTablePage(val viewer: RDataFrameViewer) : JPanel(BorderLayout()) {
  private val table = RVisualizeTableUtil.createMaterialTableFromViewer(viewer)

  private val scrollPane = JBScrollPane(table)
  private var paginator: RDataFrameTablePaginator? = null
  private var filterHeader: TableFilterHeader? = null
  private lateinit var filterTableButton: ActionButton

  val preferredHeight: Int
    get() = table.preferredSize.height

  class TableCopyProvider(private val table: JBTable) : CopyProvider {
    override fun performCopy(dataContext: DataContext) {
      val copySelectedToString: String? = ClipboardUtils.copySelectedToString(table)
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
      MaterialTableUtils.fitColumnsWidth(table, maxRows = FIT_WIDTH_MAX_ROWS)
    }
  }

  private fun setupTablePopupMenu() {
    val filterByValue = JMenuItem("Filter by value")
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

    val copyAll = JMenuItem("Copy all")
    copyAll.addActionListener { ClipboardUtils.copyAllToClipboard(table) }

    val copySelected = JMenuItem("Copy selected")
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
        popupMenu.show(table, e.point.x, e.point.y)
      }
    })
  }

  private fun setupSelectAllAction() {
    val actionName = "TABLE_SELECT_ALL"
    val action = object : AbstractAction(actionName) {
      override fun actionPerformed(e: ActionEvent) {
        table.setRowSelectionInterval(0, table.rowCount - 1)
        table.setColumnSelectionInterval(0, table.columnCount - 1)
      }
    }
    table.actionMap.put(actionName, action)
    table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), actionName)
  }

  private fun addTableFilterHeader() {
    if (filterHeader != null) return
    filterHeader = TableFilterHeader(RDataFrameFiltersHandler()).also {
      it.isAdaptiveChoices = false
      it.table = table
    }
  }

  private fun createActionsPanel() {
    val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    fun createButton(action: AnAction): ActionButton {
      val button = ActionButton(action, action.templatePresentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
      actionsPanel.add(button)
      return button
    }

    val actionSaveAsCsv = object : DumbAwareAction("Export As", "Export as csv/tsv.", AllIcons.ToolbarDecorator.Export) {
      override fun actionPerformed(e: AnActionEvent) {
        saveAsCsv()
      }
    }
    createButton(actionSaveAsCsv)

    val filterTable = object : DumbAwareToggleAction(FILTER_TOOLTIP_ESCAPED, "Filter", AllIcons.Actions.Find) {
      override fun isSelected(e: AnActionEvent) = filterHeader != null

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
          addTableFilterHeader()
        } else {
          filterHeader?.table = null
          filterHeader = null
        }
      }
    }
    filterTableButton = createButton(filterTable)

    val paginateTable = object : DumbAwareToggleAction("Pagination", "Pagination", VisualizationIcons.TABLE_PAGINATION) {
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

    add(actionsPanel, BorderLayout.NORTH)
  }

  /** Save the file as tsv (tab separated values) via intellij SaveFileDialog. */
  private fun saveAsCsv() {
    val descriptor = FileSaverDescriptor(
      "Export as tsv",
      "Exports the selected range or whole table is nothing is selected as csv or tsv file.",
      "csv", "tsv"
    )
    val chooser: FileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, this)
    val virtualBaseDir = LocalFileSystem.getInstance().findFileByIoFile(File(ProjectManager.getInstance().openProjects[0].basePath!!))
    val fileWrapper = chooser.save(virtualBaseDir, "table.csv") ?: return

    fun saveSelection(out: BufferedWriter, cellBreak: String) {
      val selectedColumnCount = table.selectedColumnCount
      val selectedRowCount = table.selectedRowCount
      val selectedRows = table.selectedRows
      val selectedColumns = table.selectedColumns

      for (i in 0 until selectedRowCount) {
        for (j in 0 until selectedColumnCount) {
          table.getValueAt(selectedRows[i], selectedColumns[j])?.let { out.write(ClipboardUtils.escape(it)) }

          if (j < selectedColumnCount - 1) {
            out.write(cellBreak)
          }
        }
        if (i < table.rowCount - 1) {
          out.append(ClipboardUtils.LINE_BREAK)
        }
      }
    }

    fun saveAll(out: BufferedWriter, cellBreak: String) {
      for (i in 0 until table.rowCount) {
        for (j in 0 until table.columnCount) {
          table.getValueAt(i, j)?.let { out.write(ClipboardUtils.escape(it)) }

          if (j < table.columnCount - 1) {
            out.write(cellBreak)
          }
        }
        if (i < table.rowCount - 1) {
          out.append(ClipboardUtils.LINE_BREAK)
        }
      }
    }

    fileWrapper.file.bufferedWriter().use { out ->
      val cellBreak = if (fileWrapper.file.extension == "csv") ";" else "\t"
      if (table.selectedColumnCount == 0 || table.selectedRowCount == 0) {
        saveAll(out, cellBreak)
      } else {
        saveSelection(out, cellBreak)
      }
    }
  }

  companion object {
    private const val FIT_WIDTH_MAX_ROWS = 1024

    private val FILTER_TOOLTIP_ESCAPED = RDataFrameTablePage::class.java.getResource("/visualizer/TableViewFilterTooltip.html")
                                           ?.readText()?.replace("&", "&&")
                                           ?.replace("_", "__") ?: "Filter"
  }
}
