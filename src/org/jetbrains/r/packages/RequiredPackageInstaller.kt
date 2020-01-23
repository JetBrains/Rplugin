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
import com.intellij.webcore.packaging.PackageManagementService
import com.intellij.webcore.packaging.RepoPackage
import com.jetbrains.rd.util.ConcurrentHashMap
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.packages.remote.MissingPackageDetailsException
import org.jetbrains.r.packages.remote.PackageDetailsException
import org.jetbrains.r.packages.remote.RPackageManagementService
import org.jetbrains.r.packages.remote.UnresolvedPackageDetailsException
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

class RequiredPackageException(val missingPackages: List<RequiredPackage>) : RuntimeException() {
  override val message: String?
    get() = RBundle.message("required.package.exception.message", StringUtil.join(missingPackages, ", "))
}

data class RequiredPackage(val name: String, val minimalVersion: String = "") {
  private fun isVersionSet(): Boolean {
    return minimalVersion.isNotEmpty()
  }

  fun toFormat(isQuoted: Boolean): String {
    val versionString = if (!isVersionSet()) "" else " ($minimalVersion)"
    val nameString = if (isQuoted) "'$name'" else name
    return "$nameString$versionString"
  }
}

class RequiredPackageInstaller(private val project: Project) {

  private val rPackageManagementService = RPackageManagementService(project)
  private val installingPackages2minimalVersions = ConcurrentHashMap<String, String>()
  private val utilityNames2installationTasks = ConcurrentHashMap<String, InstallationTask>()

  init {
    RLibraryWatcher.subscribeAsync(project, RLibraryWatcher.TimeSlot.SECOND) {
      val names2tasks = utilityNames2installationTasks.toMap()  // Note: make a copy before iterating
      for ((utilityName, installationTask) in names2tasks) {
        val missingPackages = getMissingPackages(installationTask.requiredPackages)
        if (missingPackages.isEmpty()) {
          utilityNames2installationTasks.remove(utilityName)
          installationTask.allRequiredPackagesProcessed()
        }
      }
      val names2versions = installingPackages2minimalVersions.toMap()  // Note: make a copy before iterating
      for ((packageName, minimalVersion) in names2versions) {
        if (findRequiredPackage(RequiredPackage(packageName, minimalVersion)) != null) {
          installingPackages2minimalVersions.remove(packageName)
        }
      }
    }
  }

  /**
   * Use these method if you need some packages for your utility to work correctly.
   *
   * @param utilityName string name of your utility
   * @param packages    list of [required packages][RequiredPackage]
   * @param askUser     if true, the user will be shown a [notification][Notification] and installation of packages will start only after his consent.
   *                    It is not recommended to set it to false if the user has not given consent in any other form
   */
  fun installPackagesWithUserPermission(utilityName: String,
                                        packages: List<RequiredPackage>,
                                        askUser: Boolean = true): Promise<Unit> {
    val isUiMode = askUser && !ApplicationManager.getApplication().isUnitTestMode
    val missingPackages = getMissingPackages(packages)
    if (missingPackages.isEmpty()) {
      return AsyncPromise<Unit>().apply { setResult(Unit) }
    }

    val task = findOrCreateInstallationTask(utilityName, missingPackages, isUiMode)
    if (isUiMode) {
      task.notification?.notify(project)
    }
    return task.promise
  }

  @Synchronized
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
    val packagesList = packages.joinToString(", ") { it.toFormat(false) }

    val notification = Notification(
      RBundle.message("required.package.notification.group.display"),
      RBundle.message("required.package.notification.title", utilityName),
      packagesList,
      NotificationType.INFORMATION
    )

    return registerInstallationTask(utilityName, packages, notification).also { task ->
      notification.addAction(object : NotificationAction(RBundle.message("required.package.notification.action")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          if (!task.isStarted && !task.isDone) {
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
    return packages.filter { findRequiredPackage(it) == null }
  }

  /**
   * Use it instead of [getMissingPackages] when a result should be returned immediately.
   * This method is **not** waiting for [interpreter][org.jetbrains.r.interpreter.RInterpreter] to be initialized
   * which can be useful in order to prevent both UI freezes and deadlocks (especially on IDE startup).
   * @return list of currently missing packages
   * or `null` if [interpreter][org.jetbrains.r.interpreter.RInterpreter] hasn't been initialized yet
   */
  fun getMissingPackagesOrNull(packages: List<RequiredPackage>): List<RequiredPackage>? {
    return RInterpreterManager.getInterpreter(project)?.let {
      getMissingPackages(packages)
    }
  }

  private fun findRequiredPackage(requiredPackage: RequiredPackage): RInstalledPackage? {
    return rPackageManagementService.findInstalledPackageByName(requiredPackage.name)?.takeIf { candidate ->
      candidate.version.isNewerOrSame(requiredPackage.minimalVersion)
    }
  }

  private fun startTask(task: InstallationTask) {
    task.isStarted = true
    val needInstall = registerRequiredPackages(task.requiredPackages)
    if (needInstall.isEmpty()) return
    val installPackageListener = object : RPackageManagementService.MultiListener {
      override fun operationStarted(packageNames: List<String>) {}

      override fun operationFinished(packageNames: List<String>,
                                     errorDescriptions: List<PackageManagementService.ErrorDescription?>) {
        for ((packageName, errorDescription) in packageNames.zip(errorDescriptions)) {
          packageProcessed(packageName, errorDescription?.message)
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

  @Synchronized
  private fun registerRequiredPackages(requiredPackages: List<RequiredPackage>): List<RequiredPackage> {
    return mutableListOf<RequiredPackage>().also { needInstall ->
      for (required in requiredPackages) {
        val previousVersion = installingPackages2minimalVersions[required.name]
        if (previousVersion == null || !previousVersion.isNewerOrSame(required.minimalVersion)) {
          installingPackages2minimalVersions[required.name] = required.minimalVersion
        }
        if (previousVersion == null) {
          needInstall.add(required)
        }
      }
    }
  }

  private fun reloadPackagesIfNecessary() {
    if (!rPackageManagementService.arePackageDetailsLoaded) {
      rPackageManagementService.reloadAllPackages()
    }
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
        packageProcessed(required.name, message)
        null
      }
    }
  }

  private fun packageProcessed(packageName: String, errorMessage: String?) {
    installingPackages2minimalVersions.remove(packageName)
    val installationTasks = utilityNames2installationTasks.values.toList()  // Note: make a copy before iterating
    for (installationTask in installationTasks) {
      packageProcessedForTask(installationTask, packageName, errorMessage)
    }
  }

  private fun packageProcessedForTask(task: InstallationTask, packageName: String, errorMessage: String?) {
    task.packageProcessed(packageName, errorMessage)
    if (task.isDone) {
      utilityNames2installationTasks.remove(task.utilityName)
    }
  }

  private class InstallationTask(val utilityName: String,
                                 val requiredPackages: List<RequiredPackage>,
                                 val notification: Notification?) {
    private val unprocessedPackages = ConcurrentSkipListSet<String>(requiredPackages.map { it.name })
    private val errorNotified = AtomicBoolean(false)
    val promise = AsyncPromise<Unit>()

    val isDone: Boolean
      get() = unprocessedPackages.isEmpty()

    @Volatile
    var isStarted = false

    fun packageProcessed(packageName: String, errorMessage: String?) {
      if (unprocessedPackages.remove(packageName)) {
        if (errorMessage != null) {
          errorOccurred(packageName, errorMessage)
        } else if (unprocessedPackages.isEmpty()) {
          allRequiredPackagesProcessed()
        }
      }
    }

    fun allRequiredPackagesProcessed() {
      unprocessedPackages.clear()
      expire()
      promise.setResult(Unit)  // Note: does nothing if it's already been failed
    }

    private fun errorOccurred(packageName: String, errorMessage: String) {
      if (errorNotified.compareAndSet(false, true)) {
        expire()
        promise.setError("$INSTALLATION_ERROR_MESSAGE:\n$packageName ($errorMessage)")
      }
    }

    private fun expire() {
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

