/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.r.console.RConsoleManager

/**
 * Use it to show tool window with [windowId] without stealing focus from current console
 */
class RNonStealingToolWindowInvoker(private val project: Project, private val windowId: String) {
  private val toolWindow: ToolWindow by lazy {
    ToolWindowManager.getInstance(project).getToolWindow(windowId)
  }

  fun showWindow() {
    ApplicationManager.getApplication().invokeLater {
      val contentComponent = RConsoleManager.getInstance(project).currentConsoleOrNull?.currentEditor?.contentComponent
      val hasFocus = contentComponent?.hasFocus()
      toolWindow.show(null)
      if (hasFocus == true) {
        contentComponent.grabFocus()
      }
    }
  }
}
