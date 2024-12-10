/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.util.EventDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.asPromise
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.runHelperProcess
import org.jetbrains.r.rinterop.RInterop
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

@Service(Service.Level.PROJECT)
class RJobRunner(
  private val project: Project,
  val coroutineScope: CoroutineScope,
) {
  internal val eventDispatcher = EventDispatcher.create(Listener::class.java)

  fun canRun(): Boolean = RInterpreterManager.getInstance(project).hasInterpreter()

  @TestOnly
  internal fun run(task: RJobTask, exportEnvName: String? = null): Promise<ProcessHandler> {
    return coroutineScope.async {
      suspendableRun(task, exportEnvName)
    }.asCompletableFuture().asPromise()
  }

  private suspend fun suspendableRun(task: RJobTask, exportEnvName: String? = null): ProcessHandler {
    check(canRun())

    writeAction {
      FileDocumentManager.getInstance().saveAllDocuments()
    }

    val rConsoleManager = RConsoleManager.getInstance(project)
    val console = rConsoleManager.currentConsoleOrNull
    val rInterop = console?.rInterop

    val interpreter = RInterpreterManager.getInstance(project).awaitInterpreter().getOrThrow()
    interpreter.prepareForExecution()

    val (scriptFile, exportRDataFile) = generateRunScript(interpreter, task, rInterop)
    val processHandler: ProcessHandler = interpreter.runHelperProcess(scriptFile, emptyList(), task.workingDirectory)
    if (exportRDataFile != null) {
      installProcessListener(processHandler, exportRDataFile, console, task, exportEnvName)
    }
    return processHandler
  }

  private fun installProcessListener(processHandler: ProcessHandler,
                                     exportRDataFile: String,
                                     console: RConsoleView?,
                                     task: RJobTask,
                                     exportEnvName: String? = null) {
    console?.rInterop?.let { rInterop ->
      processHandler.addProcessListener(
        object : ProcessAdapter() {
          override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
            if (rInterop.isAlive) {
              val variableName = if (task.exportGlobalEnv == ExportGlobalEnvPolicy.EXPORT_TO_VARIABLE)
                exportEnvName ?: (task.script.nameWithoutExtension + "_results")
              else ""
              rInterop.loadEnvironment(exportRDataFile, variableName).then {
                console.debuggerPanel?.onCommandExecuted()
              }
            }
          }
        },
        rInterop
      )
    }
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

  suspend fun suspendableRunRJob(task: RJobTask, exportEnvName: String? = null, name: String? = null): RJobDescriptor {
    return withContext(Dispatchers.Default) {
      val processHandler = suspendableRun(task, exportEnvName)
      val consoleView = ConsoleViewImpl(project, true)
      consoleView.attachToProcess(processHandler)
      val myInputMessageFilterField = ConsoleViewImpl::class.memberProperties.first { it.name == "myInputMessageFilter" }
      val rJobProgressProvider = RJobProgressProvider()
      val rSourceProgressInputFilter = RSourceProgressInputFilter(rJobProgressProvider::onProgressAvailable)
      setFinalStatic(consoleView, myInputMessageFilterField.javaField!!, rSourceProgressInputFilter)
      val rJobDescriptor = RJobDescriptorImpl(project, task, rJobProgressProvider, processHandler, consoleView, name)
      eventDispatcher.multicaster.onJobDescriptionCreated(rJobDescriptor)
      processHandler.startNotify()

      coroutineScope.launch(Dispatchers.EDT) {
        RJobsToolWindowFactory.getJobsPanel(project)?.addJobDescriptor(rJobDescriptor)
        RJobsToolWindowFactory.focusOnJobs(project)
      }

      return@withContext rJobDescriptor
    }
  }

  fun runRJob(task: RJobTask, exportEnvName: String? = null, name: String? = null): Promise<RJobDescriptor> {
    return coroutineScope.async(ModalityState.defaultModalityState().asContextElement()) {
      suspendableRunRJob(task, exportEnvName, name)
    }.asCompletableFuture().asPromise()
  }

  companion object {
    fun getInstance(project: Project): RJobRunner = project.service()

    private fun setFinalStatic(o: Any, field: Field, newValue: Any) {
      field.isAccessible = true
      val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
      modifiersField.isAccessible = true
      modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
      field.set(o, newValue)
    }
  }

  internal interface Listener : EventListener {
    fun onJobDescriptionCreated(rJobDescriptor: RJobDescriptor)
  }

  private fun String.toRString(): String =
    "'${replace("""\""", """\\""").replace("""'""", """\'""")}'"
}