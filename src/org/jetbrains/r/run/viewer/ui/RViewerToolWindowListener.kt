/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer.ui

import com.intellij.openapi.project.Project
import org.jetbrains.r.run.ui.RNonStealingToolWindowInvoker
import org.jetbrains.r.run.viewer.RViewerState

class RViewerToolWindowListener(project: Project) : RViewerState.Listener {
  private val invoker = RNonStealingToolWindowInvoker(project, RViewerToolWindow.TOOL_WINDOW_ID)

  override fun onCurrentChange(newUrl: String) {
    invoker.showWindow()
  }

  override fun onReset() {
    // Nothing to do here
  }
}
