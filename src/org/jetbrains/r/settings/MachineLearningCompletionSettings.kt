package org.jetbrains.r.settings

import com.intellij.openapi.components.*


@State(
  name = "org.jetbrains.r.settings.MachineLearningCompletionSettings",
  storages = [Storage(value="MachineLearningCompletion.xml")]
)
class MachineLearningCompletionSettings: SimplePersistentStateComponent<MachineLearningCompletionSettings.State>(State()) {
  class State(host: String? = DEFAULT_HOST, port: Int = DEFAULT_PORT, isEnabled: Boolean = DEFAULT_IS_ENABLED) : BaseState() {
    var host by string(host)
    var port by property(port)
    var isEnabled by property(isEnabled)
  }

  companion object {
    fun getInstance() = service<MachineLearningCompletionSettings>()

    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_PORT = 7337
    const val DEFAULT_IS_ENABLED = true
  }

  fun copyState(): State {
    return State(state.host, state.port, state.isEnabled)
  }
}
