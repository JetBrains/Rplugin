package org.jetbrains.r.editor.mlcompletion.update

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.roots.ProjectFileIndex
import org.jetbrains.r.RFileType
import org.jetbrains.r.editor.mlcompletion.update.MachineLearningCompletionNotifications.showPopup
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import java.util.concurrent.atomic.AtomicBoolean

class MachineLearningCompletionProjectOpenListener : ProjectManagerListener {

  companion object {
    val notifiedAnyProject = AtomicBoolean(false)
  }

  override fun projectOpened(project: Project) {
    if (!MachineLearningCompletionSettings.getInstance().state.isEnabled
        || isNotRProject(project)) {
      return
    }
    if (!notifiedAnyProject.compareAndSet(false, true)) {
      return
    }

    val modelDownloaderService = MachineLearningCompletionDownloadModelService.getInstance()
    modelDownloaderService.initiateUpdateCycle(isModal = false, reportIgnored = false) { (artifactsToUpdate, size) ->
      if (artifactsToUpdate.isNotEmpty()) {
        showPopup(project, artifactsToUpdate, size)
      }
    }
  }

  private fun isNotRProject(project: Project) =
    ProjectFileIndex.getInstance(project).iterateContent { it.fileType != RFileType }
}
