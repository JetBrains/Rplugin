/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.intentions

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiFile
import com.intellij.r.psi.RBundle
import org.jetbrains.r.packages.RequiredPackage

abstract class DependencyManagementFix(protected val missingPackages: List<RequiredPackage> = emptyList()) : LocalQuickFix {

  protected inline fun <reified T : DependencyManagementFix> getAllPackagesWithSameQuickFix(
    file: PsiFile, project: Project, localInspectionTool: LocalInspectionTool): List<RequiredPackage> {

    val allMissingPackageProblems = runReadAction {
      InspectionEngine.runInspectionOnFile(file, LocalInspectionToolWrapper(localInspectionTool),
                                           InspectionManager.getInstance(project).createNewGlobalContext())
    }

    return allMissingPackageProblems
      .flatMap { problem ->
        problem.fixes?.flatMap { fix ->
          (fix as? T)?.missingPackages ?: emptyList()
        } ?: emptyList()
      }
      .distinct()
  }

  override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

  companion object {
    private val UNKNOWN_ERROR_MESSAGE = RBundle.message("notification.unknown.error.message")
    private val NOTIFICATION_GROUP_ID = RBundle.message("install.all.library.fix.notification.group.id")
    @NlsSafe
    private val NOTIFICATION_TITLE = RBundle.message("install.all.library.fix.notification.title")

    private val Throwable?.messageOrDefault: String
      get() = this?.message ?: UNKNOWN_ERROR_MESSAGE

    fun showErrorNotification(project: Project, e: Throwable?) {
      val notification = Notification(NOTIFICATION_GROUP_ID, NOTIFICATION_TITLE, e.messageOrDefault, NotificationType.ERROR)
      notification.notify(project)
    }
  }
}