package org.jetbrains.r.settings

import com.intellij.openapi.components.*


@State(
  name = "org.jetbrains.r.settings.MLCompletionSettings",
  storages = [Storage(value="MLCompletion.xml")]
)
class MLCompletionSettings: SimplePersistentStateComponent<MLCompletionSettings.State>(State()) {
  class State : BaseState() {
    var host by string("localhost")
    var port by property(7337)
    var isEnabled by property(true)
  }

  companion object {
    fun getInstance() = service<MLCompletionSettings>()
  }
}
