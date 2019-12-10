/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.intentions

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.webcore.packaging.RepoPackage
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.inspections.MissingPackageInspection
import org.jetbrains.r.intentions.InstallLibraryFix
import org.jetbrains.r.packages.remote.PackageDetailsException
import org.jetbrains.r.packages.remote.RPackageManagementService
import org.jetbrains.r.packages.remote.RepoUtils

class InstallAllFileLibraryFix : DependencyManagementFix() {

  override fun getName(): String {
    return RBundle.message("install.all.library.fix.name")
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val file = descriptor.psiElement.containingFile
    runBackgroundableTask(RBundle.message("install.all.library.fix.background"), project, true) {
      val packageNames = getAllPackagesWithSameQuickFix<InstallLibraryFix>(file, project, MissingPackageInspection())

      val rPackageManagementService = RPackageManagementService(project, emptyRPackageServiceListener)
      if (RepoUtils.getPackageDetails(project) == null) {
        rPackageManagementService.reloadAllPackages()
      }

      val resolved = resolveAndNotify(project, rPackageManagementService, packageNames)
      if (resolved.isNotEmpty()) {
        rPackageManagementService.installPackages(resolved, false, emptyPackageManagementServiceListener)
      }
    }
  }

  private fun resolveAndNotify(project: Project, service: RPackageManagementService, packageNames: List<String>): List<RepoPackage> {
    return resolve(service, packageNames).also { resolved ->
      if (resolved.size != packageNames.size) {
        Notification(
          RBundle.message("install.all.library.fix.notification.group.id"),
          RBundle.message("install.all.library.fix.notification.title"),
          RBundle.message("install.all.library.fix.notification.content"),
          NotificationType.ERROR
        ).notify(project)
      }
    }
  }

  private fun resolve(service: RPackageManagementService, packageNames: List<String>): List<RepoPackage> {
    return packageNames.mapNotNull { packageName ->
      try {
        service.resolvePackage(RepoPackage(packageName, null))
      } catch (e: PackageDetailsException) {
        null
      }
    }
  }
}