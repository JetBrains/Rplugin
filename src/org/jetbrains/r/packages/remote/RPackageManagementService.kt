// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.CatchingConsumer
import com.intellij.webcore.packaging.InstalledPackage
import com.intellij.webcore.packaging.PackageManagementService
import com.intellij.webcore.packaging.RepoPackage
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.common.emptyExpiringList
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.documentation.SHOW_PACKAGE_DOCS
import org.jetbrains.r.execution.ExecuteExpressionUtils.getListBlocking
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil.DEFAULT_TIMEOUT
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.packages.remote.ui.RPackageServiceListener
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import java.util.concurrent.atomic.AtomicInteger

sealed class PackageDetailsException(message: String) : RuntimeException(message)

/**
 * This error is thrown when package manager is unable to gather list of available packages.
 * This can be caused by networking issues
 */
class MissingPackageDetailsException(message: String) : PackageDetailsException(message)

/**
 * This error is thrown when package manager cannot resolve particular package name.
 * This can be caused by missing repository or package name misspelling
 */
class UnresolvedPackageDetailsException(message: String) : PackageDetailsException(message)

class RPackageManagementService(private val project: Project,
                                private val serviceListener: RPackageServiceListener? = null) : PackageManagementService() {
  private val interpreterManager: RInterpreterManager  // Should be evaluated lazily otherwise it will break unit tests
    get() = RInterpreterManager.getInstance(project)

  private val interpreter: RInterpreter?
    get() {
      fun getInitializedManager(): RInterpreterManager {
        return interpreterManager.apply {
          if (!hasInterpreter()) {
            initializeInterpreter()
              .onError { LOGGER.warn("Unable to initialize interpreter") }
              .silentlyBlockingGet(DEFAULT_TIMEOUT)
          }
        }
      }

      return getInitializedManager().interpreter
    }

  private val provider: RepoProvider
    get() = RepoProvider.getInstance(project)

  private val numScheduledOperations = AtomicInteger(0)

  @Volatile
  private var lastInstalledPackages: ExpiringList<RInstalledPackage>? = null

  val arePackageDetailsLoaded: Boolean
    get() = provider.name2AvailablePackages != null

  fun isPackageLoaded(packageName: String): Boolean {
    val currentConsoleOrNull = RConsoleManager.getInstance(project).currentConsoleOrNull ?: return false
    return currentConsoleOrNull.rInterop.loadedPackages.keys.contains(packageName)
  }

  fun loadPackage(packageName: String) {
    RConsoleManager.getInstance(project).currentConsoleOrNull?.rInterop?.loadLibrary(packageName)
  }

  fun unloadPackage(packageName: String, withDynamicLibrary: Boolean) {
    RConsoleManager.getInstance(project).currentConsoleOrNull?.rInterop?.unloadLibrary(packageName, withDynamicLibrary)
  }

  override fun getAllRepositories(): List<String> {
    val repositorySelections = getListBlocking("repositories selections") {
      provider.repositorySelectionsAsync
    }
    return repositorySelections.map { it.first.url }
  }

  @Deprecated("")
  override fun getAllPackages(): List<RepoPackage> {
    val cached = allPackagesCached
    return if (cached.isNotEmpty()) cached else reloadAllPackages()
  }

  override fun getAllPackagesCached(): List<RepoPackage> {
    return getListBlocking("cache of available packages") {
      provider.allPackagesCachedAsync
    }
  }

  override fun reloadAllPackages(): List<RepoPackage> {
    return getListBlocking("available packages") {
      provider.loadAllPackagesAsync()
    }
  }

  override fun getInstalledPackages(): List<RInstalledPackage> {
    return lastInstalledPackages?.takeIf { it.hasNotExpired } ?: loadInstalledPackages().also { installed ->
      lastInstalledPackages = installed
    }
  }

  private fun loadInstalledPackages(): ExpiringList<RInstalledPackage> {
    val installed = interpreter?.withAutoUpdate { installedPackages } ?: emptyExpiringList()
    return installed.filter { it.isUser }.map { it }
  }

  fun findInstalledPackageByName(name: String): RInstalledPackage? {
    return interpreter?.getPackageByName(name)
  }

  private fun onOperationStart() {
    numScheduledOperations.incrementAndGet()
    serviceListener?.onTaskStart()
  }

  private fun onOperationStop() {
    val numOperationsLeft = numScheduledOperations.decrementAndGet()
    if (numOperationsLeft <= 0) {
      serviceListener?.onTaskFinish()
    }
  }

  override fun installPackage(
    repoPackage: RepoPackage,
    version: String?,
    forceUpgrade: Boolean,
    extraOptions: String?,
    listener: Listener,
    installToUser: Boolean
  ) {
    val multiListener = convertToInstallMultiListener(listener)
    installPackages(listOf(repoPackage), forceUpgrade, multiListener)
  }

  @Throws(PackageDetailsException::class)
  fun installPackages(packages: List<RepoPackage>, forceUpgrade: Boolean, listener: MultiListener) {
    val packageNames = packages.map { it.name }
    val manager = RPackageTaskManager(interpreter, project, getTaskListener(packageNames, listener))
    onOperationStart()
    packages.map { resolvePackage(it) }.let { them ->
      if (forceUpgrade) {
        manager.update(them)
      }
      else {
        manager.install(them)
      }
    }
  }

  @Throws(PackageDetailsException::class)
  fun resolvePackage(repoPackage: RepoPackage): RepoPackage {
    return if (repoPackage.repoUrl == null || repoPackage.latestVersion == null) {
      val name2Packages = provider.name2AvailablePackages ?: throw MissingPackageDetailsException("Package mapping is not set")
      val filled = name2Packages[repoPackage.name]
      filled ?: throw UnresolvedPackageDetailsException("Can't get details for package '" + repoPackage.name + "'")
    } else {
      repoPackage
    }
  }

  override fun canInstallToUser(): Boolean {
    return false
  }

  fun canUninstallPackage(installedPackage: RInstalledPackage): Boolean {
    return interpreter?.getLibraryPathByName(installedPackage.name)?.isWritable ?: false
  }

  override fun uninstallPackages(installedPackages: List<InstalledPackage>, listener: Listener) {
    val packageNames = installedPackages.map { it.name }
    val multiListener = convertToUninstallMultiListener(listener)
    val manager = RPackageTaskManager(interpreter, project, getTaskListener(packageNames, multiListener))
    onOperationStart()
    val rInstalledPackages = installedPackages.map { it as RInstalledPackage }
    manager.uninstall(rInstalledPackages)
  }

  override fun fetchPackageVersions(s: String, consumer: CatchingConsumer<List<String>, Exception>) {
    consumer.consume(listOf())
  }

  override fun fetchPackageDetails(packageName: String, consumer: CatchingConsumer<String, Exception>) {
    val repoPackage = provider.name2AvailablePackages?.get(packageName)
    consumer.consume(RepoUtils.formatDetails(repoPackage))
  }

  fun fetchLatestVersion(packageName: String): String? {
    return provider.name2AvailablePackages?.get(packageName)?.latestVersion
  }

  private fun getTaskListener(packageNames: List<String>, listener: MultiListener): RPackageTaskManager.TaskListener {
    return object : RPackageTaskManager.TaskListener {
      override fun started() {
        listener.operationStarted(packageNames)
      }

      override fun finished(exceptions: List<ExecutionException?>) {
        onOperationStop()
        listener.operationFinished(packageNames, toErrorDescriptions(exceptions))
      }
    }
  }

  fun navigateToPackageDocumentation(pkg: RInstalledPackage) {
    RToolWindowFactory.showDocumentation(RElementFactory.buildRFileFromText(project, "$SHOW_PACKAGE_DOCS(${pkg.name})").firstChild)
  }

  interface MultiListener {
    fun operationStarted(packageNames: List<String>)
    fun operationFinished(packageNames: List<String>, errorDescriptions: List<ErrorDescription?>)
  }

  companion object {
    private val LOGGER = Logger.getInstance(RPackageManagementService::class.java)

    internal fun toErrorDescriptions(exceptions: List<ExecutionException?>): List<ErrorDescription?> {
      return exceptions.map { toErrorDescription(it) }
    }

    internal fun toErrorDescription(exception: ExecutionException?): ErrorDescription? {
      return exception?.let { e ->
        if (e is RExecutionException) {
          ErrorDescription(e.message ?: "Unknown error", e.command, e.stderr, null)
        }
        else {
          ErrorDescription(e.message ?: "Unknown error", null, null, null)
        }
      }
    }

    private fun <E, C : List<E>>RInterpreter.withAutoUpdate(property: RInterpreter.() -> C): C {
      return property().let { values ->
        if (values.isEmpty()) {
          updateState().blockingGet(DEFAULT_TIMEOUT)
          property()
        } else {
          values
        }
      }
    }

    private fun <R> Promise<R>.silentlyBlockingGet(timeout: Int): R? {
      val promise = AsyncPromise<R>()
      onSuccess { promise.setResult(it) }
      onError { promise.setResult(null) }
      return promise.blockingGet(timeout)
    }

    fun convertToInstallMultiListener(listener: Listener): MultiListener {
      return convertToMultiListener(listener, { it.first() }, { it.first() })
    }

    private fun convertToUninstallMultiListener(listener: Listener): MultiListener {
      return convertToMultiListener(listener, { if (it.size == 1) it.first() else null }, { it.first() })
    }

    private fun convertToMultiListener(
      listener: Listener,
      nameAggregator: (List<String>) -> String?,
      errorAggregator: (List<ErrorDescription?>) -> ErrorDescription?
    ): MultiListener {
      return object : MultiListener {
        override fun operationStarted(packageNames: List<String>) {
          listener.operationStarted(nameAggregator(packageNames))
        }

        override fun operationFinished(packageNames: List<String>, errorDescriptions: List<ErrorDescription?>) {
          listener.operationFinished(nameAggregator(packageNames), errorAggregator(errorDescriptions))
        }
      }
    }
  }
}
