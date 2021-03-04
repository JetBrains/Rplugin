/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote.filesystem

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.remote.RRemoteInterpreterImpl
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class RRemoteHostViewManager(private val project: Project) {
  private val contents = mutableMapOf<RRemoteInterpreterImpl, Pair<Content, RRemoteHostViewPanel>>()

  init {
    ROpenedRemoteFilesState.getInstance(project).initialize()
  }

  fun addInterpreter(interpreter: RRemoteInterpreterImpl) {
    invokeLater {
      if (interpreter in contents) return@invokeLater
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(
        RRemoteHostToolWindowFactory.ID) ?: return@invokeLater
      val toolWindowPanel = RRemoteHostViewPanel(project, interpreter)
      val content = ContentFactory.SERVICE.getInstance().createContent(
        toolWindowPanel, interpreter.remoteHost.presentableName, true)
      Disposer.register(content, toolWindowPanel)
      contents[interpreter] = content to toolWindowPanel
      toolWindow.contentManager.addContent(content)
      toolWindow.setAvailable(true) {
        toolWindow.show()
      }
      if (contents.keys.count { it.remoteHost == interpreter.remoteHost } == 1) {
        ROpenedRemoteFilesState.getInstance(project).remoteHostAdded(interpreter.remoteHost)
      }

      val refreshScheduled = AtomicBoolean(false)
      interpreter.addFsNotifierListenerForHost(listOf(interpreter.basePath), content) {
        if (refreshScheduled.compareAndSet(false, true)) {
          AppExecutorUtil.getAppScheduledExecutorService().schedule({
            invokeLater {
              toolWindowPanel.refresh()
              refreshScheduled.set(false)
            }
          }, REFRESH_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS)
        }
      }
    }
  }

  fun removeInterpreter(interpreter: RRemoteInterpreterImpl) {
    invokeLater {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(
        RRemoteHostToolWindowFactory.ID) ?: return@invokeLater
      val (content, _) = contents.remove(interpreter) ?: return@invokeLater
      toolWindow.contentManager.removeContent(content, true)
      toolWindow.isAvailable = toolWindow.contentManager.contentCount > 0
      if (contents.keys.none { it.remoteHost == interpreter.remoteHost }) {
        ROpenedRemoteFilesState.getInstance(project).remoteHostRemoved(interpreter.remoteHost)
      }
    }
  }

  fun closeExtraViews() {
    contents.keys
      .filter { it != RInterpreterManager.getInterpreterOrNull(project) }
      .forEach(this::removeInterpreter)
  }

  companion object {
    private const val REFRESH_DELAY_MILLISECONDS = 1000L

    fun getInstance(project: Project) = project.service<RRemoteHostViewManager>()
  }
}

class RRemoteHostToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun init(toolWindow: ToolWindow) {
    toolWindow.isAvailable = false
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
  }

  companion object {
    const val ID = "R Remote Host"
  }
}
