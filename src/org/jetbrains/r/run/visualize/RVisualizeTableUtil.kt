/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import icons.org.jetbrains.r.run.visualize.RDataFrameTablePage
import org.intellij.datavis.ui.MaterialTable
import org.jetbrains.builtInWebServer.ConsoleManager
import org.jetbrains.r.console.RConsoleManager
import java.awt.EventQueue
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.TableColumn

object RVisualizeTableUtil {
  private const val DEFAULT_ROW_HEIGHT = 20

  @JvmStatic
  fun showTable(project: Project, viewer: RDataFrameViewer, name: String) {
    EventQueue.invokeLater {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(RTableViewToolWindowFactory.ID)
      val contentManager = toolWindow.contentManager
      contentManager.contents.forEach {
        if ((it.component as? RDataFrameTablePage)?.viewer === viewer) {
          contentManager.setSelectedContent(it)
          it.displayName = name
          return@invokeLater
        }
      }
      val page = RDataFrameTablePage(viewer)
      val content = contentManager.factory.createContent(page, name, true)
      viewer.registerDisposable(content)
      contentManager.addContent(content)
      toolWindow.show(null)
      contentManager.setSelectedContent(content)
    }
  }

  fun createMaterialTableFromViewer(viewer: RDataFrameViewer): MaterialTable {
    val columnModel = DefaultTableColumnModel()
    for (i in 0 until viewer.nColumns) {
      val tableColumn = TableColumn(i)
      tableColumn.headerValue = viewer.getColumnName(i)
      columnModel.addColumn(tableColumn)
    }
    val model = RDataFrameTableModel(viewer)
    val materialTable = MaterialTable(model, columnModel)
    materialTable.rowSorter = RDataFrameRowSorter(model, materialTable)
    materialTable.rowHeight = DEFAULT_ROW_HEIGHT
    return materialTable
  }
}

class RTableViewToolWindowFactory : ToolWindowFactory {

  override fun shouldBeAvailable(project: Project) = false

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {}

  companion object {
    const val ID = "R Table View"
  }
}

