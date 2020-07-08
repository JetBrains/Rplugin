/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.*
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.*
import org.jetbrains.r.rendering.settings.RMarkdownSettings
import org.jetbrains.r.util.RPathUtil
import java.awt.BorderLayout
import java.io.IOException
import javax.swing.JPanel

class RMarkdownRenderingConsoleRunner(private val project : Project,
                                      private val consoleTitle: String = "RMarkdown Console") {
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

  fun render(project: Project, file: VirtualFile, isShiny: Boolean = false): Promise<Unit> {
    return AsyncPromise<Unit>().also { promise ->
      RMarkdownUtil.checkOrInstallPackages(project, RBundle.message("rmarkdown.processor.notification.utility.name"))
        .onSuccess {
          if (!isInterrupted) {
            doRender(project, file, promise, isShiny)
          } else {
            promise.setError("Rendering was interrupted")
          }
        }
        .onError {
          promise.setError("Unable to install dependencies for rendering")
        }
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

  private fun doRender(project: Project, rMarkdownFile: VirtualFile, promise: AsyncPromise<Unit>, isShiny: Boolean) {
    RInterpreterManager.getInterpreterAsync(project).onSuccess { interpreter ->
      runAsync {
        val rmdFileOnHost = interpreter.uploadFileToHostIfNeeded(rMarkdownFile, preserveName = true)
        val outputDirectory = RMarkdownSettings.getInstance(project).state.getKnitRootDirectory(rMarkdownFile) ?:
                              rMarkdownFile.parent
        val knitRootDirectory = interpreter.getFilePathAtHost(outputDirectory) ?:
                                interpreter.createTempDirOnHost("knot-root")
        val libraryPath = RPathUtil.join(interpreter.getHelpersRootOnHost(), "pandoc")
        val resultTmpFile = interpreter.createTempFileOnHost("rmd-output-path.txt")

        val args = arrayListOf(libraryPath, rmdFileOnHost, knitRootDirectory, resultTmpFile)
        val processHandler = interpreter.runHelperProcess(interpreter.uploadFileToHost(R_MARKDOWN_HELPER), args,
                                                          knitRootDirectory)
        currentProcessHandler = processHandler
        val knitListener = makeKnitListener(interpreter, rMarkdownFile, promise, resultTmpFile, outputDirectory, isShiny)
        processHandler.addProcessListener(knitListener)
        runInEdt {
          if (!ApplicationManager.getApplication().isUnitTestMode) {
            val consoleView = createConsoleView(processHandler)
            consoleView.attachToProcess(processHandler)
            consoleView.scrollToEnd()
          }
          processHandler.startNotify()
        }
      }
    }.onError {
      promise.setError(it)
    }
  }

  private fun renderingErrorNotification() {
    val notification = Notification("RMarkdownRenderError", "Rendering status",
                                    "Error occurred during rendering", NotificationType.ERROR, null)
    notification.notify(project)
  }

  private fun makeKnitListener(interpreter: RInterpreter, file: VirtualFile, promise: AsyncPromise<Unit>, resultTmpFileOnHost: String,
                               outputDir: VirtualFile, isShiny: Boolean): ProcessListener {
    return object : ProcessAdapter() {
      override fun processTerminated(event: ProcessEvent) {
        val exitCode = event.exitCode
        if (isInterrupted || currentConsoleView?.let { Disposer.isDisposing(it) || Disposer.isDisposed(it) } == true) {
          isInterrupted = false
          promise.setError("Rendering was interrupted")
        } else {
          if (exitCode == 0) {
            try {
              if (!isShiny) {
                val outputPathOnHost =
                  interpreter.findFileByPathAtHost(resultTmpFileOnHost)?.contentsToByteArray()?.toString(Charsets.UTF_8).orEmpty()
                val outputPath = if (interpreter.isLocal()) {
                  outputPathOnHost
                } else {
                  val dir = if (outputDir.isInLocalFileSystem) {
                    outputDir.path
                  } else {
                    FileUtilRt.createTempDirectory("rmd-output", null, true).path
                  }
                  RPathUtil.join(dir, PathUtil.getFileName(outputPathOnHost)).also {
                    interpreter.downloadFileFromHost(outputPathOnHost, it)
                  }
                }
                RMarkdownSettings.getInstance(project).state.setProfileLastOutput(file, outputPath)
              }
              promise.setResult(Unit)
            } catch (e: IOException) {
              promise.setError(e)
            }
          } else {
            renderingErrorNotification()
            promise.setError("Rendering has non-zero exit code")
          }
        }
      }
    }
  }

  companion object {
    private val R_MARKDOWN_HELPER = RPluginUtil.findFileInRHelpers("R/render_markdown.R")
  }
}