package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import org.jetbrains.r.settings.MachineLearningCompletionSettingsChangeListener
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

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
  private var lastRelaunchInitializedTime: Long = System.currentTimeMillis()

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
    }.subscribeWithDisposable(this)

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
    lastRelaunchInitializedTime = System.currentTimeMillis()
    shutdownServer()
    launchServer(host, port)
  }

  fun shouldAttemptCompletion(): Boolean {
    return settings.state.isEnabled
  }

  private fun launchServer(host: String, port: Int) = completionFilesService.tryRunActionOnFiles { completionFiles ->
    completionFiles.localServerAppExecutableFile?.let { appFile ->
      try {
        setExecutablePermission(appFile)
        val launchCommand = Paths.get(appFile).asLaunchCommand()
        val processBuilder = ProcessBuilder(launchCommand,
                                            "--config=${completionFiles.localServerConfigFile}",
                                            "--host=$host",
                                            "--port=$port")
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .directory(File(completionFiles.localServerModelDirectory!!))
        processBuilder.environment()
          .putAll(MachineLearningCompletionLocalServerVariables.SERVER_ENVIRONMENT)
        localServer = processBuilder.start()
      }
      catch (e: Exception) {
        LOG.warn("Exception has occurred in R ML Completion server thread", e)
      }
    }
  }

  private fun setExecutablePermission(file: String) {
    if (SystemInfo.isUnix) {
      try {
        val process = ProcessBuilder("chmod", "+x", file).start()
        process.waitFor()
      } catch (e: Exception) {
        LOG.warn("Exception has occurred when trying to set executable permission for R ML Completion server app", e)
      }
    }
  }

  fun shutdownServer() {
    localServer?.destroy()
  }

  fun shutdownBlocking() {
    shutdownServer()
    localServer?.waitFor()
  }

  override fun dispose() {
    shutdownServer()
  }
}
