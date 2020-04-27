/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.*
import com.intellij.execution.runners.ConsoleTitleGen
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.rendering.settings.RMarkdownSettings
import java.awt.BorderLayout
import java.io.File
import java.io.IOException
import javax.swing.JPanel

class RMarkdownRenderingConsoleRunner(private val project : Project,
                                      private val consoleTitle: String = "RMarkdown Console") {
  @Volatile
  private var isInterrupted = false
  @Volatile
  private var currentProcessHandler: OSProcessHandler? = null
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

  private fun createConsoleView(processHandler: OSProcessHandler): ConsoleViewImpl {
    val consoleView = ConsoleViewImpl(project, false)
    currentConsoleView = consoleView
    val rMarkdownConsolePanel = JPanel(BorderLayout())
    rMarkdownConsolePanel.add(consoleView.component, BorderLayout.CENTER)
    val title = ConsoleTitleGen(project, consoleTitle, false).makeTitle()
    val contentDescriptor = RunContentDescriptor(consoleView, processHandler, rMarkdownConsolePanel, title)
    contentDescriptor.setFocusComputable { consoleView.preferredFocusableComponent }
    contentDescriptor.isAutoFocusContent = true
    val contentManager = RunContentManager.getInstance(project)
    val executor = DefaultRunExecutor.getRunExecutorInstance()
    contentManager.allDescriptors.filter { it.displayName == title }.forEach { contentManager.removeRunContent(executor, it) }
    contentManager.showRunContent(executor, contentDescriptor)
    return consoleView
  }

  private fun runRender(commands: ArrayList<String>, renderDirectory: String, path: String, promise: AsyncPromise<Unit>,
                        resultTmpFile: File, isShiny: Boolean) {
    val commandLine = GeneralCommandLine(commands)
    commandLine.setWorkDirectory(renderDirectory)
    val processHandler = OSProcessHandler(commandLine)
    currentProcessHandler = processHandler
    val knitListener = makeKnitListener(path, promise, resultTmpFile, isShiny)
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

  private fun doRender(project: Project, rMarkdownFile: VirtualFile, promise: AsyncPromise<Unit>, isShiny: Boolean) {
    val interpreter = RInterpreterManager.getInterpreter(project) ?: return
    val pathToRscript = getRScriptPath(interpreter.interpreterPath)
    val scriptPath = StringUtil.escapeBackSlashes(R_MARKDOWN_HELPER.absolutePath)
    val filePath = StringUtil.escapeBackSlashes(rMarkdownFile.path)
    val knitRootDirectory = StringUtil.escapeBackSlashes(
      RMarkdownSettings.getInstance(project).state.getKnitRootDirectory(rMarkdownFile.path))
    val libraryPath = StringUtil.escapeBackSlashes(getPandocLibraryPath())
    val resultTmpFile = FileUtil.createTempFile("rmd-output-path", ".txt", true)
    val script = arrayListOf<String>(
      pathToRscript, scriptPath, libraryPath, filePath, knitRootDirectory, resultTmpFile.absolutePath)
    runRender(script, knitRootDirectory, rMarkdownFile.path, promise, resultTmpFile, isShiny)
  }

  private fun getRScriptPath(interpreterPath: String): String {
    return if (SystemInfo.isWindows) {
      val withoutExe = interpreterPath.substring(0, interpreterPath.length - 4)
      withoutExe + "script.exe"
    } else {
      interpreterPath + "script"
    }
  }

  private fun getPandocLibraryPath(): String {
    return RPluginUtil.findFileInRHelpers("pandoc").toString().also {
      File(it).mkdir()
    }
  }

  private fun renderingErrorNotification() {
    val notification = Notification("RMarkdownRenderError", "Rendering status",
                                    "Error occurred during rendering", NotificationType.ERROR, null)
    notification.notify(project)
  }

  private fun makeKnitListener(renderFilePath: String, promise: AsyncPromise<Unit>, resultTmpFile: File, isShiny: Boolean): ProcessListener {
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
                val outputPath = resultTmpFile.readText().trim()
                RMarkdownSettings.getInstance(project).state.setProfileLastOutput(renderFilePath, outputPath)
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
        resultTmpFile.delete()
      }
    }
  }

  companion object {
    private val R_MARKDOWN_HELPER = RPluginUtil.findFileInRHelpers("R/render_markdown.R")
  }
}