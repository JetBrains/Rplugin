/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer.ui

import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.javafx.JavaFxHtmlPanel
import com.intellij.util.ui.JBUI
import org.intellij.datavis.inlays.components.EmptyComponentPanel
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.RBundle
import org.jetbrains.r.run.viewer.RViewerUtils
import java.io.File
import java.net.URI
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.border.EmptyBorder

class RViewerPanel {
  private val label = JLabel(NO_CONTENT, JLabel.CENTER)
  private val rootPanel = EmptyComponentPanel(label)

  private val htmlPanel: ExtendedJavaFxHtmlPanel by lazy {
    // Lazy evaluation prevents JavaFxHtmlPanel ctor from throwing
    // "Write-unsafe context!" exception
    ExtendedJavaFxHtmlPanel().also {
      Disposer.get("ui")?.let { uiParent ->
        Disposer.register(uiParent, it)
      }
    }
  }

  private val multilineLabel = MultilineLabel().apply {
    border = EmptyBorder(JBUI.insets(5))
  }

  private val multilineScrollPane = JBScrollPane(multilineLabel)

  val component = rootPanel.component

  fun loadUrl(url: String): Promise<Unit> {
    val qualifiedUrl = RViewerUtils.getQualifiedUrl(url)
    val path = URI.create(qualifiedUrl).path
    val file = File(path)
    return if (file.exists()) {
      closeViewer(LOADING)
      loadHtmlOrText(file, qualifiedUrl)
    } else {
      closeViewer(makeNoSuchFileText(url))
      resolvedPromise()
    }
  }

  private fun loadHtmlOrText(file: File, qualifiedUrl: String): Promise<Unit> {
    return if (file.extension.isNotEmpty()) {
      htmlPanel.load(qualifiedUrl).onSuccess {
        rootPanel.contentComponent = htmlPanel.component
      }
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

  private class ExtendedJavaFxHtmlPanel : JavaFxHtmlPanel() {
    fun load(url: String): Promise<Unit> {
      return AsyncPromise<Unit>().also {
        runInPlatformWhenAvailable {
          webViewGuaranteed.engine.load(url)
          it.setResult(Unit)
        }
      }
    }

    fun loadText(text: String): Promise<Unit> {
      return AsyncPromise<Unit>().also {
        runInPlatformWhenAvailable {
          webViewGuaranteed.engine.loadContent(text, "text/plain")
          it.setResult(Unit)
        }
      }
    }
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
