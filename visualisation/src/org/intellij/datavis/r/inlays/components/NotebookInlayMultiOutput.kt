/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBScrollPane
import org.intellij.datavis.r.inlays.InlayDimensions
import org.intellij.datavis.r.inlays.InlayOutput
import org.intellij.datavis.r.inlays.MouseWheelUtils
import org.intellij.datavis.r.inlays.dataframe.DataFrameCSVAdapter
import org.intellij.datavis.r.inlays.runAsyncInlay
import org.intellij.datavis.r.ui.ToolbarUtil
import org.intellij.datavis.r.ui.UiCustomizer
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/** Page control with table and chart pages. */
class NotebookInlayMultiOutput(val editor: Editor, parent: Disposable) : NotebookInlayState() {

  private val mainPanel: JPanel

  private val scrollPane: JBScrollPane

  private val outputsPanel: JPanel

  var onChange: (() -> Unit)? = null

  private val disposable = Disposer.newDisposable()

  private val outputs: MutableSet<NotebookInlayState> = mutableSetOf()

  /** Total height of all outputs in the multi-output. */
  private var totalHeight = 0

  private val project = editor.project!!

  private fun NotebookInlayOutput.getToolbarPane(): ToolbarPane? = components.filterIsInstance<ToolbarPane>().first()

  init {
    Disposer.register(parent, disposable)

    layout = BorderLayout()

    mainPanel = JPanel(BorderLayout())
    add(mainPanel, BorderLayout.CENTER)

    outputsPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      background = UiCustomizer.instance.getTextOutputBackground(editor)
    }

    scrollPane = JBScrollPane(outputsPanel).apply {
      MouseWheelUtils.wrapMouseWheelListeners(this, parent)
      mainPanel.add(this, BorderLayout.CENTER)
      border = IdeBorderFactory.createEmptyBorder(Insets(0, 0, 0, 0))
    }

    mainPanel.add(createToolbar(), BorderLayout.LINE_END)
  }

  fun onOutputs(inlayOutputs: List<InlayOutput>) {
    outputs.clear()
    inlayOutputs.forEach { inlayOutput ->
      if (inlayOutput.type == "TABLE") {
        runAsyncInlay {
          val data = DataFrameCSVAdapter.fromCsvString(inlayOutput.data)
          invokeLater {
            NotebookInlayData(project, disposable, data).apply {
              addOutput(this)
              onHeightCalculated = ::updateHeight
              setDataFrame(data)
            }
          }
        }
      }
      else {
        NotebookInlayOutput(editor, disposable).apply {
          addData(inlayOutput.type, inlayOutput.data, inlayOutput.progressStatus)
          onHeightCalculated = ::updateHeight
          addOutput(this)
        }
      }
    }
  }

  private fun addOutput(output: NotebookInlayState) {
    outputs.add(output)
    outputsPanel.add(output)
  }

  private fun updateHeight(height: Int) {
    totalHeight += height
    if (totalHeight < InlayDimensions.multiOutputHeightThreshold) {
      onHeightCalculated?.invoke(totalHeight)
    } else {
      onHeightCalculated?.invoke(InlayDimensions.multiOutputDefaultHeight)
    }
  }

  override fun updateProgressStatus(progressStatus: InlayProgressStatus) {
    outputs.forEach { output ->
      output.updateProgressStatus(progressStatus)
    }
  }

  override fun clear() {
  }

  override fun getCollapsedDescription(): String {
    return "foooo"
  }

  private fun createClearAction(): AnAction {
    return ToolbarUtil.createAnActionButton<ClearOutputAction> { clearAction() }
  }

  private fun createToolbar(): JComponent {
    return ToolbarUtil.createEllipsisToolbar(listOf(createClearAction())).apply {
      isOpaque = true
      background = UiCustomizer.instance.getTextOutputBackground(editor)
    }
  }
}
