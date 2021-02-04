package org.jetbrains.r.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*


@State(
  name = "org.jetbrains.r.settings.MachineLearningCompletionSettings",
  storages = [Storage(value = "MachineLearningCompletion.xml")]
)
class MachineLearningCompletionSettings : SimplePersistentStateComponent<MachineLearningCompletionSettings.State>(State()) {
  class State(host: String? = DEFAULT_HOST,
              port: Int = DEFAULT_PORT,
              isEnabled: Boolean = DEFAULT_IS_ENABLED,
              lastCheckedForUpdatesMs: Long = DEFAULT_LAST_CHECKED_FOR_UPDATES) : BaseState() {
    var host by string(host)
    var port by property(port)
    var isEnabled by property(isEnabled)
    var lastCheckedForUpdatesMs by property(lastCheckedForUpdatesMs)

    fun hostOrDefault(): String {
      return host ?: DEFAULT_HOST
    }
  }

  companion object {
    fun getInstance() = service<MachineLearningCompletionSettings>()

    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_PORT = 7337
    const val DEFAULT_IS_ENABLED = true
    const val DEFAULT_LAST_CHECKED_FOR_UPDATES: Long = -1L

    fun notifySettingsChanged(beforeState: State,
                              afterState: State) {
      ApplicationManager.getApplication().messageBus
        .syncPublisher(MachineLearningCompletionSettingsChangeListener.R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC)
        .settingsChanged(beforeState, afterState)
    }
  }

  fun copyState(): State {
    return State(state.host, state.port, state.isEnabled)
  }

  fun reportUpdateCheck() {
    val beforeState = copyState()
    state.lastCheckedForUpdatesMs = System.currentTimeMillis()
    notifySettingsChanged(beforeState, state)
  }
}
