package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionNotifications.askForUpdate

class MachineLearningCompletionProjectOpenListener : ProjectManagerListener {

  override fun projectOpened(project: Project) {
    val modelDownloaderService = MachineLearningCompletionDownloadModelService.getInstance()
    modelDownloaderService.initiateUpdateCycle(false) { (artifactsToUpdate, size) ->
      askForUpdate(project, artifactsToUpdate, size)
    }
  }

}
