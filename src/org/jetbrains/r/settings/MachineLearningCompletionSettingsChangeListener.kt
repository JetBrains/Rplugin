package org.jetbrains.r.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic


fun interface MachineLearningCompletionSettingsChangeListener {

  companion object {
    private val R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC =
      Topic.create("RMachineLearningCompletion.Settings", MachineLearningCompletionSettingsChangeListener::class.java)

    fun notifySettingsChanged(beforeState: MachineLearningCompletionSettings.State,
                              afterState: MachineLearningCompletionSettings.State) {
      ApplicationManager.getApplication().messageBus
        .syncPublisher(R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC)
        .settingsChanged(beforeState, afterState)
    }
  }

  fun settingsChanged(beforeState: MachineLearningCompletionSettings.State,
                      afterState: MachineLearningCompletionSettings.State)

  fun subscribeWithDisposable(parentDisposable: Disposable) =
    ApplicationManager.getApplication().messageBus
      .connect(parentDisposable)
      .subscribe(R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC, this)
}
