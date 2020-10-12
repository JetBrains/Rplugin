package org.jetbrains.r.settings

import com.intellij.util.messages.Topic


val R_MACHINE_LEARNING_COMPLETION_SETTINGS_TOPIC =
  Topic.create("RMachineLearningCompletion.Settings", MachineLearningCompletionSettingsChangeListener::class.java)

interface MachineLearningCompletionSettingsChangeListener {
  fun settingsChanged(beforeState: MachineLearningCompletionSettings.State,
                      afterState: MachineLearningCompletionSettings.State)
}
