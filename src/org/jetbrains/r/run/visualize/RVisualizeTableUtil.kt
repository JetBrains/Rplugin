/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.notebooks.visualization.r.ui.MaterialTable
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.TableColumn
import kotlin.math.max

object RVisualizeTableUtil {
  private const val MAX_ITEMS_FOR_SIZE_CALCULATION = 256
  private val DEFAULT_ROW_HEIGHT = JBUI.scale(22)

  @JvmStatic
  fun showTable(project: Project, viewer: RDataFrameViewer, name: String) {
    invokeLater {
      val fileEditorManager = FileEditorManager.getInstance(project)
      fileEditorManager.openFiles.filterIsInstance<RTableVirtualFile>().firstOrNull { it.table.viewer === viewer }?.let {
        fileEditorManager.openFile(it, true)
        return@invokeLater
      }
      val page = RDataFrameTablePage(viewer)
      val rTableVirtualFile = RTableVirtualFile(page, name)
      fileEditorManager.openFile(rTableVirtualFile, true)[0]
    }
  }

  fun createMaterialTableFromViewer(viewer: RDataFrameViewer): MaterialTable {
    val columnModel = createColumnModel(viewer)
    val model = RDataFrameTableModel(viewer)
    val materialTable = RMaterialTable(model, columnModel)
    materialTable.setMaxItemsForSizeCalculation(MAX_ITEMS_FOR_SIZE_CALCULATION)
    val connect = ApplicationManager.getApplication().messageBus.connect(viewer)
    connect.subscribe(EditorColorsManager.TOPIC, EditorColorsListener { globalColorScheme ->
      globalColorScheme?.defaultBackground?.let { materialTable.background = it }
    })
    materialTable.rowSorter = RDataFrameRowSorter(model, materialTable)
    materialTable.rowHeight = max(DEFAULT_ROW_HEIGHT, materialTable.rowHeight)
    return materialTable
  }

  fun createColumnModel(viewer: RDataFrameViewer): DefaultTableColumnModel {
    val columnModel = DefaultTableColumnModel()
    for (i in 0 until viewer.nColumns) {
      val tableColumn = TableColumn(i)
      tableColumn.headerValue = viewer.getColumnName(i)
      columnModel.addColumn(tableColumn)
    }
    return columnModel
  }

  fun refreshTables(project: Project) {
    invokeLater {
      FileEditorManager.getInstance(project).openFiles.filterIsInstance<RTableVirtualFile>().forEach {
        val table = it.table
        if (table.autoRefresh) {
          table.refreshTable()
        }
      }
    }
  }
}
