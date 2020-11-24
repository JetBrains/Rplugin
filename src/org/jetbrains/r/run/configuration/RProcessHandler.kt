package org.jetbrains.r.run.configuration

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.runHelperProcess
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class RProcessHandler(script: String,
                      scriptArgs: List<String>,
                      workingDirectory: String?,
                      environment: Map<String, String>? = null,
                      interpreterArgs: List<String>? = null,
                      val project: Project) : ProcessHandler() {

  @Volatile var myStartedToNotify: Boolean = false
  @Volatile var myExitCode: Int? = null
  val myProcessHandlerFuture = CompletableFuture<ProcessHandler>()
  val myQueue = ConcurrentLinkedQueue<Runnable>()

  init {
    val interpreterPromise = RInterpreterManager.getInterpreterAsync(project)
    interpreterPromise.then {
      val processHandler = it.runHelperProcess(script,
                                               scriptArgs = scriptArgs,
                                               workingDirectory = workingDirectory ?: it.basePath,
                                               environment = environment,
                                               interpreterArgs = interpreterArgs)
      synchronized(this) {
        myProcessHandlerFuture.complete(processHandler)

        while (!myQueue.isEmpty()) {
          myQueue.poll().run()
        }
      }
    }
  }

  @Synchronized
  fun forwardToOriginalProcessHandler(runnable: Runnable) {
    if (myProcessHandlerFuture.isDone) {
      runnable.run()
    } else {
      myQueue.add(runnable)
    }
  }

  override fun destroyProcess() {
    forwardToOriginalProcessHandler { myProcessHandlerFuture.getNow(null)?.destroyProcess() }
  }

  override fun detachProcess() {
    forwardToOriginalProcessHandler { myProcessHandlerFuture.getNow(null)?.detachProcess() }
  }

  override fun startNotify() {
    myStartedToNotify = true
    forwardToOriginalProcessHandler {
      myProcessHandlerFuture.getNow(null)?.startNotify()
    }
  }

  override fun waitFor(): Boolean {
    try {
      while (!myProcessHandlerFuture.isDone) {
        myProcessHandlerFuture.get(100, TimeUnit.MILLISECONDS)
        ProgressManager.checkCanceled()
      }
    }
    catch (e: ProcessCanceledException) {
      return false
    }

    return myProcessHandlerFuture.getNow(null)?.waitFor() ?: false
  }

  override fun waitFor(timeoutInMilliseconds: Long): Boolean {
    val startTime = System.nanoTime()
    try {
      while (myProcessHandlerFuture.isDone) {
        myProcessHandlerFuture.get(timeoutInMilliseconds, TimeUnit.MILLISECONDS)
        ProgressManager.checkCanceled()
      }
    }
    catch (e: ProcessCanceledException) {
      return false
    }
    val passed = (System.nanoTime() - startTime) / 1_000_000
    val restToWait = timeoutInMilliseconds - passed
    if (restToWait <= 0) {
      return false
    }
    return myProcessHandlerFuture.getNow(null)?.waitFor(restToWait) ?: false
  }

  override fun isProcessTerminated(): Boolean {
    return myProcessHandlerFuture.getNow(null)?.isProcessTerminated ?: false
  }

  override fun isProcessTerminating(): Boolean {
    return myProcessHandlerFuture.getNow(null)?.isProcessTerminating ?: false
  }

  override fun getExitCode(): Int? {
    return myProcessHandlerFuture.getNow(null)?.exitCode ?: myExitCode
  }

  override fun addProcessListener(listener: ProcessListener) {
    forwardToOriginalProcessHandler {
      myProcessHandlerFuture.getNow(null)?.addProcessListener(listener)
    }
  }

  override fun removeProcessListener(listener: ProcessListener) {
    forwardToOriginalProcessHandler {
      myProcessHandlerFuture.getNow(null)?.removeProcessListener(listener)
    }
  }

  override fun notifyTextAvailable(text: String, outputType: Key<*>) {
    forwardToOriginalProcessHandler {
      myProcessHandlerFuture.getNow(null)?.notifyTextAvailable(text, outputType)
    }
  }

  override fun isStartNotified(): Boolean {
    return  myStartedToNotify
  }

  override fun destroyProcessImpl() {
  }

  override fun detachProcessImpl() {
  }

  override fun detachIsDefault(): Boolean {
    return myProcessHandlerFuture.getNow(null)?.detachIsDefault() ?: false
  }

  override fun getProcessInput(): OutputStream? {
    return myProcessHandlerFuture.getNow(null)?.processInput
  }
}