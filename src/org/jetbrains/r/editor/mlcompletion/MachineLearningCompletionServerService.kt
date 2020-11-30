package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.isLocalHost
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import org.jetbrains.r.settings.MachineLearningCompletionSettingsChangeListener
import org.jetbrains.r.settings.R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC
import java.io.File
import java.nio.file.Paths

class MachineLearningCompletionServerService: Disposable {

  companion object {
    private val settings = MachineLearningCompletionSettings.getInstance()
    private val LOG = Logger.getInstance(MachineLearningCompletionServerService::class.java)
    private const val RELAUNCH_TIMEOUT_MS = 30_000L
    private val LOCAL_SERVER_DIRECTORY = resolveWithNullable(RPluginUtil.helperPathOrNull, "python_server")
    private val LAUNCH_SERVER_COMMAND = resolveWithNullable(LOCAL_SERVER_DIRECTORY, "dist", "./run_demo")
    private val LOCAL_SERVER_CONFIG_PATH = resolveWithNullable(LOCAL_SERVER_DIRECTORY, "config.yml")
    fun getInstance() = service<MachineLearningCompletionServerService>()

    private fun resolveWithNullable(first: String?, vararg more: String): String? =
      first?.let {
        return Paths.get(first, *more).toString()
      }
  }

  private var localServer: Process? = null
  private var lastRelaunchInitializedTime: Long = System.currentTimeMillis()

  val serverAddress
  get() = "http://${settings.state.host}:${settings.state.port}"

  init {
    val settingsListener = object : MachineLearningCompletionSettingsChangeListener {
      override fun settingsChanged(beforeState: MachineLearningCompletionSettings.State,
                                   afterState: MachineLearningCompletionSettings.State) {
        if (beforeState == afterState) {
          return
        }
        if (beforeState.isEnabled && !afterState.isEnabled) {
          shutdownServer()
        } else {
          tryRelaunchServer(afterState.hostOrDefault(), afterState.port)
        }
      }
    }

    ApplicationManager.getApplication().messageBus.connect(this).apply {
      subscribe(R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC, settingsListener)
    }

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

  private fun launchServer(host: String, port: Int) {
    if (!isLocalHost(host)
        || LOCAL_SERVER_DIRECTORY == null
        || LAUNCH_SERVER_COMMAND == null
        || LOCAL_SERVER_CONFIG_PATH == null) {
      return
    }
    try {
      val processBuilder = ProcessBuilder(LAUNCH_SERVER_COMMAND,
                                   "--config=$LOCAL_SERVER_CONFIG_PATH",
                                   "--host=$host",
                                   "--port=$port")
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .directory(File(LOCAL_SERVER_DIRECTORY))
      processBuilder.environment()
        .putAll(MachineLearningCompletionLocalServerVariables.SERVER_ENVIRONMENT)
      localServer = processBuilder.start()
    } catch (e: Exception) {
      LOG.warn("Exception has occurred in R ML Completion server thread", e)
    }
  }

  private fun shutdownServer() {
    localServer?.destroy()
  }

  override fun dispose() {
    shutdownServer()
  }
}
