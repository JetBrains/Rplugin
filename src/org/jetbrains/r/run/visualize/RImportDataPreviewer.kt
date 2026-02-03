/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.codeInsight.hint.HintUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.visualization.inlays.components.EmptyComponentPanel
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBInsets
import org.jetbrains.r.visualization.ui.MaterialTable
import org.jetbrains.r.visualization.ui.MaterialTableUtils
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

internal class RImportDataPreviewer(private val parent: Disposable, emptyComponent: JComponent, private val statusComponent: JComponent) {
  private val loadingPanel = JBLoadingPanel(BorderLayout(), parent).apply {
    startLoading()
  }

  private val rootPanel = EmptyComponentPanel(emptyComponent)

  @Volatile
  private var currentViewer: RDataFrameViewer? = null

  val component = rootPanel.component

  val hasPreview: Boolean
    get() = currentViewer != null

  fun showLoading() {
    closePreview()
    rootPanel.contentComponent = loadingPanel
  }

  fun showPreview(viewer: RDataFrameViewer, errorCount: Int) {
    closePreview()
    val component = createViewerComponent(viewer, errorCount)
    rootPanel.contentComponent = component
    Disposer.register(parent, viewer)
    currentViewer = viewer
  }

  fun closePreview() {
    rootPanel.contentComponent = null
    currentViewer?.let { viewer ->
      Disposer.dispose(viewer)
      currentViewer = null
    }
  }

  private fun createViewerComponent(viewer: RDataFrameViewer, errorCount: Int): JComponent {
    val scrollPane = JBScrollPane(createTableFrom(viewer))
    return JPanel(GridBagLayout()).apply {
      addWithYWeight(scrollPane, 1.0)
      if (errorCount > 0) {
        addWithYWeight(createErrorBar(errorCount), 0.0)
      }
      addWithYWeight(statusComponent, 0.0)
    }
  }

  private fun createErrorBar(errorCount: Int): JComponent {
    return JPanel().apply {
      background = HintUtil.getErrorColor()
      border = JBEmptyBorder(ERROR_LABEL_INSETS)
      layout = BoxLayout(this, BoxLayout.LINE_AXIS)
      val errorText = createParsingErrorsMessage(errorCount)
      val errorLabel = JLabel(errorText).apply {
        font = font.deriveFont(Font.BOLD)
      }
      add(errorLabel)
    }
  }

  private fun createTableFrom(viewer: RDataFrameViewer): MaterialTable {
    return RVisualizeTableUtil.createMaterialTableFromViewer(viewer).apply {
      MaterialTableUtils.fitColumnsWidth(this)
      rowSorter = null
    }
  }
}

private val ERROR_LABEL_INSETS = JBInsets(6, 8, 6, 8)

private fun JPanel.addWithYWeight(component: JComponent, yWeight: Double) {
  val constraints = GridBagConstraints().apply {
    fill = GridBagConstraints.BOTH
    gridy = componentCount
    weighty = yWeight
    weightx = 1.0
  }
  add(component, constraints)
}

private fun createParsingErrorsMessage(errorCount: Int): String {
  return RBundle.message("import.data.dialog.preview.parsing.errors", errorCount)
}
