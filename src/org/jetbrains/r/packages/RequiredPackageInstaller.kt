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
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.packages.remote.*
import java.util.concurrent.atomic.AtomicBoolean

class RequiredPackageException(val missingPackages: List<RequiredPackage>) : RuntimeException() {
  override val message: String?
    get() = RBundle.message("required.package.exception.message", StringUtil.join(missingPackages, ", "))
}

data class RequiredPackage(val name: String, val minimalVersion: String = "") {
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

class RequiredPackageInstaller(private val project: Project) {

  private val rPackageManagementService = RPackageManagementService(project)
  private val packageInstallationErrorDescriptions = ConcurrentHashMap<String, String>()
  private val utilityNames2installationTasks = ConcurrentHashMap<String, InstallationTask>()

  init {
    RLibraryWatcher.subscribeAsync(project, RLibraryWatcher.TimeSlot.SECOND) {
      val names2tasks = utilityNames2installationTasks.toMap()  // Note: make a copy before iterating
      for ((utilityName, installationTask) in names2tasks) {
        val missingPackages = getMissingPackages(installationTask.requiredPackages)
        if (missingPackages.isEmpty()) {
          utilityNames2installationTasks.remove(utilityName)
          installationTask.allRequiredPackagesInstalled()
        }
      }
      for ((packageName, errorDescription) in packageInstallationErrorDescriptions) {
        if (errorDescription.isNotBlank() && rPackageManagementService.findInstalledPackageByName(packageName) != null) {
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
                                        askUser: Boolean = true): Promise<Unit> {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      return AsyncPromise<Unit>().apply { setResult(Unit) }
    }

    val missingPackages = getMissingPackages(packages)
    if (missingPackages.isEmpty()) {
      return AsyncPromise<Unit>().apply { setResult(Unit) }
    }

    val task = findOrCreateInstallationTask(utilityName, missingPackages, askUser)
    if (askUser) {
      task.notification?.notify(project)
    }
    return task.promise
  }

  private fun findOrCreateInstallationTask(utilityName: String, packages: List<RequiredPackage>, askUser: Boolean): InstallationTask {
    return utilityNames2installationTasks[utilityName] ?: createInstallationTask(utilityName, packages, askUser)
  }

  private fun createInstallationTask(utilityName: String, packages: List<RequiredPackage>, askUser: Boolean): InstallationTask {
    return if (!askUser) {
      createInstantInstallationTask(utilityName, packages)
    } else {
      createUserInstallationTask(utilityName, packages)
    }
  }

  private fun createInstantInstallationTask(utilityName: String, packages: List<RequiredPackage>): InstallationTask {
    return registerInstallationTask(utilityName, packages, null).also { task ->
      startTask(task)
    }
  }

  private fun createUserInstallationTask(utilityName: String, packages: List<RequiredPackage>): InstallationTask {
    val packagesList = packages.joinToString(", ") {
      val version = if (!it.isVersionSet()) "" else " (${it.minimalVersion})"
      "${it.name}$version"
    }

    val notification = Notification(
      RBundle.message("required.package.notification.group.display"),
      RBundle.message("required.package.notification.title", utilityName),
      packagesList,
      NotificationType.INFORMATION
    )

    return registerInstallationTask(utilityName, packages, notification).also { task ->
      notification.addAction(object : NotificationAction(RBundle.message("required.package.notification.action")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          if (!task.isStarted) {
            notification.expire()
            startTask(task)
          }
        }
      })
    }
  }

  private fun registerInstallationTask(utilityName: String, packages: List<RequiredPackage>, notification: Notification?): InstallationTask {
    return InstallationTask(utilityName, packages, notification).also { task ->
      utilityNames2installationTasks[utilityName] = task
    }
  }

  fun getMissingPackages(packages: List<RequiredPackage>): List<RequiredPackage> {
    return if (ApplicationManager.getApplication().isUnitTestMode) {
      emptyList()
    }
    else {
      packages.filter { findRequiredPackage(it) == null }
    }
  }

  private fun findRequiredPackage(requiredPackage: RequiredPackage): InstalledPackage? {
    return rPackageManagementService.findInstalledPackageByName(requiredPackage.name)?.takeIf {
      it.version >= requiredPackage.minimalVersion
    }
  }

  private fun startTask(task: InstallationTask) {
    task.isStarted = true
    val needInstall = mutableListOf<RequiredPackage>()
    for (missingPackage in task.requiredPackages) {
      packageInstallationErrorDescriptions.getOrPut(missingPackage.name) {
        needInstall.add(missingPackage)
        ""
      }
    }

    if (needInstall.isEmpty()) return
    val installPackageListener = object : RPackageManagementService.MultiListener {
      override fun operationStarted(packageNames: List<String>) {}

      override fun operationFinished(packageNames: List<String>,
                                     errorDescriptions: List<PackageManagementService.ErrorDescription?>) {
        for ((packageName, errorDescription) in packageNames.zip(errorDescriptions)) {
          if (errorDescription != null) {
            packageInstallationErrorDescriptions.replace(packageName, errorDescription.message)
            errorOccurred()
          }
          else {
            packageInstallationErrorDescriptions.remove(packageName)
          }
        }
      }
    }

    runBackgroundableTask(RBundle.message("required.package.background.preparing.title"), project, true) {
      reloadPackagesIfNecessary()
      val resolved = resolvePackages(needInstall)
      if (resolved.isNotEmpty()) {
        rPackageManagementService.installPackages(resolved, true, installPackageListener)
      }
    }
  }

  private fun reloadPackagesIfNecessary() {
    RepoUtils.getPackageDetails(project) ?: rPackageManagementService.reloadAllPackages()
  }

  private fun resolvePackages(packages: List<RequiredPackage>): List<RepoPackage> {
    return packages.mapNotNull { required ->
      try {
        rPackageManagementService.resolvePackage(RepoPackage(required.name, null))
      } catch (e: PackageDetailsException) {
        val message = when (e) {
          is MissingPackageDetailsException -> MISSING_DETAILS_ERROR_MESSAGE
          is UnresolvedPackageDetailsException -> getUnresolvedPackageErrorMessage(required.name)
        }
        packageInstallationErrorDescriptions.replace(required.name, message)
        errorOccurred()
        null
      }
    }
  }

  private fun errorOccurred() {
    val installationTasks = utilityNames2installationTasks.values.toList()  // Note: make a copy before iterating
    for (installationTask in installationTasks) {
      val missingPackages = getMissingPackages(installationTask.requiredPackages)
      if (missingPackages.isNotEmpty() && missingPackages.any { !packageInstallationErrorDescriptions[it.name].isNullOrBlank() }) {
        installationTask.errorOccurred(InstallationPackageException(INSTALLATION_ERROR_MESSAGE, packageInstallationErrorDescriptions))
      }
    }
  }

  private class InstallationTask(val utilityName: String,
                                 val requiredPackages: List<RequiredPackage>,
                                 val notification: Notification?) {
    private val errorNotified = AtomicBoolean(false)
    val promise = AsyncPromise<Unit>()

    @Volatile
    var isStarted = false

    fun allRequiredPackagesInstalled() {
      expire()
      runAsync {
        promise.setResult(Unit)
      }
    }

    fun errorOccurred(e: InstallationPackageException) {
      if (errorNotified.compareAndSet(false, true)) {
        expire()
        runAsync {
          promise.setError(e.message ?: UNKNOWN_ERROR_MESSAGE)
        }
      }
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

