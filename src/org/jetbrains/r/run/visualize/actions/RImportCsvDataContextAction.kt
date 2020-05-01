/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize.actions

import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RBundle
import org.jetbrains.r.R_LOGO_16
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.run.visualize.RImportCsvDataDialog

class RImportCsvDataContextAction : DumbAwareAction(TITLE, null, R_LOGO_16) {
  override fun actionPerformed(e: AnActionEvent) {
    e.project?.let { project ->
      e.selectedFiles?.firstOrNull()?.let { file ->
        getCurrentInteropAsync(project).onSuccess { interop ->
          RImportCsvDataDialog(project, interop, project, file.path).show()
        }
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val isVisible = e.selectedFiles?.let { it.size == 1 && isApplicable(it.first()) } ?: false
    e.presentation.isEnabled = isVisible && isEnabled(e.project)
    e.presentation.isVisible = isVisible
  }

  private fun isEnabled(project: Project?): Boolean {
    return project?.let { areRequirementsSatisfied(it) } ?: false
  }

  private fun areRequirementsSatisfied(project: Project): Boolean {
    val installer = RequiredPackageInstaller.getInstance(project)
    val missing = installer.getMissingPackagesOrNull(REQUIREMENTS)
    return missing?.isEmpty() == true
  }

  private fun getCurrentInteropAsync(project: Project): Promise<RInterop> {
    return RConsoleManager.getInstance(project).currentConsoleAsync.then { it.rInterop }
  }

  private fun isApplicable(file: VirtualFile): Boolean {
    return !file.isDirectory && !ScratchUtil.isScratch(file)
  }

  private val AnActionEvent.selectedFiles: Array<VirtualFile>?
    get() = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

  companion object {
    private val REQUIREMENTS = listOf(RequiredPackage("readr"))
    private val TITLE = RBundle.message("import.data.action.csv.title")
  }
}
