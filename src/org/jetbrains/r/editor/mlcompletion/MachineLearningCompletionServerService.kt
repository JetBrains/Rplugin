package org.jetbrains.r.editor.mlcompletion

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.util.io.isLocalHost
import org.jetbrains.r.settings.MachineLearningCompletionSettings
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

  private var host = settings.state.host
  private var port = settings.state.port
  private var localServer: Process? = null
  private var lastRelaunchInitializedTime: Long = System.currentTimeMillis();

  init {
    launchServer()
  }

  val serverAddress: String
  get() {
    if (settingsChanged()) {
      tryRelaunchServer()
    }
    return "http://${host}:${port}"
  }

  @Synchronized
  fun tryRelaunchServer() {
    if (System.currentTimeMillis() - lastRelaunchInitializedTime < RELAUNCH_TIMEOUT_MS) {
      return
    }
    lastRelaunchInitializedTime = System.currentTimeMillis()
    shutdownServer()
    launchServer()
  }

  fun shouldAttemptCompletion(): Boolean {
    return settings.state.isEnabled
  }

  private fun settingsChanged(): Boolean {
    if (host == settings.state.host
        && port == settings.state.port) {
      return false
    }
    host = settings.state.host
    port = settings.state.port
    return true
  }

  private fun launchServer() {
    if (!isLocalHost(host ?: "")) {
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