/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RBundle
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller

object RMarkdownUtil {
  private val NOTIFICATION_GROUP = RBundle.message("rmarkdown.processor.notification.group.display")
  private val UNKNOWN_ERROR_MESSAGE = RBundle.message("notification.unknown.error.message")

  private val requiredPackages = listOf(
    RequiredPackage("rmarkdown", "1.16"),  // Note: minimal version of "1.16" fixes "Extension ascii_identifiers is not supported for markdown" error
    RequiredPackage("knitr")
  )

  fun areRequirementsSatisfied(project: Project): Boolean {
    return getMissingPackages(project)?.isEmpty() == true
  }

  /**
   * @return list of missing R Markdown's requirements or `null` if such a list cannot be formed right now
   * (notably when [interpreter][org.jetbrains.r.interpreter.RInterpreter] hasn't been initialized yet)
   */
  fun getMissingPackages(project: Project): List<RequiredPackage>? {
    return RequiredPackageInstaller.getInstance(project).getMissingPackagesOrNull(requiredPackages)
  }

  fun checkOrInstallPackages(project: Project, utilityName: String): Promise<Unit> {
    return RequiredPackageInstaller.getInstance(project).installPackagesWithUserPermission(utilityName, requiredPackages)
      .onError {
        notifyFailure(project, utilityName)
      }
  }

  private fun notifyFailure(project: Project, utilityName: String) {
    val title = makeNotificationTitle(utilityName)
    val content = makeNotificationContent(utilityName)
    Notification(NOTIFICATION_GROUP, title, content, NotificationType.ERROR).notify(project)
  }

  private fun makeNotificationTitle(utilityName: String): String {
    return RBundle.message("rmarkdown.rendering.packages.notification.title", utilityName)
  }

  private fun makeNotificationContent(utilityName: String): String {
    return RBundle.message("rmarkdown.rendering.packages.notification.content", utilityName)
  }
}
