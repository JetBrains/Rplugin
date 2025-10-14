/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.interpreter.getLocalOrRemotePath
import org.jetbrains.r.rinterop.RInteropImpl
import org.jetbrains.r.run.visualize.RImportBaseDataDialog
import org.jetbrains.r.run.visualize.RImportDataUtil

class RImportBaseDataContextAction : RImportDataContextAction(
  RBundle.message("import.data.action.base.title"),
  RBundle.message("import.data.action.base.description")
) {
  override val supportedFormats = RImportDataUtil.supportedTextFormats
  override val suggestedFormats = RImportDataUtil.suggestedTextFormats

  override fun applyTo(project: Project, interop: RInteropImpl, file: VirtualFile) {
    RImportBaseDataDialog.show(project, interop, project, file.getLocalOrRemotePath(interop.interpreter))
  }
}
