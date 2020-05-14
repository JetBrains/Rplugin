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
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.rinterop.RInterop

abstract class RImportDataContextAction(text: String, description: String) : DumbAwareAction(text, description, null) {
  protected abstract val supportedFormats: List<String>

  final override fun actionPerformed(e: AnActionEvent) {
    e.project?.let { project ->
      e.selectedFiles?.firstOrNull()?.let { file ->
        applyTo(project, file)
      }
    }
  }

  final override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project?.let { isEnabled(it) } ?: false
  }

  fun isApplicableTo(e: AnActionEvent): Boolean {
    return e.selectedFiles?.let { it.size == 1 && isApplicableTo(it.first()) } ?: false
  }

  fun isApplicableTo(file: VirtualFile): Boolean {
    return !file.isDirectory && !ScratchUtil.isScratch(file) && file.extension?.isSupportedFormat == true
  }

  fun applyTo(project: Project, file: VirtualFile) {
    getCurrentInteropOrNull(project)?.let { interop ->
      applyTo(project, interop, file)
    }
  }

  open fun isEnabled(project: Project): Boolean {
    return true
  }

  protected abstract fun applyTo(project: Project, interop: RInterop, file: VirtualFile)

  private val String.isSupportedFormat: Boolean
    get() = toLowerCase() in supportedFormats

  companion object {
    private val AnActionEvent.selectedFiles: Array<VirtualFile>?
      get() = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

    private fun getCurrentInteropOrNull(project: Project): RInterop? {
      return RConsoleManager.getInstance(project).currentConsoleOrNull?.rInterop
    }

    fun List<RequiredPackage>.areSatisfied(project: Project): Boolean {
      val installer = RequiredPackageInstaller.getInstance(project)
      val missing = installer.getMissingPackagesOrNull(this)
      return missing?.isEmpty() == true
    }
  }
}
