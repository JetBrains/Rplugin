/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.ToolWindowImpl
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.*
import com.intellij.util.ui.UIUtil.findComponentOfType
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise

class RConsoleToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun init(window: ToolWindow) {
    if (window is ToolWindowImpl) {
      window.contentManager.let { cm ->
        cm.addContentManagerListener(object : ContentManagerAdapter() {
          override fun selectionChanged(event: ContentManagerEvent) {
            findComponentOfType(cm.selectedContent?.component, RConsoleView::class.java)?.apply {
              onSelect()
            }
          }
        })
      }

      window.toolWindowManager.project.getUserData(QUEUE_KEY)?.let { actions ->
        synchronized(actions) {
          actions.forEach { action -> action(window) }
        }
      }
      window.toolWindowManager.project.putUserData(QUEUE_KEY, null)
    }
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    RConsoleManager.getInstance(project).runIfEmpty()
  }

  companion object {
    private val QUEUE_KEY: Key<MutableList<(ToolWindow) -> Unit>> = Key.create("org.jetbrains.r.console.RConsoleToolWindowQueue")
    private const val ID = "R Console"

    fun runWhenAvailable(project: Project, action: (ToolWindow) -> Unit): Promise<Unit> {
      val result = AsyncPromise<Unit>()
      getToolWindow(project)?.let { toolWindow ->
        action(toolWindow)
        return result.apply { setResult(Unit) }
      }
      val list = (project as UserDataHolderEx).putUserDataIfAbsent(QUEUE_KEY, mutableListOf())
      synchronized(list) {
        getToolWindow(project)?.let { toolWindow ->
          action(toolWindow)
          return result.apply { setResult(Unit) }
        }
        list.add {
          action(it)
          result.setResult(Unit)
        }
      }
      return result
    }

    fun addContentWhenAvailable(project: Project, contentDescriptor: RunContentDescriptor): Promise<Unit> {
      return runWhenAvailable(project) {
        addContent(it, contentDescriptor)
      }
    }

    fun show(project: Project) {
      getToolWindow(project)?.show { }
    }

    fun getToolWindow(project: Project): ToolWindow? {
      return ToolWindowManager.getInstance(project).getToolWindow(ID)
    }

    private fun addContent(toolWindow: ToolWindow, contentDescriptor: RunContentDescriptor) {
      toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
      val content = createContent(contentDescriptor)
      toolWindow.contentManager.addContent(content)
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