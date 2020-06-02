/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.rinterop.RInterop
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

@Service
class RJobRunner(private val project: Project) {

  fun canRun(): Boolean =
    RInterpreterManager.getInstance(project).interpreterPath.isNotEmpty()

  @TestOnly
  internal fun run(task: RJobTask): ProcessHandler {
    check(canRun())
    val rConsoleManager = RConsoleManager.getInstance(project)
    val console = rConsoleManager.currentConsoleOrNull
    val rInterop = console?.rInterop
    val interpreterPath = RInterpreterManager.getInstance(project).interpreterPath
    val (scriptFile, exportRDataFile) = generateRunScript(task, rInterop)
    val commands = RInterpreterUtil.getRunHelperCommands(interpreterPath, scriptFile, emptyList())
    val commandLine = RInterpreterUtil.createCommandLine(interpreterPath, commands, task.workingDirectory)
    val osProcessHandler = OSProcessHandler(commandLine)
    if (exportRDataFile != null) {
      installProcessListener(osProcessHandler, exportRDataFile, console, task)
    }
    return osProcessHandler
  }

  private fun installProcessListener(osProcessHandler: OSProcessHandler,
                                     exportRDataFile: File,
                                     console: RConsoleView?,
                                     task: RJobTask) {
    val rInterop = console?.rInterop
    osProcessHandler.addProcessListener(object : ProcessAdapter() {
      override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
        if (exportRDataFile.length() > 0 && rInterop?.isAlive == true) {
          val variableName = if (task.exportGlobalEnv == ExportGlobalEnvPolicy.EXPORT_TO_VARIABLE)
            File(task.scriptPath).nameWithoutExtension + "_results"
          else ""
          rInterop.loadEnvironment(exportRDataFile.absolutePath, variableName).then {
            console.debuggerPanel?.onCommandExecuted()
          }
        }
      }
    })
  }

  private fun generateRunScript(task: RJobTask, rInterop: RInterop?): Pair<File, File?> {
    var importFile: String? = null
    var exportFile: File? = null

    if (task.importGlobalEnv) {
      if (rInterop?.isAlive == true) {
        importFile = FileUtil.createTempFile("import", ".RData", true).absolutePath
        rInterop.saveGlobalEnvironment(importFile).blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
      }
    }
    if (task.exportGlobalEnv != ExportGlobalEnvPolicy.DO_NO_EXPORT) {
      exportFile = FileUtil.createTempFile("export", ".RData", true)
    }
    var text = RPluginUtil.findFileInRHelpers("R/SourceWithProgress.template.R").readText()
    text = text.replace("<file-path>", task.scriptPath.toRString())
               .replace("<rdata-import>", importFile?.toRString() ?: "NULL")
               .replace("<rdata-export>", exportFile?.absolutePath?.toRString() ?: "NULL")

    val template = FileUtil.createTempFile("rjob-", ".R", true)
    template.writeText(text)
    return Pair(template, exportFile)
  }

  fun runRJob(task: RJobTask) {
    val processHandler = run(task)
    val consoleView = ConsoleViewImpl(project, true)
    consoleView.attachToProcess(processHandler)
    val myInputMessageFilterField = ConsoleViewImpl::class.memberProperties.first { it.name == "myInputMessageFilter" }
    val rJobProgressProvider = RJobProgressProvider()
    val rSourceProgressInputFilter = RSourceProgressInputFilter(rJobProgressProvider::onProgressAvailable)
    setFinalStatic(consoleView, myInputMessageFilterField.javaField!!, rSourceProgressInputFilter)
    val rJobDescriptor = RJobDescriptorImpl(project, task, rJobProgressProvider, processHandler, consoleView)
    processHandler.startNotify()
    invokeLater {
      RConsoleToolWindowFactory.getJobsPanel(project)?.addJobDescriptor(rJobDescriptor)
      RConsoleToolWindowFactory.focusOnJobs(project)
    }
  }

  companion object {
    fun getInstance(project: Project): RJobRunner = project.getService(RJobRunner::class.java)

    private fun setFinalStatic(o: Any, field: Field, newValue: Any) {
      field.setAccessible(true)
      val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
      modifiersField.setAccessible(true)
      modifiersField.setInt(field, field.getModifiers() and Modifier.FINAL.inv())
      field.set(o, newValue)
    }
  }

  private fun String.toRString(): String =
    "'${replace("""\""", """\\""").replace("""'""", """\'""")}'"
}