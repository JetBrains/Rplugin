/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory

class RConsoleToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val rConsoleManager = RConsoleManager.getInstance(project)
    rConsoleManager.registerContentManager(toolWindow.contentManager)
    toolWindow.setToHideOnEmptyContent(true)
    setAvailableForRToolWindows(project, true)
    project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
      override fun stateChanged() {
        if (toolWindow.isVisible) {
          rConsoleManager.currentConsoleAsync
        }
      }
    })
    ToolWindowManager.getInstance(project).getToolWindow(RToolWindowFactory.ID)?.show {  }
    rConsoleManager.currentConsoleAsync
  }

  companion object {
    internal const val ID = "R Console"

    fun show(project: Project) {
      getRConsoleToolWindows(project)?.show(null)
    }

    fun getRConsoleToolWindows(project: Project): ToolWindow? {
      return ToolWindowManager.getInstance(project).getToolWindow(ID)
    }

    fun addContent(project: Project, contentDescriptor: RunContentDescriptor, contentIndex: Int? = null) {
      val toolWindow = getRConsoleToolWindows(project) ?: throw IllegalStateException("R Console Tool Window doesn't exist")
      toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
      toolWindow.setToHideOnEmptyContent(true)
      val content = createContent(contentDescriptor)
      if (contentIndex == null) {
        toolWindow.contentManager.addContent(content)
      } else {
        toolWindow.contentManager.addContent(content, contentIndex)
      }
    }

    fun setAvailableForRToolWindows(project: Project, isAvailable: Boolean) {
      ToolWindowManager.getInstance(project).getToolWindow(RToolWindowFactory.ID)?.setAvailable(isAvailable, null)
    }

    fun restartConsole(console: RConsoleView) {
      val toolWindow = getRConsoleToolWindows(console.project) ?: return
      val content = getConsoleContent(console) ?: return
      val index = toolWindow.contentManager.getIndexOfContent(content)
      val title = content.toolwindowTitle
      Disposer.dispose(console.rInterop)
      console.rInterop.executeOnTermination {
        invokeLater {
          toolWindow.contentManager.removeContent(content, true)
          RConsoleRunner(console.project, console.workingDirectory, title, index).initAndRun().then {
            getConsoleContent(it)?.let { newContent ->
              invokeLater {
                toolWindow.show {
                  toolWindow.contentManager.setSelectedContent(newContent)
                }
              }
            }
          }
        }
      }
    }

    fun getConsoleContent(console: RConsoleView): Content? {
      val toolWindow = getRConsoleToolWindows(console.project) ?: return null
      return toolWindow.contentManager.contents.firstOrNull {
        UIUtil.findComponentOfType(it.component, RConsoleView::class.java) == console
      }
    }

    private fun createContent(contentDescriptor: RunContentDescriptor): Content {
      val panel = SimpleToolWindowPanel(false, true)

      val content = ContentFactory.SERVICE.getInstance().createContent(panel, contentDescriptor.displayName, false)
      content.isCloseable = true

      resetContent(contentDescriptor, panel, content)

      return content
    }

    private fun resetContent(contentDescriptor: RunContentDescriptor, panel: SimpleToolWindowPanel, content: Content) {
      val oldDescriptor = if (content.disposer is RunContentDescriptor) content.disposer as RunContentDescriptor else null
      if (oldDescriptor != null) Disposer.dispose(oldDescriptor)

      panel.setContent(contentDescriptor.component)

      content.component = panel
      content.setDisposer(contentDescriptor)
      content.preferredFocusableComponent = contentDescriptor.component
    }
  }
}