package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionNotifications.askForUpdate

class MachineLearningCompletionProjectOpenListener : ProjectManagerListener {

  companion object {
    private val modelDownloaderService = MachineLearningCompletionDownloadModelService.getInstance()
  }

  override fun projectOpened(project: Project) =
    modelDownloaderService.getArtifactsToDownloadDescriptorsAsync { artifactsToUpdate ->
      val size = modelDownloaderService.getArtifactsSize(artifactsToUpdate)
      if (artifactsToUpdate.isNotEmpty()) {
        askForUpdate(project, artifactsToUpdate, size)
      } else {
        MachineLearningCompletionDownloadModelService.isBeingDownloaded.set(false)
      }
    }
}
