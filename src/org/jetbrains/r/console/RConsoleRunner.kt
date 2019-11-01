// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.google.common.annotations.VisibleForTesting
import com.intellij.execution.ExecutionManager
import com.intellij.execution.Executor
import com.intellij.execution.console.ConsoleExecuteAction
import com.intellij.execution.console.ConsoleHistoryController
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ConsoleTitleGen
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.ide.CommonActionsManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.WaitForProgressToShow
import com.intellij.util.ui.UIUtil
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RFileType
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import org.jetbrains.r.run.graphics.RGraphicsRepository
import org.jetbrains.r.run.graphics.RGraphicsState
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.ui.RGraphicsToolWindowListener
import org.jetbrains.r.run.viewer.RViewerRepository
import org.jetbrains.r.run.viewer.RViewerState
import org.jetbrains.r.run.viewer.RViewerUtils
import org.jetbrains.r.run.viewer.ui.RViewerToolWindowListener
import org.jetbrains.r.settings.RGraphicsSettings
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

class RConsoleRunner(private val project: Project,
                     private val workingDir: String,
                     private val consoleTitle: String = RBundle.message("console.runner.default.title")) {
  private lateinit var consoleView: RConsoleView

  internal var hasPendingPrompt = false

  fun initAndRun(): Promise<RConsoleView> {
    val promise = AsyncPromise<RConsoleView>()
    RInterpreterManager.getInstance(project).initializeInterpreter().onSuccess {
      UIUtil.invokeLaterIfNeeded {
        RInteropUtil.runRWrapperAndInterop(project).onSuccess { rInterop ->
          val interpreterPath = RInterpreterManager.getInterpreter(project)?.interpreterPath ?: throw IllegalStateException(
            "Interpreter must be initlialized here")
          UIUtil.invokeLaterIfNeeded {
            consoleView = RConsoleView(rInterop, interpreterPath, project, consoleTitle)
            ProcessTerminatedListener.attach(rInterop.processHandler)
            rInterop.processHandler.addProcessListener(object : ProcessAdapter() {
              override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (outputType == ProcessOutputType.SYSTEM) {
                  consoleView.print(event.text, ConsoleViewContentType.SYSTEM_OUTPUT)
                }
              }

              override fun processTerminated(event: ProcessEvent) {
                finishConsole()
              }
            })
            ConsoleHistoryController(RConsoleRootType.instance, "", consoleView).install()

            // Setup console listener for graphics state
            val graphicsState = RGraphicsUtils.createGraphicsState(RGraphicsSettings.getScreenParameters(project))
            graphicsState.addListener(RGraphicsToolWindowListener(project))
            consoleView.addOnSelectListener {
              RGraphicsRepository.getInstance(project).setActiveState(graphicsState)
            }

            // Setup console listener for HTML viewer
            val viewerState = RViewerUtils.createViewerState()
            viewerState.addListener(RViewerToolWindowListener(project))
            consoleView.addOnSelectListener {
              RViewerRepository.getInstance(project).setActiveState(viewerState)
            }

            createContentDescriptorAndActions()
            promise.setResult(consoleView)
            consoleView.createDebuggerPanel()

            // Setup viewer handler
            runBackgroundableTask(RBundle.message("console.runner.initializing.viewer.title"), project, false) {
              val viewerHandler = UpdateViewerHandler(rInterop, viewerState)
              consoleView.executeActionHandler.addListener(viewerHandler)
            }

            // Setup custom graphical device (it's more time consuming so it should be the last one)
            runBackgroundableTask(RBundle.message("graphics.device.initializing.title"), project, false) {
              val graphicsHandler = UpdateGraphicsHandler(project, rInterop, graphicsState)
              consoleView.executeActionHandler.addListener(graphicsHandler)
            }
          }
        }.onError {
          showErrorMessage(project,  it.message ?: "Cannot find suitable rwrapper",  "Cannot run console")
          promise.setError(it)
        }
      }
    }
    return promise
  }

  private fun createContentDescriptorAndActions() {
    val executeAction = createConsoleExecAction()
    val interruptAction = RConsoleView.createInterruptAction(consoleView)
    val helpAction = CommonActionsManager.getInstance().createHelpAction("interactive_console")

    val actions = listOf(executeAction, interruptAction, helpAction)
    val actionsWhenRunning = actions.filter { it !== executeAction }.toTypedArray()
    val actionsWhenNotRunning = actions.filter { it !== interruptAction }.toTypedArray()
    val toolbarActions = object : ActionGroup() {
      override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return if (consoleView.isRunningCommand) {
          actionsWhenRunning
        } else {
          actionsWhenNotRunning
        }
      }
    }
    val actionToolbar = ActionManager.getInstance()
      .createActionToolbar(RBundle.message("console.runner.action.toolbar.place"), toolbarActions, false)

    val panel = JPanel(BorderLayout())
    panel.add(actionToolbar.component, BorderLayout.WEST)
    panel.add(consoleView.component, BorderLayout.CENTER)

    actionToolbar.setTargetComponent(panel)
    val title = ConsoleTitleGen(project, consoleTitle, false).makeTitle()
    val contentDescriptor = RunContentDescriptor(consoleView, consoleView.rInterop.processHandler, panel, title, RFileType.icon)

    contentDescriptor.setFocusComputable { consoleView.consoleEditor.contentComponent }
    contentDescriptor.isAutoFocusContent = true

    registerActionShortcuts(actions, consoleView.consoleEditor.component)
    registerActionShortcuts(actions, panel)
    RConsoleToolWindowFactory.addContent(project, contentDescriptor)
  }

  private fun createConsoleExecAction(): AnAction {
    val emptyAction = consoleView.executeActionHandler.emptyExecuteAction
    return object : ConsoleExecuteAction(consoleView, consoleView.executeActionHandler, emptyAction, consoleView.executeActionHandler) {
    }
  }


  private fun getExecutor(): Executor {
    return DefaultRunExecutor.getRunExecutorInstance()
  }

  private fun finishConsole() {
    consoleView.isEditable = false
  }

  private fun registerActionShortcuts(actions: List<AnAction>, component: JComponent) {
    for (action in actions) {
      action.registerCustomShortcutSet(action.shortcutSet, component)
    }
  }

  private fun showConsole(defaultExecutor: Executor, contentDescriptor: RunContentDescriptor) {
    ExecutionManager.getInstance(project).contentManager.showRunContent(defaultExecutor, contentDescriptor)
  }
}

@VisibleForTesting
internal class UpdateGraphicsHandler(
  private val project: Project,
  private val rInterop: RInterop,
  private val state: RGraphicsState
) : RConsoleExecuteActionHandler.Listener {

  private val tracedDirectory: File
    get() = state.tracedDirectory

  private var isLoaded: Boolean = false
  private var screenParameters: RGraphicsUtils.ScreenParameters? = null

  init {
    state.screenParameters.addListener { parameters ->
      screenParameters = parameters
      if (isLoaded) {
        rInterop.graphicsReset()
      }
      onReset()
    }
  }

  override fun onReset() {
    fun initializeDevice(): Boolean {
      val initProperties = RGraphicsUtils.calculateInitProperties(tracedDirectory.absolutePath, screenParameters)
      val graphicsInit = rInterop.graphicsInit(initProperties)
      return if (graphicsInit.stderr.isNotBlank()) {
        Logger.getInstance(javaClass).error(graphicsInit.stderr)
        false
      } else {
        true
      }
    }

    fun clearAllSnapshots() {
      rInterop.graphicsDump()
      tracedDirectory.listFiles { _, name -> name.endsWith("png") }?.let {
        for (file in it) {
          file.delete()
        }
      }
    }

    isLoaded = initializeDevice()
    if (isLoaded) {
      // On Windows a white snapshot will be created after very first user command.
      // To prevent this, we need to manually dump graphics device and delete this snapshot
      // TODO [mine]: this approach deletes all snapshots after every device reset which is awkward, should be replaced with a more fine-grained solution
      clearAllSnapshots()
    }
  }

  override fun onCommandExecuted() {
    if (isLoaded) {
      ApplicationManager.getApplication().executeOnPooledThread {
        rInterop.graphicsDump()
        state.update()
      }
    }
  }
}

@VisibleForTesting
internal class UpdateViewerHandler(
  rInterop: RInterop,
  private val state: RViewerState
) : RConsoleExecuteActionHandler.Listener {

  init {
    rInterop.htmlViewerInit(FileUtil.toSystemIndependentName(state.tracedFile.absolutePath))
  }

  override fun onCommandExecuted() {
    ApplicationManager.getApplication().executeOnPooledThread {
      state.update()
    }
  }
}

private fun showErrorMessage(project: Project, message: String, title: String) {
  val task = { Messages.showErrorDialog(project, message, title) }
  WaitForProgressToShow.runOrInvokeLaterAboveProgress(task, null, project)
}
