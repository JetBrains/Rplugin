/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.r.run.viewer.RViewerRepository

class RViewerToolWindow(project: Project) : SimpleToolWindowPanel(true, true) {
  private val viewerPanel = RViewerPanel()

  init {
    setContent(viewerPanel.component)
    RViewerRepository.getInstance(project).addUrlListener { url ->
      ApplicationManager.getApplication().invokeLater {
        refresh(url)
      }
    }
  }

  fun refresh(url: String?) {
    if (url != null) {
      viewerPanel.loadUrl(url)
    } else {
      viewerPanel.reset()
    }
  }
}