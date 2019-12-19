/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.packages.InstallationPackageException
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.packages.RequiredPackageListener

object RMarkdownUtil {
  private val NOTIFICATION_GROUP = RBundle.message("rmarkdown.processor.notification.group.display")
  private val UNKNOWN_ERROR_MESSAGE = RBundle.message("notification.unknown.error.message")

  private val requiredPackages = listOf(
    RequiredPackage("rmarkdown", "1.16"),  // Note: minimal version of "1.16" fixes "Extension ascii_identifiers is not supported for markdown" error
    RequiredPackage("knitr")
  )

  fun checkOrInstallPackages(project: Project, utilityName: String): Promise<Unit> {
    return AsyncPromise<Unit>().also { promise ->
      val listener = object : RequiredPackageListener {
        override fun onPackagesMissed(missingPackages: List<RequiredPackage>) {
          promise.setError(RBundle.message("run.chunk.executor.missedPackages") + missingPackages.joinToString { it.name })
        }

        override fun onPackagesInstalled() {
          promise.setResult(Unit)
        }

        override fun onErrorOccurred(e: InstallationPackageException) {
          val title = makeNotificationTitle(utilityName)
          val content = makeNotificationContent(utilityName)
          Notification(NOTIFICATION_GROUP, title, content, NotificationType.ERROR).notify(project)
          promise.setError(e.message ?: UNKNOWN_ERROR_MESSAGE)
        }
      }
      RequiredPackageInstaller.getInstance(project).installPackagesWithUserPermission(utilityName, requiredPackages, listener)
    }
  }

  private fun makeNotificationTitle(utilityName: String): String {
    return RBundle.message("rmarkdown.rendering.packages.notification.title", utilityName)
  }

  private fun makeNotificationContent(utilityName: String): String {
    return RBundle.message("rmarkdown.rendering.packages.notification.content", utilityName)
  }
}
