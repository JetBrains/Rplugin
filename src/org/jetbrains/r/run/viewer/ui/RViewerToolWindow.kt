/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class RViewerToolWindow(parentDisposable: Disposable) : SimpleToolWindowPanel(true, true) {
  private val viewerPanel = RViewerPanel(parentDisposable)

  init {
    setContent(viewerPanel.component)
  }

  fun refresh(url: String?): Promise<Unit> {
    return if (url != null) {
      viewerPanel.loadUrl(url)
    } else {
      viewerPanel.reset()
      resolvedPromise()
    }
  }
}