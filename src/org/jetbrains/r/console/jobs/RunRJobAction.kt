/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import org.jetbrains.r.RFileType
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.settings.RSettings

class RunRJobAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    showDialog(project)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabled = project?.isDefault == false && RSettings.getInstance(project).interpreterPath.isNotEmpty()
  }

  companion object {
    fun showDialog(project: Project) {
      val selectedFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
      val scriptPath = if (selectedFile?.fileType == RFileType) selectedFile.path else ""
      val workingDirectory = RConsoleManager.getInstance(project).currentConsoleOrNull?.rInterop?.workingDir?.takeIf { it.isNotEmpty() }
                             ?: project.basePath ?: ""
      RRunJobDialog(project, scriptPath, workingDirectory).show()
    }
  }
}

