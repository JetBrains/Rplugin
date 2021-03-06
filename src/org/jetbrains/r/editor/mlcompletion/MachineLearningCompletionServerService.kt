package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import org.jetbrains.r.settings.MachineLearningCompletionSettingsChangeListener
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class MachineLearningCompletionServerService : Disposable {

  companion object {
    private val settings = MachineLearningCompletionSettings.getInstance()
    private val completionFilesService = MachineLearningCompletionModelFilesService.getInstance()
    private val LOG = Logger.getInstance(MachineLearningCompletionServerService::class.java)
    private const val RELAUNCH_TIMEOUT_MS = 30_000L

    fun getInstance() = service<MachineLearningCompletionServerService>()

    private fun Path.asLaunchCommand(): String =
      when {
        SystemInfo.isWindows -> toString()
        else -> parent.resolve("./$fileName").toString()
      }
  }

  private var localServer: Process? = null
  private var lastRelaunchInitializedTime: Long = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS)

  val serverAddress
    get() = "http://${settings.state.host}:${settings.state.port}"

  val requestTimeoutMs
    get() = settings.state.requestTimeoutMs

  init {
    MachineLearningCompletionSettingsChangeListener { beforeState, afterState ->
        if (beforeState == afterState) {
          return@MachineLearningCompletionSettingsChangeListener
        }
        if (beforeState.isEnabled && !afterState.isEnabled) {
          shutdownServer()
        }
        else {
          tryRelaunchServer(afterState.hostOrDefault(), afterState.port)
        }
    }.subscribe(this)

    if (settings.state.isEnabled) {
      launchServer(settings.state.hostOrDefault(), settings.state.port)
    }
  }

  @Synchronized
  fun tryRelaunchServer(host: String = settings.state.hostOrDefault(),
                        port: Int = settings.state.port) {
    if (System.currentTimeMillis() - lastRelaunchInitializedTime < RELAUNCH_TIMEOUT_MS) {
      return
    }
    lastRelaunchInitializedTime = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS)
    shutdownServer()
    launchServer(host, port)
  }

  fun shouldAttemptCompletion(): Boolean {
    return settings.state.isEnabled
  }

  private fun launchServer(host: String, port: Int): Boolean = completionFilesService.tryRunActionOnFiles { completionFiles ->
    completionFiles.localServerAppExecutableFile?.let { appFile ->
      try {
        FileUtil.setExecutable(File(appFile))
        val launchCommand = Paths.get(appFile).asLaunchCommand()
        val processBuilder = ProcessBuilder(launchCommand,
                                            "--config=${completionFiles.localServerConfigFile}",
                                            "--host=$host",
                                            "--port=$port")
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .directory(File(completionFiles.localServerDirectory!!))
        processBuilder.environment()
          .putAll(MachineLearningCompletionLocalServerVariables.SERVER_ENVIRONMENT)
        localServer = processBuilder.start()
      }
      catch (e: Exception) {
        LOG.warn("Exception has occurred in R ML Completion server thread", e)
      }
    }
  }

  fun shutdownServer(): Unit? = localServer?.run {
    descendants().forEach(ProcessHandle::destroy)
    destroy()
  }

  fun shutdownBlocking() {
    shutdownServer()
    localServer?.waitFor()
  }

  override fun dispose() {
    shutdownServer()
  }
}
