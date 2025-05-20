package org.jetbrains.r.visualization.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import org.jetbrains.r.visualization.inlays.InlayOutputData
import org.jetbrains.r.visualization.inlays.MouseWheelUtils
import org.jetbrains.r.visualization.inlays.dataframe.DataFrameCSVAdapter

class InlayOutputTable(val parent: Disposable, editor: Editor)
  : InlayOutput(editor, loadActions()) {

  private val inlayTablePage: InlayTablePage = InlayTablePage()

  init {
    toolbarPane.dataComponent = inlayTablePage
    MouseWheelUtils.wrapMouseWheelListeners(inlayTablePage.scrollPane, parent)
  }

  override fun clear() {}

  fun addData(data: InlayOutputData.CsvTable) {
    val dataFrame = DataFrameCSVAdapter.fromCsvString(data.text)
    inlayTablePage.setDataFrame(dataFrame)
  }

  override fun scrollToTop() {}

  override fun getCollapsedDescription(): String = "Table output"

  override fun acceptType(type: String): Boolean = type == "TABLE"
}