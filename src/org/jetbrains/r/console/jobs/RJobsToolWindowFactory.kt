/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.ContentFactory
import org.jetbrains.r.RBundle

class RJobsToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
    val rJobsPanel = RJobPanel(project)
    val content = ContentFactory.getInstance().createContent(rJobsPanel, RBundle.message("jobs.panel.title"), false)
    rJobsPanel.jobsStatusCallback = { ongoing: Int, finished: Int, failed: Int ->
      content.displayName = buildDisplayName(ongoing, finished, failed)
    }
    runInEdt {
      toolWindow.contentManager.addContent(content)
    }
  }

  private fun buildDisplayName(ongoing: Int, finished: Int, failed: Int): String {
    val builder = StringBuilder()
    builder.append(RBundle.message("jobs.panel.title"))
    if (ongoing + finished + failed > 0) {
      builder.append(": ")
    }
    if (ongoing > 0) {
      builder.append(RBundle.message("jobs.panel.title.ongoing", ongoing))
      if (finished + failed > 0) {
        builder.append(", ")
      }
    }
    if (finished > 0) {
      builder.append(RBundle.message("jobs.panel.title.finished", finished))
      if (failed > 0) {
        builder.append(", ")
      }
    }
    if (failed > 0) {
      builder.append(RBundle.message("jobs.panel.title.failed", failed))
    }
    return builder.toString()
  }

  companion object {
    internal const val ID = "R_Jobs"

    fun focusOnJobs(project: Project) {
      getRJobsToolWindows(project)?.show(null)
    }

    private fun getRJobsToolWindows(project: Project): ToolWindow? {
      return ToolWindowManager.getInstance(project).getToolWindow(ID)
    }

    fun getJobsPanel(project: Project): RJobPanel? =
      getRJobsToolWindows(project)?.contentManager?.contents?.firstOrNull()?.component as? RJobPanel
  }
}
