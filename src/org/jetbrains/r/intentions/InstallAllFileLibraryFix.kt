/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.intentions

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.inspections.MissingPackageInspection
import org.jetbrains.r.intentions.InstallLibraryFix
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller

class InstallAllFileLibraryFix : DependencyManagementFix() {

  override fun getName(): String {
    return RBundle.message("install.all.library.fix.name")
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val missing = findAllMissingPackages(project, descriptor)
    RequiredPackageInstaller.getInstance(project).installPackagesWithUserPermission(getName(), missing, false)
      .onError { notifyError(project, it) }
  }

  private fun findAllMissingPackages(project: Project, descriptor: ProblemDescriptor): List<RequiredPackage> {
    val file = descriptor.psiElement.containingFile
    val packageNames = getAllPackagesWithSameQuickFix<InstallLibraryFix>(file, project, MissingPackageInspection())
    return packageNames.map { RequiredPackage(it) }
  }

  private fun notifyError(project: Project, e: Throwable?) {
    val message = e?.message ?: UNKNOWN_ERROR_MESSAGE
    val notification = Notification(NOTIFICATION_GROUP_ID, NOTIFICATION_TITLE, message, NotificationType.ERROR)
    notification.notify(project)
  }

  companion object {
    private val UNKNOWN_ERROR_MESSAGE = RBundle.message("notification.unknown.error.message")
    private val NOTIFICATION_GROUP_ID = RBundle.message("install.all.library.fix.notification.group.id")
    private val NOTIFICATION_TITLE = RBundle.message("install.all.library.fix.notification.title")
  }
}