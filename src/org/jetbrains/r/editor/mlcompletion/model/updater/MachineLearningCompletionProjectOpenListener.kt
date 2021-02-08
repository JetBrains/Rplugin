package org.jetbrains.r.editor.mlcompletion.model.updater

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.r.editor.mlcompletion.model.updater.MachineLearningCompletionNotifications.askForUpdate
import java.util.concurrent.atomic.AtomicBoolean

class MachineLearningCompletionProjectOpenListener : ProjectManagerListener {

  companion object {
    val notifiedAnyProject = AtomicBoolean(false)
  }

  override fun projectOpened(project: Project) {
    if (!notifiedAnyProject.compareAndSet(false, true)) {
      return
    }

    val modelDownloaderService = MachineLearningCompletionDownloadModelService.getInstance()
    modelDownloaderService.initiateUpdateCycle(false) { (artifactsToUpdate, size) ->
      if (artifactsToUpdate.isNotEmpty()) {
        askForUpdate(project, artifactsToUpdate, size)
      }
    }
  }

}
