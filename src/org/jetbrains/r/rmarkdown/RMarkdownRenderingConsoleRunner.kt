/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ScriptRunnerUtil
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.runHelperProcess
import org.jetbrains.r.interpreter.uploadFileToHost
import org.jetbrains.r.rendering.settings.RMarkdownSettings
import org.jetbrains.r.util.RPathUtil
import java.awt.BorderLayout
import java.io.IOException
import javax.swing.JPanel

class RMarkdownRenderingConsoleRunnerException(message: String) : RuntimeException(message)

class RMarkdownRenderingConsoleRunner(private val project : Project,
                                      private val consoleTitle: String = RBundle.message("rmarkdown.rendering.console.title")) {
  @Volatile
  private var isInterrupted = false
  @Volatile
  private var currentProcessHandler: ProcessHandler? = null
  @Volatile
  private var currentConsoleView: ConsoleViewImpl? = null

  fun interruptRendering() {
    isInterrupted = true
    currentConsoleView?.print("\nInterrupted\n", ConsoleViewContentType.ERROR_OUTPUT)
    currentProcessHandler?.let { ScriptRunnerUtil.terminateProcessHandler(it, 2000, null) }
  }

  @Throws(RMarkdownRenderingConsoleRunnerException::class, IOException::class)
  suspend fun render(project: Project, file: VirtualFile, isShiny: Boolean = false) {
    RMarkdownUtil.checkOrInstallPackages(project, RBundle.message("rmarkdown.processor.notification.utility.name"))

    if (!isInterrupted) {
      doRender(project, file, isShiny)
    }
    else {
      throw RMarkdownRenderingConsoleRunnerException("Rendering was interrupted")
    }
  }

  private fun createConsoleView(processHandler: ProcessHandler): ConsoleViewImpl {
    val consoleView = ConsoleViewImpl(project, false)
    currentConsoleView = consoleView
    val rMarkdownConsolePanel = JPanel(BorderLayout())
    rMarkdownConsolePanel.add(consoleView.component, BorderLayout.CENTER)
    val title = consoleTitle
    val contentDescriptor = RunContentDescriptor(consoleView, processHandler, rMarkdownConsolePanel, title)
    contentDescriptor.setFocusComputable { consoleView.preferredFocusableComponent }
    contentDescriptor.isAutoFocusContent = true
    val contentManager = RunContentManager.getInstance(project)
    val executor = DefaultRunExecutor.getRunExecutorInstance()
    contentManager.allDescriptors.filter { it.displayName == title }.forEach { contentManager.removeRunContent(executor, it) }
    contentManager.showRunContent(executor, contentDescriptor)
    return consoleView
  }

  private suspend fun doRender(project: Project, rMarkdownFile: VirtualFile, isShiny: Boolean) {
    val interpreter = RInterpreterManager.getInstance(project).awaitInterpreter().getOrThrow()
    interpreter.prepareForExecution()

    val rmdFileOnHost = interpreter.uploadFileToHostIfNeeded(rMarkdownFile, preserveName = true)
    val knitRootDirectory = PathUtil.getParentPath(rmdFileOnHost)
    val outputDirectory = RMarkdownSettings.getInstance(project).state.getOutputDirectory(rMarkdownFile)
                          ?: rMarkdownFile.parent
    val outputDirOnHost = interpreter.getFilePathAtHost(outputDirectory) ?: knitRootDirectory
    val libraryPath = RPathUtil.join(interpreter.getHelpersRootOnHost(), "pandoc")
    val resultTmpFile = interpreter.createTempFileOnHost("rmd-output-path.txt")

    val args = arrayListOf(libraryPath, rmdFileOnHost, knitRootDirectory, outputDirOnHost, resultTmpFile)
    val processHandler = interpreter.runHelperProcess(interpreter.uploadFileToHost(R_MARKDOWN_HELPER), args,
                                                      knitRootDirectory)
    currentProcessHandler = processHandler

    val (knitListener, onProcessTerminated) = makeKnitListener(interpreter, rMarkdownFile, resultTmpFile, outputDirectory, isShiny)
    processHandler.addProcessListener(knitListener)

    withContext(Dispatchers.EDT) {
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        val consoleView = createConsoleView(processHandler)
        consoleView.attachToProcess(processHandler)
        consoleView.scrollToEnd()
      }
      processHandler.startNotify()
    }

    return onProcessTerminated.await()
  }

  private fun renderingErrorNotification() {
    Notification("RMarkdownRenderError", RBundle.message("notification.title.rendering.status"), RBundle.message("notification.content.error.occurred.during.rendering"), NotificationType.ERROR)
      .notify(project)
  }

  private fun makeKnitListener(interpreter: RInterpreter, file: VirtualFile, resultTmpFileOnHost: String,
                               outputDir: VirtualFile, isShiny: Boolean): Pair<ProcessListener, CompletableDeferred<Unit>> {
    val onProcessTerminated = CompletableDeferred<Unit>()

    val processListener =  object : ProcessListener {
      override fun processTerminated(event: ProcessEvent) {
        val exitCode = event.exitCode
        if (isInterrupted || currentConsoleView?.let { Disposer.isDisposed(it) } == true) {
          isInterrupted = false
          onProcessTerminated.completeExceptionally(RMarkdownRenderingConsoleRunnerException("Rendering was interrupted"))
        } else {
          if (exitCode == 0) {
            try {
              if (!isShiny) {
                val outputPathOnHost =
                  interpreter.findFileByPathAtHost(resultTmpFileOnHost)?.contentsToByteArray()?.toString(Charsets.UTF_8).orEmpty()
                RMarkdownSettings.getInstance(project).state.setProfileLastOutput(file, outputPathOnHost)
                outputDir.refresh(true, false)
              }
              onProcessTerminated.complete(Unit)
            } catch (e: IOException) {
              onProcessTerminated.completeExceptionally(e)
            }
          } else {
            renderingErrorNotification()
              // todo make exception for runner
            onProcessTerminated.completeExceptionally(RMarkdownRenderingConsoleRunnerException("Rendering has non-zero exit code"))
          }
        }
      }
    }

    return processListener to onProcessTerminated
  }

  companion object {
    private val R_MARKDOWN_HELPER = RPluginUtil.findFileInRHelpers("R/render_markdown.R")
  }
}
