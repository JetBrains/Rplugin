/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo.EMPTY
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.console.RConsoleRuntimeInfo
import org.jetbrains.r.packages.RequiredPackage

class LoadPackageFix(private val packageName: String,
                     private val runtimeInfo: RConsoleRuntimeInfo) : DependencyManagementFix(listOf(RequiredPackage(packageName))) {

  override fun getName(): String {
    return RBundle.message("load.library.fix.name", packageName)
  }

  override fun getFamilyName(): String {
    return RBundle.message("load.library.fix.family.name")
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    runBackgroundableTask(name, project, true) {
      runtimeInfo.loadPackage(packageName)
    }
  }

  override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = EMPTY
}