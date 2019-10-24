/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer.ui

import com.intellij.openapi.util.Disposer
import com.intellij.ui.javafx.JavaFxHtmlPanel
import icons.org.jetbrains.r.RBundle
import org.intellij.datavis.inlays.components.EmptyComponentPanel
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.run.viewer.RViewerUtils
import java.io.File
import java.net.URI
import javax.swing.JLabel

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

  val component = rootPanel.component

  fun loadUrl(url: String) {
    fun String.toQualifiedUrl(): String? {
      val qualifiedUrl = RViewerUtils.getQualifiedUrl(url)
      val path = URI.create(qualifiedUrl).path
      val file = File(path)
      return if (file.exists()) qualifiedUrl else null
    }

    url.toQualifiedUrl()?.let { openViewer(it) } ?: closeViewer(makeNoSuchFileText(url))
  }

  fun reset() {
    closeViewer(NO_CONTENT)
  }

  private fun openViewer(url: String) {
    closeViewer(LOADING)
    htmlPanel.load(url).onSuccess {
      rootPanel.contentComponent = htmlPanel.component
    }
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
  }

  companion object {
    private val LOADING = RBundle.message("viewer.panel.loading")
    private val NO_CONTENT = RBundle.message("viewer.panel.no.content")

    private fun makeNoSuchFileText(url: String): String {
      return RBundle.message("viewer.panel.no.such.file", url)
    }
  }
}
