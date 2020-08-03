/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.runHelperProcess
import org.jetbrains.r.rinterop.RInterop
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

@Service
class RJobRunner(private val project: Project) {

  fun canRun(): Boolean = RInterpreterManager.getInstance(project).hasInterpreter()

  @TestOnly
  internal fun run(task: RJobTask): Promise<ProcessHandler> {
    check(canRun())
    invokeAndWaitIfNeeded { FileDocumentManager.getInstance().saveAllDocuments() }
    val rConsoleManager = RConsoleManager.getInstance(project)
    val console = rConsoleManager.currentConsoleOrNull
    val rInterop = console?.rInterop
    return RInterpreterManager.getInterpreterAsync(project).thenAsync { interpreter ->
      interpreter.prepareForExecution().then {
        val (scriptFile, exportRDataFile) = generateRunScript(interpreter, task, rInterop)
        val processHandler: ProcessHandler = interpreter.runHelperProcess(scriptFile, emptyList(), task.workingDirectory)
        if (exportRDataFile != null) {
          installProcessListener(processHandler, exportRDataFile, console, task)
        }
        processHandler
      }
    }
  }

  private fun installProcessListener(processHandler: ProcessHandler,
                                     exportRDataFile: String,
                                     console: RConsoleView?,
                                     task: RJobTask) {
    val rInterop = console?.rInterop
    processHandler.addProcessListener(object : ProcessAdapter() {
      override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
        if (rInterop?.isAlive == true) {
          val variableName = if (task.exportGlobalEnv == ExportGlobalEnvPolicy.EXPORT_TO_VARIABLE)
            task.script.nameWithoutExtension + "_results"
          else ""
          rInterop.loadEnvironment(exportRDataFile, variableName).then {
            console.debuggerPanel?.onCommandExecuted()
          }
        }
      }
    })
  }

  private fun generateRunScript(interpreter: RInterpreter, task: RJobTask, rInterop: RInterop?): Pair<String, String?> {
    var importFile: String? = null
    var exportFile: String? = null

    if (task.importGlobalEnv) {
      if (rInterop?.isAlive == true) {
        importFile = interpreter.createTempFileOnHost("import.RData")
        rInterop.saveGlobalEnvironment(importFile).blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
      }
    }
    if (task.exportGlobalEnv != ExportGlobalEnvPolicy.DO_NO_EXPORT) {
      exportFile = interpreter.createTempFileOnHost("export.RData")
    }
    var text = RPluginUtil.findFileInRHelpers("R/SourceWithProgress.template.R").readText()
    text = text.replace("<file-path>", interpreter.uploadFileToHostIfNeeded(task.script).toRString())
               .replace("<rdata-import>", importFile?.toRString() ?: "NULL")
               .replace("<rdata-export>", exportFile?.toRString() ?: "NULL")

    return Pair(interpreter.createTempFileOnHost("rjob.R", text.toByteArray()), exportFile)
  }

  fun runRJob(task: RJobTask): Promise<RJobDescriptor> {
    val promise = AsyncPromise<RJobDescriptor>()
    run(task).then { processHandler ->
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
      promise.setResult(rJobDescriptor)
    }
    return promise
  }

  companion object {
    fun getInstance(project: Project): RJobRunner = project.getService(RJobRunner::class.java)

    private fun setFinalStatic(o: Any, field: Field, newValue: Any) {
      field.isAccessible = true
      val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
      modifiersField.isAccessible = true
      modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
      field.set(o, newValue)
    }
  }

  private fun String.toRString(): String =
    "'${replace("""\""", """\\""").replace("""'""", """\'""")}'"
}