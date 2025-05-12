/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.r.RFileType
import org.jetbrains.r.RPluginCoroutineScope
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.interpreter.RInterpreterManager

class RunRJobAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    showDialog(project)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabled = project?.isDefault == false && RInterpreterManager.getInstance(project).hasInterpreterLocation()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    fun showDialog(project: Project) {
      RPluginCoroutineScope.getScope(project)
        .launch(Dispatchers.Default + ModalityState.defaultModalityState().asContextElement()) {
          val interpreter = RInterpreterManager.getInstance(project).awaitInterpreter().getOrNull() ?: return@launch

          withContext(Dispatchers.EDT) {
            val selectedFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            val script = selectedFile?.takeIf { FileTypeRegistry.getInstance().isFileOfType(it, RFileType) }
            val workingDirectory =
              RConsoleManager.getInstance(project).currentConsoleOrNull?.rInterop?.workingDir?.takeIf { it.isNotEmpty() }
              ?: interpreter.basePath
            RRunJobDialog(interpreter, script, workingDirectory).show()
          }
        }
    }
  }
}

