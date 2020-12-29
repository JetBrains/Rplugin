package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class MachineLearningCompletionProjectOpenListener : ProjectManagerListener {
  override fun projectOpened(project: Project) {
    val modelDownloaderService = service<MachineLearningCompletionDownloadModelService>()
    modelDownloaderService.checkForUpdatesAndDownloadIfNeeded(project)
  }

  // TODO: think, whether you need to do some actions (stopping download, saving its state somehow),
  //  during and before exiting
}
