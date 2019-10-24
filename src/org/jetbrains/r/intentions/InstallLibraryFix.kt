// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiEditorUtil
import com.intellij.webcore.packaging.RepoPackage
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.intentions.DependencyManagementFix
import org.jetbrains.r.packages.remote.*


/**
 * Also see http://stackoverflow.com/questions/4090169/elegant-way-to-check-for-missing-packages-and-install-them
 */
class InstallLibraryFix(override val packageName: String) : DependencyManagementFix() {

  override fun getName(): String {
    return RBundle.message("install.library.fix.name", packageName)
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    runBackgroundableTask(RBundle.message("install.library.fix.background", packageName),
                          project, true) {
      try {
        val rPackageManagementService = RPackageManagementService(project, emptyRPackageServiceListener)
        if (RepoUtils.getPackageDetails(project) == null) {
          rPackageManagementService.reloadAllPackages()
        }

        rPackageManagementService
          .installPackages(listOf(RepoPackage(packageName, null)), false, emptyPackageManagementServiceListener)
      }
      catch (e: PackageDetailsException) {
        val message = when (e) {
          is MissingPackageDetailsException -> MISSING_DETAILS_ERROR_MESSAGE
          is UnresolvedPackageDetailsException -> getUnresolvedPackageErrorMessage(packageName)
        }
        runInEdt {
          PsiEditorUtil.Service.getInstance().findEditorByPsiElement(descriptor.psiElement)?.let {
            HintManager.getInstance().showErrorHint(it, message)
          }
        }
      }
    }
  }

  companion object {
    private val MISSING_DETAILS_ERROR_MESSAGE = RBundle.message("required.package.missing.details.error.message")

    private fun getUnresolvedPackageErrorMessage(packageName: String): String {
      return RBundle.message("install.library.fix.unresolved", packageName)
    }
  }
}
