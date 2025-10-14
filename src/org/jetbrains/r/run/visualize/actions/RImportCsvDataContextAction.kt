/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.interpreter.getLocalOrRemotePath
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.rinterop.RInteropImpl
import org.jetbrains.r.run.visualize.RImportCsvDataDialog
import org.jetbrains.r.run.visualize.RImportDataUtil

class RImportCsvDataContextAction : RImportDataContextAction(TITLE, DESCRIPTION) {
  override val supportedFormats = RImportDataUtil.supportedTextFormats
  override val suggestedFormats = RImportDataUtil.suggestedTextFormats

  override fun applyTo(project: Project, interop: RInteropImpl, file: VirtualFile) {
    RImportCsvDataDialog.show(project, interop, project, file.getLocalOrRemotePath(interop.interpreter))
  }

  override fun isEnabled(project: Project): Boolean {
    return REQUIREMENTS.areSatisfied(project)
  }

  companion object {
    private val REQUIREMENTS = listOf(RequiredPackage("readr"))
    private val DESCRIPTION = RBundle.message("import.data.action.csv.description")
    private val TITLE = RBundle.message("import.data.action.csv.title")
  }
}
