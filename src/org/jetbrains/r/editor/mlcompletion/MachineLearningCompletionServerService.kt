package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.util.io.isLocalHost
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import org.jetbrains.r.settings.MachineLearningCompletionSettingsChangeListener
import org.jetbrains.r.settings.R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC
import java.nio.file.Paths

class MachineLearningCompletionServerService: Disposable {

  companion object {
    private val settings = MachineLearningCompletionSettings.getInstance()
    private const val RELAUNCH_TIMEOUT_MS = 5_000L
    private const val LAUNCH_SERVER_COMMAND = "python3"
    private val LOCAL_SERVER_MAIN_FILE_PATH = Paths.get(
      System.getProperty("user.dir"),
      "../rplugin/src/org/jetbrains/r/editor/mlcompletion/python_server"
    ).toString()

    fun getInstance() = service<MachineLearningCompletionServerService>()
  }

  private var localServer: Process? = null
  private var lastRelaunchInitializedTime: Long = System.currentTimeMillis();

  val serverAddress
  get() = "http://${settings.state.host}:${settings.state.port}"

  private val settingsListener = object : MachineLearningCompletionSettingsChangeListener {
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

  init {
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
    if (!isLocalHost(host)) {
      return
    }
    localServer =
      ProcessBuilder(LAUNCH_SERVER_COMMAND, LOCAL_SERVER_MAIN_FILE_PATH, host, port.toString())
        .start()
  }

  private fun shutdownServer() {
    localServer?.destroy()
  }

  override fun dispose() {
    shutdownServer()
  }
}
