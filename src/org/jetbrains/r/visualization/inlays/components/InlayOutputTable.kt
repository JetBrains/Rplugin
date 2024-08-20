package org.jetbrains.r.visualization.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import org.jetbrains.r.visualization.inlays.dataframe.DataFrameCSVAdapter

class InlayOutputTable(val parent: Disposable, editor: Editor)
  : InlayOutput(parent, editor, loadActions()) {

  private val inlayTablePage: InlayTablePage = InlayTablePage()

  init {
    toolbarPane.dataComponent = inlayTablePage
  }

  override fun clear() {}

  override fun addData(data: String, type: String) {
    val dataFrame = DataFrameCSVAdapter.fromCsvString(data)
    inlayTablePage.setDataFrame(dataFrame)
  }

  override fun scrollToTop() {}

  override fun getCollapsedDescription(): String = "Table output"

  override fun acceptType(type: String): Boolean = type == "TABLE"
}