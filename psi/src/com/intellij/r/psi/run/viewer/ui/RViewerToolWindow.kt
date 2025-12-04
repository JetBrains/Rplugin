/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.run.viewer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.ui.SimpleToolWindowPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RViewerToolWindow(parentDisposable: Disposable) : SimpleToolWindowPanel(true, true) {
  private val viewerPanel = RViewerPanel(parentDisposable)

  init {
    setContent(viewerPanel.component)
  }

  suspend fun refreshFile(file: String?) {
    withContext(Dispatchers.EDT) {
      if (file != null) {
        viewerPanel.loadFile(file)
      }
      else {
        viewerPanel.reset()
      }
    }
  }

  suspend fun refreshUrl(url: String?) {
    withContext(Dispatchers.EDT) {
      if (url != null) {
        viewerPanel.loadUrl(url)
      }
      else {
        viewerPanel.reset()
      }
    }
  }
}