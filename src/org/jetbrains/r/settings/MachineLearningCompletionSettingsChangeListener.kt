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

    private fun subscribeToDefaultTopic(listener: MachineLearningCompletionSettingsChangeListener,
                                        parentDisposable: Disposable?): Unit =
      ApplicationManager.getApplication().messageBus.run {
        if (parentDisposable == null) connect() else connect(parentDisposable)
      }.subscribe(R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC, listener)
  }

  fun settingsChanged(beforeState: MachineLearningCompletionSettings.State,
                      afterState: MachineLearningCompletionSettings.State)

  fun subscribe(): Unit = subscribeToDefaultTopic(this, null)

  fun subscribe(parentDisposable: Disposable): Unit = subscribeToDefaultTopic(this, parentDisposable)
}
