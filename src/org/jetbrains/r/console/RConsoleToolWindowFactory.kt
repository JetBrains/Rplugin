/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.r.RBundle
import org.jetbrains.r.configuration.RSettingsProjectConfigurable
import org.jetbrains.r.console.jobs.RJobPanel
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterManagerImpl
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingConstants

class RConsoleToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val rConsoleManager = RConsoleManager.getInstance(project)
    rConsoleManager.registerContentManager(toolWindow.contentManager)
    toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
    toolWindow.setToHideOnEmptyContent(true)
    project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
      val previousVisibility = AtomicBoolean()
      override fun stateChanged() {
        val visible = toolWindow.isVisible
        if (!previousVisibility.get() && visible) {
          tryAddContent(toolWindow, project)
        }
        previousVisibility.set(visible)
      }
    })
    tryAddContent(toolWindow, project)
    addJobsPanel(toolWindow, project)
  }

  private fun addJobsPanel(toolWindow: ToolWindow, project: Project) {
    val rJobsPanel = RJobPanel(project)
    val content = ContentFactory.SERVICE.getInstance().createContent(rJobsPanel, "Jobs", false)
    content.putUserData(JOBS_CONTENT_KEY, Unit)
    content.executionId
    content.isCloseable = false
    toolWindow.contentManager.addContent(content)
  }

  companion object {
    internal const val ID = "R Console"
    private val JOBS_CONTENT_KEY = Key.create<Unit>("org.jetbrains.r.console.content.job")
    private val CONSOLE_CONTENT_KEY = Key.create<Unit>("org.jetbrains.r.console.content.console")
    private val CONSOLE_PLACEHOLDER_KEY = Key.create<Unit>("org.jetbrains.r.console.content.placeholder")

    private fun isJob(content: Content): Boolean = content.getUserData(JOBS_CONTENT_KEY) != null
    private fun isPlaceholder(content: Content): Boolean = content.getUserData(CONSOLE_PLACEHOLDER_KEY) != null
    internal fun isConsole(content: Content): Boolean = content.getUserData(CONSOLE_CONTENT_KEY) != null

    fun focusOnCurrentConsole(project: Project) {
      val rConsoleToolWindows = getRConsoleToolWindows(project)
      rConsoleToolWindows?.show(null)
      val currentConsoleOrNull = RConsoleManager.getInstance(project).currentConsoleOrNull ?: return
      rConsoleToolWindows?.contentManager?.let {contentManager ->
        contentManager.contents.firstOrNull { isConsole(it) && UIUtil.isAncestor(it.component, currentConsoleOrNull) }?.let { content ->
         contentManager.setSelectedContent(content)
        }
      }
    }

    fun getRConsoleToolWindows(project: Project): ToolWindow? {
      return ToolWindowManager.getInstance(project).getToolWindow(ID)
    }

    fun getJobsPanel(project: Project): RJobPanel? =
      getRConsoleToolWindows(project)?.contentManager?.contents?.firstOrNull { isJob(it) }?.component as RJobPanel?

    fun addContent(project: Project, contentDescriptor: RunContentDescriptor) {
      val toolWindow = getRConsoleToolWindows(project) ?: throw IllegalStateException("R Console Tool Window doesn't exist")
      val contentManager = toolWindow.contentManager
      contentManager.contents.firstOrNull { it.displayName == NO_INTERPRETER_FOUND_DISPLAY_NAME }?.let {
        contentManager.removeContent(it, true)
      }
      val consoleCount = contentManager.contents.count { isConsole(it) }
      val content = createContent(contentDescriptor)
      content.putUserData(CONSOLE_CONTENT_KEY, Unit)
      val indexOfPlaceholder = contentManager.contents.indexOfFirst { isPlaceholder(it) }
      if (indexOfPlaceholder >= 0) {
        contentManager.removeContent(contentManager.getContent(indexOfPlaceholder)!!, true)
        contentManager.addContent(content, indexOfPlaceholder)
      }
      else {
        val lastConsoleIndex = contentManager.contents.indexOfLast { isConsole(it) }
        contentManager.addContent(content, lastConsoleIndex + 1)
      }
      contentManager.setSelectedContent(content)
      if (consoleCount == 0) {
        setAvailableForRToolWindows(project, true, Runnable {
          val rTools = ToolWindowManager.getInstance(project).getToolWindow(RToolWindowFactory.ID)
          rTools?.activate {
            rTools.hide { rTools.show { } }
          }
        })
      }
    }

    fun setAvailableForRToolWindows(project: Project, isAvailable: Boolean, runnable: Runnable? = null) {
      invokeLater {
        ToolWindowManager.getInstance(project).getToolWindow(RToolWindowFactory.ID)?.setAvailable(isAvailable, runnable)
      }
    }

    fun restartConsole(console: RConsoleView) {
      val toolWindow = getRConsoleToolWindows(console.project) ?: return
      val content = getConsoleContent(console) ?: return
      val index = toolWindow.contentManager.getIndexOfContent(content)
      val title = content.toolwindowTitle
      Disposer.dispose(console.rInterop)
      console.rInterop.executeOnTermination {
        invokeLater {
          RConsoleRunner(console.project, console.workingDirectory, title, index).initAndRun().then {
            invokeLater {
              toolWindow.contentManager.removeContent(content, true)
              getConsoleContent(it)?.let { newContent ->
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

    fun addConsolePlaceholder(project: Project, contentIndex: Int? = null) {
      val toolWindow = getRConsoleToolWindows(project) ?: return
      val contentFactory = ContentFactory.SERVICE.getInstance()
      val panel = BorderLayoutPanel()
      panel.addToCenter(JBLabel("Starting console...").apply {
        horizontalAlignment = SwingConstants.CENTER
        foreground = UIUtil.getInactiveTextColor()
      })
      val content = contentFactory.createContent(panel, "Starting console", false)
      content.isCloseable = false
      content.putUserData(CONSOLE_PLACEHOLDER_KEY, Unit)
      val contentManager = toolWindow.contentManager
      if (contentIndex == null) {
        val lastConsoleIndex = contentManager.contents.indexOfLast { isConsole(it) }
        contentManager.addContent(content, lastConsoleIndex + 1)
      } else {
        contentManager.addContent(content, contentIndex)
      }
    }

    private fun createNoInterpreterConsoleView(project: Project): ConsoleView =
      ConsoleViewImpl(project, true).apply {
       print(RBundle.message("console.no.interpreter.error") + "\n", ConsoleViewContentType.ERROR_OUTPUT)
       printHyperlink(RBundle.message("console.no.interpreter.setup.interpreter") + "\n") {
          ShowSettingsUtil.getInstance().showSettingsDialog(project, RSettingsProjectConfigurable::class.java)
        }
        printHyperlink(RBundle.message("console.no.interpreter.download.interpreter") + "\n") {
          RInterpreterManagerImpl.openDownloadRPage()
        }
      }

    private fun tryAddContent(toolWindow: ToolWindow, project: Project) {
      if (toolWindow.contentManager.contents.count { isConsole(it) } == 0) {
        if (RInterpreterManager.getInstance(project).interpreterPath.isBlank()) {
          val contentFactory = ContentFactory.SERVICE.getInstance()
          val console = contentFactory.createContent(createNoInterpreterConsoleView(project).component, NO_INTERPRETER_FOUND_DISPLAY_NAME, true)
          console.isCloseable = false
          toolWindow.contentManager.addContent(console, 0)
        } else {
          RConsoleManager.getInstance(project).currentConsoleAsync
        }
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

private val NO_INTERPRETER_FOUND_DISPLAY_NAME = RBundle.message("console.no.interpreter.error")