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
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RPluginUtil
import com.intellij.r.psi.interpreter.RInterpreter
import com.intellij.r.psi.interpreter.RInterpreterManager
import com.intellij.r.psi.interpreter.runHelperProcess
import com.intellij.r.psi.interpreter.uploadFileToHost
import com.intellij.r.psi.util.RPathUtil
import com.intellij.util.PathUtil
import kotlinx.coroutines.*
import org.jetbrains.r.rendering.settings.RMarkdownSettings
import java.awt.BorderLayout
import java.io.IOException
import javax.swing.JPanel
import kotlin.coroutines.resumeWithException

class RMarkdownRenderingConsoleRunnerException(message: String) : RuntimeException(message)

class RMarkdownRenderingConsoleRunner(
  private val project: Project,
  private val consoleTitle: String = RBundle.message("rmarkdown.rendering.console.title"),
) {
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

    coroutineScope {
      suspendCancellableCoroutine { cancellableContinuation ->
        val knitListener = makeKnitListener(interpreter, rMarkdownFile, resultTmpFile, outputDirectory, isShiny, cancellableContinuation)
        processHandler.addProcessListener(knitListener)

        launch(Dispatchers.EDT) {
          if (!ApplicationManager.getApplication().isUnitTestMode) {
            val consoleView = createConsoleView(processHandler)
            consoleView.attachToProcess(processHandler)
            consoleView.scrollToEnd()
          }
          processHandler.startNotify()
        }
      }
    }
  }

  private fun renderingErrorNotification() {
    Notification("RMarkdownRenderError", RBundle.message("notification.title.rendering.status"), RBundle.message("notification.content.error.occurred.during.rendering"), NotificationType.ERROR)
      .notify(project)
  }

  private fun makeKnitListener(
    interpreter: RInterpreter,
    file: VirtualFile,
    resultTmpFileOnHost: String,
    outputDir: VirtualFile,
    isShiny: Boolean,
    cancellableContinuation: CancellableContinuation<Unit>,
  ): ProcessListener =
    object : ProcessListener {
      override fun processTerminated(event: ProcessEvent) {
        val exitCode = event.exitCode
        if (isInterrupted || currentConsoleView?.let { Disposer.isDisposed(it) } == true) {
          isInterrupted = false
          cancellableContinuation.resumeWithException(RMarkdownRenderingConsoleRunnerException("Rendering was interrupted"))
        }
        else {
          if (exitCode == 0) {
            try {
              if (!isShiny) {
                val outputPathOnHost =
                  interpreter.findFileByPathAtHost(resultTmpFileOnHost)?.contentsToByteArray()?.toString(Charsets.UTF_8).orEmpty()
                RMarkdownSettings.getInstance(project).state.setProfileLastOutput(file, outputPathOnHost)
                outputDir.refresh(true, false)
              }
              cancellableContinuation.resume(Unit) { cause, _, _ -> Unit }
            }
            catch (e: IOException) {
              cancellableContinuation.resumeWithException(e)
            }
          }
          else {
            renderingErrorNotification()
            cancellableContinuation.resumeWithException(RMarkdownRenderingConsoleRunnerException("Rendering has non-zero exit code"))
          }
        }
      }
    }

  companion object {
    private val R_MARKDOWN_HELPER = RPluginUtil.findFileInRHelpers("R/render_markdown.R")
  }
}
