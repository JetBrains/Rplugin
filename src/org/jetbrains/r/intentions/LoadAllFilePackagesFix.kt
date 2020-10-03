/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleRuntimeInfo
import org.jetbrains.r.inspections.UnresolvedReferenceInspection

class LoadAllFilePackagesFix(private val runtimeInfo: RConsoleRuntimeInfo) : DependencyManagementFix() {

  override fun getFamilyName(): String {
    return RBundle.message("load.all.library.fix.name")
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val file = descriptor.psiElement.containingFile

    runBackgroundableTask(RBundle.message("load.all.library.fix.background"), project, true) {
      val packageNames = getAllPackagesWithSameQuickFix<LoadPackageFix>(file, project, UnresolvedReferenceInspection())
      packageNames.forEach {
        runtimeInfo.loadPackage(it.name)
      }
    }
  }
}