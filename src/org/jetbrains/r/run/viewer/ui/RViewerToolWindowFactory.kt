/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.r.console.RConsoleManager

class RViewerToolWindowFactory : ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager
    val content = contentManager.factory.createContent(RViewerToolWindow(project), null, false)
    contentManager.addContent(content)
  }

  override fun shouldBeAvailable(project: Project) = false

  companion object {
    const val ID = "R HTML Viewer"
  }
}