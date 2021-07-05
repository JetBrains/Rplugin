package org.jetbrains.r.editor.mlcompletion.update

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.roots.ProjectFileIndex
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RFileType
import org.jetbrains.r.editor.mlcompletion.update.MachineLearningCompletionNotifications.showPopup
import org.jetbrains.r.settings.MachineLearningCompletionSettings
import java.util.concurrent.atomic.AtomicBoolean

class MachineLearningCompletionProjectOpenListener : ProjectManagerListener {

  companion object {
    val notifiedAnyProject = AtomicBoolean(false)
  }

  override fun projectOpened(project: Project) {
    if (notifiedAnyProject.get() || !MachineLearningCompletionSettings.getInstance().state.isEnabled) {
      return
    }

    runAsync {
      if (isNotRProject(project) || !notifiedAnyProject.compareAndSet(false, true)) {
        return@runAsync
      }

      val modelDownloaderService = MachineLearningCompletionDownloadModelService.getInstance()
      modelDownloaderService.initiateUpdateCycle(isModal = false, reportIgnored = false) { (artifactsToUpdate, size) ->
        if (artifactsToUpdate.isNotEmpty()) {
          showPopup(project, artifactsToUpdate, size)
        }
      }
    }
  }

  private fun isNotRProject(project: Project): Boolean =
    ProjectFileIndex.getInstance(project).iterateContent { it.fileType != RFileType }
}
