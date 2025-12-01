/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.ui

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.r.psi.RPluginCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.r.console.RConsoleManagerImpl
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory

/**
 * Use it to show tool window with [displayName] without stealing focus from current console
 */
class RNonStealingToolWindowInvoker(private val project: Project, private val displayName: String) {
  private val toolWindow: ToolWindow by lazy {
    ToolWindowManager.getInstance(project).getToolWindow(RToolWindowFactory.ID)!!
  }

  fun showWindow() {
    RPluginCoroutineScope.getScope(project).launch(Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()) {
      val contentComponent = RConsoleManagerImpl.getInstance(project).currentConsoleOrNull?.currentEditor?.contentComponent
      val hasFocus = contentComponent?.hasFocus()
      toolWindow.show(null)
      val content = RToolWindowFactory.findContent(project, displayName)
      toolWindow.contentManager.setSelectedContent(content)
      if (hasFocus == true) {
        contentComponent.grabFocus()
      }
    }
  }
}
