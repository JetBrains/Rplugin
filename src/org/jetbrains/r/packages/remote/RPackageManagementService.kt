// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.CatchingConsumer
import com.intellij.webcore.packaging.InstalledPackage
import com.intellij.webcore.packaging.PackageManagementService
import com.intellij.webcore.packaging.RepoPackage
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.documentation.SHOW_PACKAGE_DOCS
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil.DEFAULT_TIMEOUT
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.RPackageService
import org.jetbrains.r.packages.remote.RepoUtils.CRAN_URL_PLACEHOLDER
import org.jetbrains.r.packages.remote.ui.RPackageServiceListener
import org.jetbrains.r.psi.RElementFactory
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

  private val interpreter: RInterpreter
    get() {
      fun getInitializedManager(): RInterpreterManager {
        return interpreterManager.apply {
          if (!hasInterpreter()) {
            initializeInterpreter()
              .onError { LOGGER.error("Unable to initialize interpreter", it) }
              .blockingGet(DEFAULT_TIMEOUT)
          }
        }
      }

      return getInitializedManager().interpreter!!
    }

  private val service: RPackageService
    get() = RPackageService.getInstance(project)

  private val numScheduledOperations = AtomicInteger(0)

  @Volatile
  private var lastInstalledPackages: ExpiringList<InstalledPackage>? = null

  val defaultRepositories: List<RDefaultRepository>
    get() = interpreter.defaultRepositories

  val userRepositories: List<RUserRepository>
    get() = service.userRepositoryUrls.map { RUserRepository(it) }

  val enabledRepositoryUrls: List<String>
    get() {
      actualizeEnabledRepositories()
      return service.enabledRepositoryUrls.let { if (it.isNotEmpty()) it else listOf(CRAN_URL_PLACEHOLDER) }
    }

  val mirrors: List<RMirror>
    get() = interpreter.cranMirrors

  var cranMirror: Int
    get() = service.cranMirror
    set(index) {
      service.cranMirror = index
    }

  override fun getAllRepositories(): List<String> {
    return mutableListOf<String>().also {
      it.addAll(defaultRepositories.map { r -> r.url })
      it.addAll(service.userRepositoryUrls)
    }
  }

  fun setRepositories(repositorySelections: List<Pair<RRepository, Boolean>>) {
    val userUrls = mutableListOf<String>()
    val enabledUrls = mutableListOf<String>()
    for ((repository, isSelected) in repositorySelections) {
      if (isSelected) {
        enabledUrls.add(repository.url)
      }
      if (repository is RUserRepository) {
        userUrls.add(repository.url)
      }
    }
    service.apply {
      enabledRepositoryUrls.apply {
        clear()
        addAll(enabledUrls)
      }
      userRepositoryUrls.apply {
        clear()
        addAll(userUrls)
      }
    }
  }

  @Deprecated("")
  override fun getAllPackages(): List<RepoPackage> {
    val cached = allPackagesCached
    return if (cached.isNotEmpty()) cached else reloadAllPackages()
  }

  override fun getAllPackagesCached(): List<RepoPackage> {
    return RepoUtils.getFreshPackageDetails(project, enabledRepositoryUrls)?.values?.toList() ?: listOf()
  }

  override fun reloadAllPackages(): List<RepoPackage> {
    return loadAllPackages()?.let {
      RepoUtils.getPackageDescriptions()  // Force loading of package descriptions
      RepoUtils.setPackageDetails(project, it.first, it.second)
      it.first
    } ?: listOf()
  }

  private fun loadAllPackages(): Pair<List<RRepoPackage>, List<String>>? {
    val cranMirrors = interpreter.withAutoUpdate { cranMirrors }
    return if (cranMirrors.isNotEmpty()) {
      // Note: copy of repos URLs list is intentional.
      // Downloading of packages will take a long time so we must ensure that the list will stay unchanged
      val repoUrls = enabledRepositoryUrls.toList()
      val mappedUrls = mapRepositoryUrls(repoUrls, cranMirrors)
      interpreter.getAvailablePackages(mappedUrls).blockingGet(DEFAULT_TIMEOUT)?.let { packages ->
        Pair(packages, repoUrls)
      }
    } else {
      LOGGER.warn("Interpreter has returned an empty list of CRAN mirrors. No packages can be loaded")
      null
    }
  }

  private fun mapRepositoryUrls(repoUrls: List<String>, cranMirrors: List<RMirror>): List<String> {
    return repoUrls.map { url ->
      if (url == CRAN_URL_PLACEHOLDER) {
        if (cranMirror !in cranMirrors.indices) {
          LOGGER.warn("Cannot get CRAN mirror with previously stored index = $cranMirror. Fallback to the first mirror")
          cranMirror = 0
        }
        cranMirrors[cranMirror].url
      } else {
        url
      }
    }
  }

  override fun getInstalledPackages(): List<InstalledPackage> {
    return lastInstalledPackages?.takeIf { it.hasNotExpired } ?: loadInstalledPackages().also { installed ->
      lastInstalledPackages = installed
    }
  }

  private fun loadInstalledPackages(): ExpiringList<InstalledPackage> {
    val installed = interpreter.withAutoUpdate { installedPackages }
    return installed.filter { it.isUser }.map { it.toInstalledPackage() }
  }

  fun findInstalledPackageByName(name: String): InstalledPackage? {
    return interpreter.getPackageByName(name)?.toInstalledPackage()
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
      val names2packages = RepoUtils.getPackageDetails(project) ?: throw MissingPackageDetailsException("Package mapping is not set")
      val filled = names2packages[repoPackage.name]
      filled ?: throw UnresolvedPackageDetailsException("Can't get details for package '" + repoPackage.name + "'")
    } else {
      repoPackage
    }
  }

  override fun canInstallToUser(): Boolean {
    return false
  }

  fun canUninstallPackage(installedPackage: InstalledPackage): Boolean {
    return interpreter.getLibraryPathByName(installedPackage.name)?.isWritable ?: false
  }

  override fun uninstallPackages(installedPackages: List<InstalledPackage>, listener: Listener) {
    val packageNames = installedPackages.map { it.name }
    val multiListener = convertToUninstallMultiListener(listener)
    val manager = RPackageTaskManager(interpreter, project, getTaskListener(packageNames, multiListener))
    onOperationStart()
    manager.uninstall(installedPackages)
  }

  override fun fetchPackageVersions(s: String, consumer: CatchingConsumer<List<String>, Exception>) {
    consumer.consume(listOf())
  }

  override fun fetchPackageDetails(packageName: String, consumer: CatchingConsumer<String, Exception>) {
    consumer.consume(RepoUtils.formatDetails(project, packageName))
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

  private fun actualizeEnabledRepositories() {
    removeOutdatedEnabledRepositories()
    enableMandatoryRepositories()
  }

  private fun removeOutdatedEnabledRepositories() {
    val enabled = service.enabledRepositoryUrls
    val filtered = enabled.filter { url ->
      defaultRepositories.find { it.url == url } != null || service.userRepositoryUrls.contains(url)
    }
    if (filtered.size != enabled.size) {
      service.enabledRepositoryUrls.apply {
        clear()
        addAll(filtered)
      }
    }
  }

  private fun enableMandatoryRepositories() {
    val mandatory = defaultRepositories.filter { !it.isOptional }
    for (repository in mandatory) {
      if (!service.enabledRepositoryUrls.contains(repository.url)) {
        service.enabledRepositoryUrls.add(repository.url)
      }
    }
  }

  fun navigateToPackageDocumentation(pkg: InstalledPackage) {
    val element = RElementFactory.buildRFileFromText(project, "$SHOW_PACKAGE_DOCS(${pkg.name})").firstChild
    DocumentationManager.getInstance(project).showJavaDocInfoAtToolWindow(element, element.originalElement)
  }

  interface MultiListener {
    fun operationStarted(packageNames: List<String>)
    fun operationFinished(packageNames: List<String>, errorDescriptions: List<ErrorDescription?>)
  }

  companion object {
    private val LOGGER = Logger.getInstance(RPackageManagementService::class.java)
    private const val ARGUMENT_DELIMITER = " "
    private const val DEPENDS_DELIMITER = "\t"

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

    fun RPackage.toInstalledPackage() = InstalledPackage(packageName, packageVersion)

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
