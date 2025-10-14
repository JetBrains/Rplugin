/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize.actions

import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.rinterop.RInteropImpl
import java.util.*

abstract class RImportDataContextAction(text: String, description: String) : DumbAwareAction(text, description, null) {
  protected abstract val supportedFormats: Array<String>

  protected open val suggestedFormats: Array<String>
    get() = supportedFormats

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

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  fun isApplicableTo(e: AnActionEvent): Boolean =
    e.selectedFiles?.let { it.size == 1 && isApplicableTo(it.first()) } ?: false

  private fun isApplicableTo(file: VirtualFile): Boolean =
    checkFile(file, supportedFormats)

  /**
   * Whether this import action should be **suggested** for a specified [file] opened in editor.
   * **Note:** this condition is stricter than [isApplicableTo].
   * For instance, if this action is used to import datasets from text files,
   * it must be **applicable to** `txt` files. On the other hand, the vast majority
   * of `txt` files are **not** datasets so it's **not suggested** using
   * this action for any opened `txt` file
   */
  fun isSuggestedFor(file: VirtualFile): Boolean = checkFile(file, suggestedFormats)

  private fun checkFile(file: VirtualFile, formats: Array<String>): Boolean =
    !file.isDirectory && !ScratchUtil.isScratch(file) && file.extension?.let { formats.contains(it.lowercase(Locale.ROOT)) } == true

  fun applyTo(project: Project, file: VirtualFile) {
    getCurrentInteropOrNull(project)?.let { interop ->
      applyTo(project, interop, file)
    }
  }

  open fun isEnabled(project: Project): Boolean {
    return true
  }

  protected abstract fun applyTo(project: Project, interop: RInteropImpl, file: VirtualFile)

  companion object {
    private val AnActionEvent.selectedFiles: Array<VirtualFile>?
      get() = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

    private fun getCurrentInteropOrNull(project: Project): RInteropImpl? {
      return RConsoleManager.getInstance(project).currentConsoleOrNull?.rInterop
    }

    fun List<RequiredPackage>.areSatisfied(project: Project): Boolean {
      val installer = RequiredPackageInstaller.getInstance(project)
      val missing = installer.getMissingPackagesOrNull(this)
      return missing?.isEmpty() == true
    }
  }
}
