/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.intentions

import com.intellij.codeInspection.InspectionEngine
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.webcore.packaging.PackageManagementService
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.packages.remote.ui.RPackageServiceListener

abstract class DependencyManagementFix : LocalQuickFix {

  open val packageName = ""

  override fun getFamilyName(): String {
    return RBundle.message("dependency.management.fix.family.name")
  }

  protected inline fun <reified T : DependencyManagementFix> getAllPackagesWithSameQuickFix(file: PsiFile,
                                                                                            project: Project,
                                                                                            localInspectionTool: LocalInspectionTool): List<String> {
    val allMissingPackageProblems = runReadAction {
      InspectionEngine.runInspectionOnFile(file,
                                           LocalInspectionToolWrapper(localInspectionTool),
                                           InspectionManager.getInstance(project).createNewGlobalContext())
    }

    return allMissingPackageProblems
      .mapNotNull { problem -> problem.fixes?.mapNotNull { (it as? T)?.packageName }?.firstOrNull() }
      .distinct()
  }

  companion object {
    val emptyRPackageServiceListener = object : RPackageServiceListener {
      override fun onTaskStart() {}
      override fun onTaskFinish() {}
    }

    val emptyPackageManagementServiceListener = object : PackageManagementService.Listener {
      override fun operationStarted(packageName: String) {}
      override fun operationFinished(packageName: String, errorDescription: PackageManagementService.ErrorDescription?) {}
    }
  }
}