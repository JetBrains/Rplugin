/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.r.RBundle
import org.jetbrains.r.actions.RActionUtil
import org.jetbrains.r.configuration.RSettingsProjectConfigurable
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterManagerImpl
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.settings.RSettings
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
          if (RSettings.getInstance(project).interpreterLocation == null) {
            ProgressManager.getInstance().runProcessWithProgressSynchronously( {
              RInterpreterUtil.suggestAllInterpreters(true, true).firstOrNull()?.let { interpreterInfo ->
                invokeAndWaitIfNeeded {
                  if (project.isDisposed) return@invokeAndWaitIfNeeded
                  RSettings.getInstance(project).interpreterLocation = interpreterInfo.interpreterLocation
                }
              }
            }, RBundle.message("console.starting.label.text"), false, project)
          }
          tryAddContent(toolWindow, project)
        }
        previousVisibility.set(visible)
      }
    })
    runInEdt {
      addCreateConsoleTabAction(toolWindow)
      addRenameConsoleDoubleClickAction(toolWindow)
    }
  }

  private fun addCreateConsoleTabAction(toolWindow: ToolWindow) {
    (toolWindow as? ToolWindowEx)?.setTabActions(
      object : AnAction(), DumbAware {
        private val addConsoleAction = ActionManager.getInstance().getAction("org.jetbrains.r.console.RConsoleAction")

        init {
          copyFrom(addConsoleAction)
          templatePresentation.icon = AllIcons.General.Add
        }

        override fun update(e: AnActionEvent) {
          addConsoleAction.update(e)
        }

        override fun actionPerformed(e: AnActionEvent) {
          RActionUtil.performDelegatedAction(addConsoleAction, e)
        }
      })
  }

  private fun addRenameConsoleDoubleClickAction(toolWindow: ToolWindow) {
    (toolWindow as? ToolWindowEx)?.setTabDoubleClickActions(listOf(RConsoleRenameAction()))
  }

  companion object {
    internal const val ID = "R Console"
    private val CONSOLE_CONTENT_KEY = Key.create<Unit>("org.jetbrains.r.console.content.console")
    private val CONSOLE_PLACEHOLDER_KEY = Key.create<Unit>("org.jetbrains.r.console.content.placeholder")

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

    fun addContent(project: Project, contentDescriptor: RunContentDescriptor) {
      val toolWindow = getRConsoleToolWindows(project) ?: throw IllegalStateException("R Console Tool Window doesn't exist")
      val contentManager = toolWindow.contentManager
      contentManager.contents.firstOrNull { it.displayName == NO_INTERPRETER_FOUND_DISPLAY_NAME }?.let {
        contentManager.removeContent(it, true)
      }
      val consoleCount = contentManager.contents.count { isConsole(it) }
      val content = createContent(contentDescriptor)
      Disposer.register(content, Disposable {
        if (contentManager.contents.count { isConsole(it) } == 0) {
          toolWindow.hide()
        }
      })
      content.setPreferredFocusedComponent(contentDescriptor.preferredFocusComputable)
      content.putUserData(CONSOLE_CONTENT_KEY, Unit)
      val indexOfPlaceholder = contentManager.contents.indexOfFirst { isPlaceholder(it) }
      if (indexOfPlaceholder >= 0) {
        val oldContent = contentManager.getContent(indexOfPlaceholder)!!
        contentManager.addContent(content, indexOfPlaceholder)
        contentManager.setSelectedContent(content)
        contentManager.removeContent(oldContent, true)
      }
      else {
        val lastConsoleIndex = contentManager.contents.indexOfLast { isConsole(it) }
        contentManager.addContent(content, lastConsoleIndex + 1)
      }
      contentManager.setSelectedContent(content)
      if (consoleCount == 0) {
        setAvailableForRToolWindows(project, true, Runnable {
          val rTools = ToolWindowManager.getInstance(project).getToolWindow(RToolWindowFactory.ID)
          rTools?.activate({
            rTools.hide { rTools.show { } }
          }, false)
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
      val rInterop = console.rInterop
      val interpreter = console.interpreter
      val workingDir = rInterop.workingDir.takeIf { it.isNotEmpty() } ?: interpreter.basePath
      rInterop.state.cancelStateUpdating()
      Disposer.dispose(rInterop)
      rInterop.executeOnTermination {
        invokeLater {
          RConsoleRunner(interpreter, workingDir, index).initAndRun().onProcessed { newConsole ->
            invokeLater {
              toolWindow.contentManager.removeContent(content, true)
              if (newConsole != null) {
                getConsoleContent(newConsole)?.let { newContent ->
                  toolWindow.show {
                    toolWindow.contentManager.setSelectedContent(newContent)
                  }
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

    fun addConsolePlaceholder(project: Project, contentIndex: Int? = null): Content? {
      val toolWindow = getRConsoleToolWindows(project) ?: return null
      val contentFactory = ContentFactory.SERVICE.getInstance()
      val panel = BorderLayoutPanel()
      panel.addToCenter(JBLabel(RBundle.message("console.starting.label.text")).apply {
        horizontalAlignment = SwingConstants.CENTER
        foreground = UIUtil.getInactiveTextColor()
      })
      val content = contentFactory.createContent(panel, RBundle.message("console.starting.title"), false)
      content.isCloseable = false
      content.putUserData(CONSOLE_PLACEHOLDER_KEY, Unit)
      val contentManager = toolWindow.contentManager
      if (contentIndex == null) {
        val lastConsoleIndex = contentManager.contents.indexOfLast { isConsole(it) }
        contentManager.addContent(content, lastConsoleIndex + 1)
      } else {
        contentManager.addContent(content, contentIndex)
      }
      contentManager.setSelectedContent(content)
      return content
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
        if (!RInterpreterManager.getInstance(project).hasInterpreter()) {
          val contentFactory = ContentFactory.SERVICE.getInstance()
          val console = contentFactory.createContent(createNoInterpreterConsoleView(project).component,
                                                     NO_INTERPRETER_FOUND_DISPLAY_NAME,
                                                     true)
          console.putUserData(CONSOLE_CONTENT_KEY, Unit)
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