// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiEditorUtil
import icons.org.jetbrains.r.RBundle
import icons.org.jetbrains.r.intentions.DependencyManagementFix
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller

/**
 * Also see http://stackoverflow.com/questions/4090169/elegant-way-to-check-for-missing-packages-and-install-them
 */
class InstallLibraryFix(override val packageName: String) : DependencyManagementFix() {

  override fun getName(): String {
    return RBundle.message("install.library.fix.name", packageName)
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val missing = listOf(RequiredPackage(packageName))
    RequiredPackageInstaller.getInstance(project).installPackagesWithUserPermission(getName(), missing, false)
      .onError { notifyError(descriptor, it) }
  }

  private fun notifyError(descriptor: ProblemDescriptor, e: Throwable?) {
    runInEdt {
      PsiEditorUtil.Service.getInstance().findEditorByPsiElement(descriptor.psiElement)?.let { editor ->
        HintManager.getInstance().showErrorHint(editor, e?.message ?: UNKNOWN_ERROR_MESSAGE)
      }
    }
  }

  companion object {
    private val UNKNOWN_ERROR_MESSAGE = RBundle.message("notification.unknown.error.message")
  }
}
