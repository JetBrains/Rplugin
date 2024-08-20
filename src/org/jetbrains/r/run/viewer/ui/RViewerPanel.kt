/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.RBundle
import org.jetbrains.r.run.viewer.RViewerUtils
import org.jetbrains.r.visualization.inlays.components.EmptyComponentPanel
import java.io.File
import java.net.URI
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.border.EmptyBorder

class RViewerPanel(disposable: Disposable) {
  private val label = JLabel(NO_CONTENT, JLabel.CENTER)
  private val rootPanel = EmptyComponentPanel(label)
  private val jbBrowser: JBCefBrowser by lazy { JBCefBrowser().also { Disposer.register(disposable, it) } }

  private val multilineLabel = MultilineLabel().apply {
    border = EmptyBorder(JBUI.insets(5))
  }

  private val multilineScrollPane = JBScrollPane(multilineLabel)

  val component = rootPanel.component

  fun loadFile(filePath: String): Promise<Unit> {
    val qualifiedUrl = RViewerUtils.getQualifiedUrl(filePath)
    val path = URI.create(qualifiedUrl).path
    val file = File(path)
    return if (file.exists()) {
      closeViewer(LOADING)
      loadHtmlOrText(file, qualifiedUrl)
    } else {
      closeViewer(makeNoSuchFileText(filePath))
      resolvedPromise()
    }
  }

  fun loadUrl(url: String): Promise<Unit> {
    jbBrowser.loadURL(url)
    if (rootPanel.contentComponent != jbBrowser.component) {
      rootPanel.contentComponent = jbBrowser.component
    }
    return resolvedPromise()
  }

  private fun loadHtmlOrText(file: File, qualifiedUrl: String): Promise<Unit> {
    return if (file.extension.isNotEmpty()) {
      jbBrowser.loadURL(qualifiedUrl)
      if (rootPanel.contentComponent != jbBrowser.component) {
        rootPanel.contentComponent = jbBrowser.component
      }
      resolvedPromise()
    } else {
      multilineLabel.text = file.readText()
      rootPanel.contentComponent = multilineScrollPane
      resolvedPromise()
    }
  }

  fun reset() {
    closeViewer(NO_CONTENT)
  }

  private fun closeViewer(text: String) {
    label.text = text
    rootPanel.contentComponent = null
  }

  private class MultilineLabel(text: String = "") : JTextArea(text) {
    init {
      setCursor(null)  // Note: `cursor = null` raises Type Mismatch warning
      isEditable = false
      isOpaque = false
      wrapStyleWord = true
      lineWrap = true
    }
  }

  companion object {
    private val LOADING = RBundle.message("viewer.panel.loading")
    private val NO_CONTENT = RBundle.message("viewer.panel.no.content")

    private fun makeNoSuchFileText(url: String): String {
      return RBundle.message("viewer.panel.no.such.file", url)
    }
  }
}
