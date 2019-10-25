/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.webcore.packaging.InstalledPackage
import com.intellij.webcore.packaging.PackageManagementService
import com.intellij.webcore.packaging.RepoPackage
import com.jetbrains.rd.util.ConcurrentHashMap
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.packages.remote.*
import org.jetbrains.r.packages.remote.ui.RPackageServiceListener
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class RequiredPackageException(val missingPackages: List<RequiredPackage>) : RuntimeException() {
  override val message: String?
    get() = RBundle.message("required.package.exception.message", StringUtil.join(missingPackages, ", "))
}

class RequiredPackage(val name: String, val minimalVersion: String) {
  constructor(name: String) : this(name, "")

  fun isVersionSet(): Boolean {
    return minimalVersion.isNotEmpty()
  }
}

class InstallationPackageException(message: String, names2errors: Map<String, String>)
  : RuntimeException(createErrorMessage(message, names2errors))
{
  companion object {
    private fun createErrorMessage(message: String, names2errors: Map<String, String>): String {
      val details = names2errors.toList().joinToString("\n") { "${it.first} (${it.second})" }
      return "$message:\n$details"
    }
  }
}

interface RequiredPackageListener {
  fun onPackagesInstalled()
  fun onErrorOccurred(e: InstallationPackageException)
}

class RequiredPackageInstaller(private val project: Project) {

  private val rPackageManagementService = RPackageManagementService(project, object : RPackageServiceListener {
    override fun onTaskStart() {}
    override fun onTaskFinish() {}
  })

  private val notificationShown = ConcurrentSkipListSet<String>()
  private val packageInstallationErrorDescriptions = ConcurrentHashMap<String, String>()
  private val installationTasks = CopyOnWriteArrayList<InstallationTask>()

  init {
    RLibraryWatcher.subscribeAsync(project, RLibraryWatcher.TimeSlot.EARLY) {
      val installedPackages = rPackageManagementService.installedPackages
      for (installationTask in installationTasks) {
        val missingPackages = getMissingPackages(installationTask.requiredPackage, installedPackages)
        if (missingPackages.isEmpty()) {
          installationTasks.remove(installationTask)
          notificationShown.remove(installationTask.utilityName)
          installationTask.allRequiredPackagesInstalled()
        }
      }

      val installedPackagesNames = installedPackages.map { it.name }
      for ((packageName, errorDescription) in packageInstallationErrorDescriptions) {
        if (errorDescription.isNotBlank() && installedPackagesNames.contains(packageName)) {
          packageInstallationErrorDescriptions.remove(packageName)
        }
      }
    }
  }

  /**
   * Use these method if you need some packages for your utility to work correctly.
   *
   * @param utilityName string name of your utility
   * @param packages    list of [required packages][RequiredPackage]
   * @param listener    package installation listener
   * @param askUser     if true, the user will be shown a [notification][Notification] and installation of packages will start only after his consent.
   *                    It is not recommended to set it to false if the user has not given consent in any other form
   *
   * @see [RequiredPackageListener]
   */
  fun installPackagesWithUserPermission(utilityName: String,
                                        packages: List<RequiredPackage>,
                                        listener: RequiredPackageListener?,
                                        askUser: Boolean = true) {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      listener?.onPackagesInstalled()
      return
    }

    val missingPackages = getMissingPackages(packages)
    if (missingPackages.isEmpty()) {
      listener?.onPackagesInstalled()
      return
    }

    if (!askUser) {
      installationTasks.add(InstallationTask(utilityName, packages, null, listener))
      installPackages(missingPackages)
      return
    }

    if (!notificationShown.add(utilityName)) {
      return
    }

    val packagesList = missingPackages.joinToString(", ") {
      val version = if (!it.isVersionSet()) "" else " (${it.minimalVersion})"
      "${it.name}$version"
    }

    val notification = Notification(
      RBundle.message("required.package.notification.group.display"),
      RBundle.message("required.package.notification.title", utilityName),
      packagesList,
      NotificationType.INFORMATION
    ).addAction(object : NotificationAction(RBundle.message("required.package.notification.action")) {
      override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        notification.expire()
        installPackages(getMissingPackages(packages))
      }
    })

    installationTasks.add(InstallationTask(utilityName, packages, notification, listener))
    notification.notify(project)
  }

  fun getMissingPackages(packages: List<RequiredPackage>,
                         installedPackages: Collection<InstalledPackage> = rPackageManagementService.installedPackages): List<RequiredPackage> {
    return if (ApplicationManager.getApplication().isUnitTestMode) {
      emptyList()
    }
    else {
      packages.filter { requiredPackage ->
        installedPackages.all { it.name != requiredPackage.name || it.version < requiredPackage.minimalVersion }
      }
    }
  }


  private fun installPackages(missingPackages: List<RequiredPackage>) {
    val needInstall = mutableListOf<RequiredPackage>()
    for (missingPackage in missingPackages) {
      packageInstallationErrorDescriptions.getOrPut(missingPackage.name) {
        needInstall.add(missingPackage)
        ""
      }
    }

    if (needInstall.isEmpty()) return
    val installPackageListener = object : PackageManagementService.Listener {
      override fun operationStarted(packageName: String) {}

      override fun operationFinished(packageName: String,
                                     errorDescription: PackageManagementService.ErrorDescription?) {
        if (errorDescription != null) {
          packageInstallationErrorDescriptions.replace(packageName, errorDescription.message)
          errorOccurred()
        }
        else {
          packageInstallationErrorDescriptions.remove(packageName)
        }
      }
    }

    runBackgroundableTask(RBundle.message("required.package.background.preparing.title"), project, true) {
      needInstall.forEach {
        val packageName = it.name
        try {
          RepoUtils.getPackageDetails(project) ?: rPackageManagementService.reloadAllPackages()
          rPackageManagementService.installPackages(listOf(RepoPackage(packageName, null)), true, installPackageListener)
        }
        catch (e: PackageDetailsException) {
          val message = when (e) {
            is MissingPackageDetailsException -> MISSING_DETAILS_ERROR_MESSAGE
            is UnresolvedPackageDetailsException -> getUnresolvedPackageErrorMessage(packageName)
          }
          packageInstallationErrorDescriptions.replace(packageName, message)
          errorOccurred()
        }
      }
    }
  }

  private fun errorOccurred() {
    val installedPackages = rPackageManagementService.installedPackages
    for (installationTask in installationTasks) {
      val missingPackages = getMissingPackages(installationTask.requiredPackage, installedPackages)
      if (missingPackages.isNotEmpty() && missingPackages.all { !packageInstallationErrorDescriptions[it.name].isNullOrBlank() }) {
        installationTask.errorOccurred(InstallationPackageException(INSTALLATION_ERROR_MESSAGE, packageInstallationErrorDescriptions))
      }
    }
  }

  private class InstallationTask(val utilityName: String,
                                 val requiredPackage: List<RequiredPackage>,
                                 private val notification: Notification?,
                                 private val listener: RequiredPackageListener?) {
    private var errorNotified = AtomicBoolean(false)

    fun allRequiredPackagesInstalled() {
      expire()
      runAsync {
        listener?.onPackagesInstalled()
      }
    }

    fun errorOccurred(e: InstallationPackageException) {
      errorNotified.compareAndSet(false, {
        expire()
        runAsync {
          listener?.onErrorOccurred(e)
        }
        true
      }())
    }

    fun expire() {
      notification?.expire()
    }
  }

  companion object {
    private val INSTALLATION_ERROR_MESSAGE = RBundle.message("required.package.installation.error.message")
    private val MISSING_DETAILS_ERROR_MESSAGE = RBundle.message("required.package.missing.details.error.message")
    private val UNKNOWN_ERROR_MESSAGE = RBundle.message("required.package.unknown.error.message")

    private fun getUnresolvedPackageErrorMessage(packageName: String): String {
      return RBundle.message("required.package.resolve.details.error.message", packageName)
    }

    fun getInstance(project: Project): RequiredPackageInstaller {
      return ServiceManager.getService(project, RequiredPackageInstaller::class.java)
    }
  }
}

