// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.google.common.annotations.VisibleForTesting
import com.intellij.execution.Executor
import com.intellij.execution.console.ConsoleExecuteAction
import com.intellij.execution.console.ConsoleHistoryController
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.ide.CommonActionsManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.util.WaitForProgressToShow
import com.intellij.util.ui.UIUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.RFileType
import org.jetbrains.r.actions.RActionUtil
import org.jetbrains.r.actions.RPromotedAction
import org.jetbrains.r.actions.ToggleSoftWrapAction
import org.jetbrains.r.help.RWebHelpProvider
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import org.jetbrains.r.run.graphics.RGraphicsDevice
import org.jetbrains.r.run.graphics.RGraphicsRepository
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.ui.RGraphicsToolWindowListener
import org.jetbrains.r.settings.REditorSettings
import org.jetbrains.r.settings.RGraphicsSettings
import org.jetbrains.r.settings.RSettings
import org.jetbrains.r.util.RPathUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class RConsoleRunner(private val interpreter: RInterpreter,
                     private val workingDir: String = interpreter.basePath,
                     private val contentIndex: Int? = null) {
  private val project = interpreter.project
  private val consoleTitle = interpreter.suggestConsoleName(workingDir)
  private lateinit var consoleView: RConsoleView

  internal var hasPendingPrompt = false

  fun initAndRun(): Promise<RConsoleView> {
    val promise = AsyncPromise<RConsoleView>()
    checkRProfile().onSuccess {
      interpreter.prepareForExecution().onProcessed {
        UIUtil.invokeLaterIfNeeded {
          val placeholder = RConsoleToolWindowFactory.addConsolePlaceholder(project, contentIndex)
          RInteropUtil.runRWrapperAndInterop(interpreter, workingDir).onSuccess { rInterop ->
            initByInterop(rInterop, promise)
            rInterop.state.scheduleSkeletonUpdate()
          }.onError {
            showErrorMessage(project, it.message ?: "Cannot find suitable rwrapper", "Cannot run console")
            promise.setError(it)
            UIUtil.invokeLaterIfNeeded {
              placeholder?.manager?.removeContent(placeholder, true)
            }
          }
        }
      }
    }.onError {
      promise.setError(it)
    }
    return promise
  }

  internal fun initByInterop(rInterop: RInterop, promise: AsyncPromise<RConsoleView> = AsyncPromise()): Promise<RConsoleView> {
    UIUtil.invokeLaterIfNeeded {
      consoleView = RConsoleView(rInterop, consoleTitle)
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

      runAsync {
        rInterop.setWorkingDir(workingDir)
        // Setup console listener for graphics device
        val graphicsDevice = if (ApplicationManager.getApplication().isUnitTestMode) {
          null
        } else {
          val screenParameters = RGraphicsSettings.getScreenParameters(project)
          RGraphicsUtils.createGraphicsDevice(rInterop, screenParameters.dimension, screenParameters.resolution).apply {
            configuration = configuration.copy(screenParameters = screenParameters)
            addListener(RGraphicsToolWindowListener(project))
          }.also {
            consoleView.addOnSelectListener { RGraphicsRepository.getInstance(project).setActiveDevice(it) }
          }
        }

        UIUtil.invokeLaterIfNeeded {
          createContentDescriptorAndActions()
          consoleView.createDebuggerPanel()

          // Setup custom graphical device (it's more time consuming so it should be the last one)
          graphicsDevice?.let { device ->
            runBackgroundableTask(RBundle.message("graphics.device.initializing.title"), project, false) {
              val graphicsHandler = UpdateGraphicsHandler(device)
              consoleView.executeActionHandler.addListener(graphicsHandler)
            }
          }

          // setResult also will trigger onSuccess handlers, but we don't wont to run them on EDT
          runAsync { promise.setResult(consoleView) }
        }
      }
    }
    return promise
  }

  private fun createContentDescriptorAndActions() {
    val historyController = ConsoleHistoryController(RConsoleRootType.instance, "", consoleView)
    // lets trigger getComponent to create the Editor
    consoleView.component
    historyController.install()
    val executeAction = createConsoleExecAction()
    val interruptAction = createInterruptAction(consoleView)
    val eofAction = createEofAction(consoleView)
    val helpAction = CommonActionsManager.getInstance().createHelpAction(RWebHelpProvider.R_CONSOLE_ID)
    val historyAction = historyController.browseHistory
    val toggleSoftWrap = createToggleSoftWrapAction(consoleView)

    val actions = listOf(executeAction, interruptAction, eofAction, historyAction,
                         createSetCurrentDirectory(),
                         createRestartRAction(),
                         Separator(),
                         toggleSoftWrap, helpAction)
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
    actionToolbar.setTargetComponent(consoleView.consoleEditor.contentComponent)

    val panel = JPanel(BorderLayout())
    panel.add(actionToolbar.component, BorderLayout.WEST)
    panel.add(consoleView.component, BorderLayout.CENTER)

    actionToolbar.setTargetComponent(panel)

    if (ApplicationManager.getApplication().isUnitTestMode) return
    // do not create RunContentDescriptor for tests
    val contentDescriptor = RunContentDescriptor(consoleView, consoleView.rInterop.processHandler, panel, consoleTitle, RFileType.icon)
    contentDescriptor.setFocusComputable { consoleView.consoleEditor.contentComponent }
    contentDescriptor.isAutoFocusContent = true
    registerActionShortcuts(actions, consoleView.consoleEditor.component)
    registerActionShortcuts(actions, panel)
    RConsoleToolWindowFactory.addContent(project, contentDescriptor)
  }

  private fun createSetCurrentDirectory(): AnAction {
    return ActionManager.getInstance().getAction("org.jetbrains.r.console.RConsoleView.RSetCurrentDirectoryFromEditor")
  }

  private fun createRestartRAction(): AnAction {
    return ActionManager.getInstance().getAction("org.jetbrains.r.console.RConsoleView.RestartRAction")
  }

  private fun createToggleSoftWrapAction(console: RConsoleView): ToggleSoftWrapAction =
    object : ToggleSoftWrapAction() {
      private var isSelected: Boolean = REditorSettings.useSoftWrapsInConsole

      init { updateEditors() }

      override fun isSelected(e: AnActionEvent): Boolean = isSelected

      private fun updateEditors() {
        console.editor.getSettings().setUseSoftWraps(isSelected)
        console.consoleEditor.getSettings().setUseSoftWraps(isSelected)
      }

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        isSelected = state
        updateEditors()
        REditorSettings.useSoftWrapsInConsole = isSelected
      }
    }


  private fun createInterruptAction(console: RConsoleView): AnAction =
    object : AnAction(), RPromotedAction {
      private val action = ActionManager.getInstance().getAction(RConsoleView.INTERRUPT_ACTION_ID).also { copyFrom(it) }

      override fun actionPerformed(e: AnActionEvent) {
        RActionUtil.performDelegatedAction(action, e)
      }

      override fun update(e: AnActionEvent) {
        action.update(createEvent(e))
      }

      private fun createEvent(e: AnActionEvent): AnActionEvent =
        AnActionEvent.createFromInputEvent(e.inputEvent, "", e.presentation,
                                            SimpleDataContext.getSimpleContext(
                                              mapOf(RConsoleView.R_CONSOLE_DATA_KEY.name to console), e.dataContext))
    }

  private fun createEofAction(console: RConsoleView): AnAction =
    object : AnAction(), RPromotedAction {
      private val action = ActionManager.getInstance().getAction(RConsoleView.EOF_ACTION_ID).also { copyFrom(it) }

      override fun actionPerformed(e: AnActionEvent) {
        RActionUtil.performDelegatedAction(action, e)
      }

      override fun update(e: AnActionEvent) {
        action.update(createEvent(e))
      }

      private fun createEvent(e: AnActionEvent): AnActionEvent =
        AnActionEvent.createFromInputEvent(e.inputEvent, "", e.presentation,
                                           SimpleDataContext.getSimpleContext(
                                             mapOf(RConsoleView.R_CONSOLE_DATA_KEY.name to console), e.dataContext))
    }


  private fun createConsoleExecAction(): AnAction {
    val emptyAction = consoleView.executeActionHandler.emptyExecuteAction

    // use unique name for statistics
    class RConsoleExecuteActionHandler : ConsoleExecuteAction(consoleView,
                                                              consoleView.executeActionHandler,
                                                              emptyAction,
                                                              consoleView.executeActionHandler)

    return RConsoleExecuteActionHandler()
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
    RunContentManager.getInstance(project).showRunContent(defaultExecutor, contentDescriptor)
  }

  // R-509: Newline is required in the end of .Rprofile
  private fun fixRProfile(): Promise<Unit> {
    return runAsync {
      val file = interpreter.findFileByPathAtHost(RPathUtil.join(workingDir, ".Rprofile")) ?: return@runAsync resolvedPromise()
      val needFix = runReadAction {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runReadAction false
        val text = document.text
        text.isNotBlank() && !text.endsWith("\n")
      }
      if (needFix) {
        WriteCommandAction.runWriteCommandAction(project) {
          val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runWriteCommandAction
          document.insertString(document.textLength, "\n")
        }
        interpreter.prepareForExecution()
      } else {
        resolvedPromise()
      }
    }.thenAsync {
      it
    }
  }

  private fun checkRProfile(): Promise<Unit> {
    if (RSettings.getInstance(project).disableRprofile) return resolvedPromise()
    return fixRProfile().thenAsync {
      runAsync<Unit> {
        RInterpreterUtil.validateRProfile(project, interpreter.interpreterLocation, workingDir, true)
      }
    }
  }
}

@VisibleForTesting
internal class UpdateGraphicsHandler(private val device: RGraphicsDevice) : RConsoleExecuteActionHandler.Listener {
  override fun onReset() {
    device.reset()
  }

  override fun beforeExecution() {
    val parameters = RGraphicsSettings.getScreenParameters(device.project)
    device.setParametersAsync(parameters)
  }

  override fun onCommandExecuted() {
    device.update()
  }
}

private fun showErrorMessage(project: Project, message: String, title: String) {
  val task = { Messages.showErrorDialog(project, message, title) }
  WaitForProgressToShow.runOrInvokeLaterAboveProgress(task, null, project)
}
