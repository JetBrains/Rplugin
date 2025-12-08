/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.project.Project
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import com.intellij.r.psi.run.graphics.RGraphicsCompletedUpdate
import com.intellij.r.psi.run.graphics.RGraphicsUpdate
import org.jetbrains.r.run.ui.RNonStealingToolWindowInvoker

class RGraphicsToolWindowListener(project: Project) : (RGraphicsUpdate) -> Unit {
  private val invoker = RNonStealingToolWindowInvoker(project, RToolWindowFactory.PLOTS)

  override fun invoke(update: RGraphicsUpdate) {
    if (update is RGraphicsCompletedUpdate && update.outputs.isEmpty()) {
      return
    }
    invoker.showWindow()
  }
}