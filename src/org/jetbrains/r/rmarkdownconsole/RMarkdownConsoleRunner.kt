/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdownconsole

import com.intellij.execution.ExecutionManager
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes.STDOUT
import com.intellij.execution.runners.ConsoleTitleGen
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.r.rendering.settings.RMarkdownSettings
import java.awt.BorderLayout
import javax.swing.JPanel

class RMarkdownConsoleRunner(private val project : Project,
                             private val workingDir: String,
                             private val consoleTitle: String = "RMarkdown Console") {
  private lateinit var consoleView : RMarkdownConsoleView
  private lateinit var processHandler: OSProcessHandler
  private lateinit var rMarkdownConsolePanel : JPanel
  private lateinit var title : String
  private lateinit var contentDescriptor: RunContentDescriptor
  private val knitOutputListener = KnitrOutputListener()
  private var currentRenderFilePath: String = ""
  private var isInterrupted = false

  private inner class KnitrOutputListener : ProcessAdapter() {
    private var outputFile : String? = null
    private var exitCode = -1
    private val OUTPUT_KEY = " --output "
    var onKnitrEnds: (()->Unit)? = null

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
      super.onTextAvailable(event, outputType)
      if (outputType == STDOUT && event.text.isNotBlank() && event.text.split(OUTPUT_KEY).size == 2) {
        outputFile = event.text.split(OUTPUT_KEY).lastOrNull()?.substringBefore(" ")?.trim()
      }
    }

    override fun processTerminated(event: ProcessEvent) {
      super.processTerminated(event)
      exitCode = event.exitCode
      if (exitCode != 0 && !isInterrupted) {
        renderingErrorNotification("Unexpected error occurred during render")
      } else if (!isInterrupted){
        RMarkdownSettings.getInstance(project).state.setProfileLastOutput(currentRenderFilePath,  outputFile.orEmpty())
      } else {
        reportRenderingInterrupted()
        isInterrupted = false
      }
      onKnitrEnds?.invoke()
    }

    fun getResultFileName() : String {
      if (exitCode == 0 && outputFile != null) {
        return outputFile ?: ""
      }
      return ""
    }
  }

  fun getResultPath() = knitOutputListener.getResultFileName()

  private fun createContentDescriptor() {
    rMarkdownConsolePanel = JPanel(BorderLayout())
    rMarkdownConsolePanel.add(consoleView.component, BorderLayout.CENTER)
    title = ConsoleTitleGen(project, consoleTitle, false).makeTitle()
    contentDescriptor = RunContentDescriptor(consoleView, processHandler, rMarkdownConsolePanel, title)
    contentDescriptor.setFocusComputable { consoleView.preferredFocusableComponent }
    contentDescriptor.isAutoFocusContent = true

    showConsole(DefaultRunExecutor.getRunExecutorInstance(), contentDescriptor)
  }

  fun runRender(commands: ArrayList<String>, path: String, onFinished: (() -> Unit)?) {
    knitOutputListener.onKnitrEnds = onFinished
    currentRenderFilePath = path
    val commandLine = GeneralCommandLine(commands)
    commandLine.setWorkDirectory(workingDir)
    processHandler = OSProcessHandler(commandLine)
    processHandler.addProcessListener(knitOutputListener)
    // I`m not sure is this usage of "runInEdt{}" correct and allowed
    runInEdt {
      if (!::consoleView.isInitialized || consoleView.editor == null) {
        consoleView = RMarkdownConsoleView(project)
        createContentDescriptor()

      }
      consoleView.attachToProcess(processHandler)
      consoleView.scrollToEnd()
      processHandler.startNotify()
    }
  }

  private fun showConsole(defaultExecutor: Executor, contentDescriptor: RunContentDescriptor) {
    ExecutionManager.getInstance(project).contentManager.showRunContent(defaultExecutor, contentDescriptor)
  }

  fun interruptRendering(){
    isInterrupted = true
    processHandler.process.destroy()
  }

  fun reportCandidateNotFound() {
    renderingErrorNotification("RMarkdown file was not found")
  }

  fun reportResultNotFound() {
    renderingErrorNotification("Result file was not found")
  }

  fun reportRenderingInterrupted() {
    renderingInfoNotification("Rendering was interrupted")
  }

  private fun renderingErrorNotification(errorMessage : String) {
    val notification = Notification("RMarkdownRenderError", "Rendering status",
                                    errorMessage, NotificationType.ERROR, null)
    notification.notify(project)
  }

  private fun renderingInfoNotification(infoMessage: String) {
    val notification = Notification("RMarkdownRenderInfo", "Rendering status",
                                    infoMessage, NotificationType.INFORMATION, null)
    notification.notify(project)
  }
}