package org.jetbrains.r.editor.mlcompletion.update

import org.jetbrains.r.settings.MachineLearningCompletionSettings
import org.jetbrains.r.settings.MachineLearningCompletionSettingsChangeListener

class MachineLearningCompletionCheckForUpdatesOnEnabledListener : MachineLearningCompletionSettingsChangeListener {
  override fun settingsChanged(beforeState: MachineLearningCompletionSettings.State, afterState: MachineLearningCompletionSettings.State) {
    val becameEnabled = !beforeState.isEnabled && afterState.isEnabled
    if (!becameEnabled || !MachineLearningCompletionUpdateAction.canInitiateUpdateAction.get()) {
      return
    }

    MachineLearningCompletionDownloadModelService.getInstance()
      .initiateUpdateCycle(isModal = false, reportIgnored = false) { (artifacts, size) ->
        if (artifacts.any(MachineLearningCompletionRemoteArtifact::localIsMissing)) {
          MachineLearningCompletionNotifications.showPopup(null, artifacts, size)
        }
      }
  }
}