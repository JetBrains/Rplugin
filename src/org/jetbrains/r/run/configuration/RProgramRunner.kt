package org.jetbrains.r.run.configuration

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise

class RProgramRunner: AsyncProgramRunner<RunnerSettings>() {
  override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
    FileDocumentManager.getInstance().saveAllDocuments()
    val result = AsyncPromise<RunContentDescriptor?>()
    AppExecutorUtil.getAppExecutorService().execute {
      val commandLineState = state as RCommandLineRunningState
      val executionResult = commandLineState.execute(environment.executor, this)

      val builder = RunContentBuilder(executionResult, environment)
      ApplicationManager.getApplication().invokeLater {
        result.setResult(builder.showRunContent(environment.contentToReuse))
      }
    }
    return result
  }

  override fun getRunnerId(): String = "r.program.runner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return DefaultRunExecutor.EXECUTOR_ID == executorId && profile is RRunConfiguration
  }
}