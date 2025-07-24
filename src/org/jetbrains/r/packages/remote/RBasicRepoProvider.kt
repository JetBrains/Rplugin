/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.SuspendingLazy
import com.intellij.util.suspendingLazy
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.r.RPluginCoroutineScope
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.runHelper
import org.jetbrains.r.packages.RPackageService
import org.jetbrains.r.packages.remote.RepoUtils.CRAN_URL_PLACEHOLDER
import java.io.File

class RBasicRepoProvider(private val project: Project) : RepoProvider {
  private val service: RPackageService
    get() = RPackageService.getInstance(project)

  private suspend fun getEnabledRepositoryUrls(): List<String> {
    actualizeEnabledRepositories()
    return service.enabledRepositoryUrls.takeIf { it.isNotEmpty() } ?: listOf(CRAN_URL_PLACEHOLDER)
  }

  private val userRepositories: List<RUserRepository>
    get() = service.userRepositoryUrls.map { RUserRepository(it) }

  private var defaultRepositoriesDeferred: SuspendingLazy<List<RDefaultRepository>> =
    RPluginCoroutineScope.getScope(project).suspendingLazy {
      loadDefaultRepositories()
    }

  override var selectedCranMirrorIndex: Int
    get() = service.cranMirror
    set(index) {
      service.cranMirror = index
    }

  private val cranMirrors: SuspendingLazy<List<RMirror>> =
    RPluginCoroutineScope.getScope(project).suspendingLazy {
      loadMirrorsWithoutCaching()
    }

  override suspend fun getCranMirrors(): List<RMirror> = cranMirrors.getValue()

  override suspend fun getMappedEnabledRepositoryUrls(): List<String> {
    val cranMirrors = getCranMirrors()
    return if (cranMirrors.isNotEmpty()) {
      val enabledRepositoryUrls = getEnabledRepositoryUrls()
      mapRepositoryUrls(enabledRepositoryUrls, cranMirrors)
    } else {
      LOGGER.warn("Interpreter has returned an empty list of CRAN mirrors. Cannot map URLs of enabled repos")
      emptyList()
    }
  }

  override suspend fun getRepositorySelections(): List<RRepositoryWithSelection> {
    val defaultRepositories = defaultRepositoriesDeferred.getValue()
    val enabledRepositoriesUrls = getEnabledRepositoryUrls()
    val repositories = defaultRepositories + userRepositories
    return repositories.map { RRepositoryWithSelection(it, it.url in enabledRepositoriesUrls) }
  }

  override val name2AvailablePackages: Map<String, RRepoPackage>?
    get() = RepoUtils.getPackageDetails(project)

  override suspend fun getAllPackagesCached(): List<RRepoPackage> {
    val enabledRepositoryUrls = getEnabledRepositoryUrls()
    return RepoUtils.getFreshPackageDetails(project, enabledRepositoryUrls)?.values?.toList() ?: emptyList()
  }

  override suspend fun loadAllPackages(): List<RRepoPackage> {
    val mappedUrls = getMappedEnabledRepositoryUrls()
    return if (mappedUrls.isNotEmpty()) {
      val enabledRepositoryUrls = getEnabledRepositoryUrls()
      // Note: copy of repos URLs list is intentional.
      // Downloading of packages will take a long time so we must ensure that the list will stay unchanged
      val repoUrls = enabledRepositoryUrls.toList()
      withContext(Dispatchers.IO) {
        loadAllPackagesWithCaching(repoUrls, mappedUrls)
      }
    } else {
      LOGGER.warn("List of enabled repos is empty. No packages can be loaded")
      emptyList()
    }
  }

  override fun selectRepositories(repositorySelections: List<Pair<RRepository, Boolean>>) {
    val userUrls = repositorySelections.filter { it.first is RUserRepository }.map { it.first.url }
    val enabledUrls = repositorySelections.filter { it.second }.map { it.first.url }
    service.apply {
      enabledRepositoryUrls = enabledUrls.toMutableList()
      userRepositoryUrls = userUrls.toMutableList()
    }
  }

  override fun onInterpreterVersionChange() {
    RepoUtils.resetPackageDetails(project)
    service.enabledRepositoryUrls.clear()

    defaultRepositoriesDeferred = RPluginCoroutineScope.getScope(project).suspendingLazy { loadDefaultRepositories() }
  }

  private fun mapRepositoryUrls(repoUrls: List<String>, cranMirrors: List<RMirror>): List<String> {
    return repoUrls.map { url ->
      if (url == CRAN_URL_PLACEHOLDER) {
        if (selectedCranMirrorIndex !in cranMirrors.indices) {
          LOGGER.warn("Cannot get CRAN mirror with previously stored index = $selectedCranMirrorIndex. Fallback to the first mirror")
          selectedCranMirrorIndex = 0
        }
        cranMirrors[selectedCranMirrorIndex].url
      } else {
        url
      }
    }
  }

  private fun loadAllPackagesWithCaching(repoUrls: List<String>, mappedUrls: List<String>): List<RRepoPackage> {
    return loadAllPackagesWithoutCaching(mappedUrls).also { packages ->
      if (packages.isNotEmpty()) {
        RepoUtils.setPackageDetails(project, packages, repoUrls)
        RepoUtils.getPackageDescriptions()  // Force loading of package descriptions
      }
    }
  }

  private fun loadAllPackagesWithoutCaching(repoUrls: List<String>): List<RRepoPackage> {
    val lines = runHelper(AVAILABLE_PACKAGES_HELPER, repoUrls)
    return parsePackages(lines)
  }

  private fun parsePackages(lines: List<String>): List<RRepoPackage> {
    return lines.mapNotNull { line ->
      val items = line.split(GROUP_DELIMITER)
      if (items.size == 2) {
        val depends = items[1].let {
          if (it != "NA") it else null
        }
        val attributes = items[0].split(WORD_DELIMITER)
        RRepoPackage(attributes[0], attributes[1], attributes[2], depends)
      } else {
        null
      }
    }
  }

  private fun loadMirrorsWithoutCaching(): List<RMirror> {
    val lines = CRAN_MIRRORS_HELPER.readLines()
    return parseMirrors(lines)
  }

  private fun parseMirrors(lines: List<String>): List<RMirror> {
    return lines.mapNotNull { line ->
      val items = line.split(GROUP_DELIMITER).filter { it.isNotBlank() }
      if (items.count() >= 2) {
        val url = items[1].trim()
        val nameWords = items[0].split(WORD_DELIMITER).filter { it.isNotBlank() }.let { words ->
          if (words.count() >= 2 && words.last() == HTTPS_SUFFIX) {
            words.dropLast(1)
          } else {
            words
          }
        }
        val name = nameWords.joinToString(" ").trim()
        RMirror(name, url)
      } else {
        null
      }
    }
  }

  private suspend fun loadDefaultRepositories(): List<RDefaultRepository> {
    val mirrors = getCranMirrors()
    return withContext(Dispatchers.IO) {
      loadDefaultRepositoriesSync(mirrors)
    }
  }

  private fun loadDefaultRepositoriesSync(mirrors: List<RMirror>): List<RDefaultRepository> {
    val lines = runHelper(DEFAULT_REPOSITORIES_HELPER)
    if (lines.isEmpty()) {
      return emptyList()
    }
    val blankIndex = lines.indexOfFirst { it.isBlank() }
    if (blankIndex < 0) {
      LOGGER.error("Cannot find separator in helper's output:\n${lines.joinToString("\n")}")
      return emptyList()
    }
    val optional = lines.subList(0, blankIndex).filter { it.isNotBlank() }
    val mandatory = lines.subList(blankIndex + 1, lines.size).filter { it.isNotBlank() }
    return mergeRepositories(mandatory, optional, mirrors)
  }

  private fun mergeRepositories(mandatory: List<String>, optional: List<String>, mirrors: List<RMirror>): List<RDefaultRepository> {
    return mutableListOf<RDefaultRepository>().apply {
      // Note: obtained repositories can be mentioned among CRAN mirrors.
      // In this case they should be replaced with "@CRAN@"
      val mirrorUrls = mirrors.asSequence().map { it.url.trimSlash() }.toSet()
      val seen = mutableSetOf<String>()
      addAllNotSeen(mandatory, false, seen, mirrorUrls)
      addAllNotSeen(optional, true, seen, mirrorUrls)
    }
  }

  private fun MutableList<RDefaultRepository>.addAllNotSeen(
    urls: List<String>,
    isOptional: Boolean,
    seen: MutableSet<String>,
    mirrorUrls: Set<String>
  ) {
    for (url in urls) {
      val trimmedUrl = url.trimSlash()
      val mappedUrl = if (trimmedUrl in mirrorUrls) CRAN_URL_PLACEHOLDER else trimmedUrl
      if (mappedUrl !in seen) {
        add(RDefaultRepository(mappedUrl, isOptional))
        seen.add(mappedUrl)
      }
    }
  }

  private suspend fun actualizeEnabledRepositories(): Unit {
    val defaultRepositories = defaultRepositoriesDeferred.getValue()
    removeOutdatedEnabledRepositories(defaultRepositories)
    enableMandatoryRepositories(defaultRepositories)
  }

  private fun removeOutdatedEnabledRepositories(defaultRepositories: List<RDefaultRepository>) {
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

  private fun enableMandatoryRepositories(defaultRepositories: List<RDefaultRepository>) {
    val mandatory = defaultRepositories.filter { !it.isOptional }
    for (repository in mandatory) {
      if (!service.enabledRepositoryUrls.contains(repository.url)) {
        service.enabledRepositoryUrls.add(repository.url)
      }
    }
  }

  private fun runHelper(helper: File, args: List<String> = emptyList()): List<String> {
    val interpreter = RInterpreterManager.getInterpreterBlocking(project, RInterpreterUtil.DEFAULT_TIMEOUT) ?: return emptyList()
    return interpreter.runHelper(helper, args).lines()
  }

  companion object {
    private const val WORD_DELIMITER = " "
    private const val GROUP_DELIMITER = "\t"
    private const val HTTPS_SUFFIX = "[https]"

    private val LOGGER = Logger.getInstance(RBasicRepoProvider::class.java)
    private val CRAN_MIRRORS_HELPER by lazy { RPluginUtil.findFileInRHelpers("R/repos/local_cran_mirrors.txt") }
    private val AVAILABLE_PACKAGES_HELPER by lazy { RPluginUtil.findFileInRHelpers("R/interpreter/available_packages.R") }
    private val DEFAULT_REPOSITORIES_HELPER by lazy { RPluginUtil.findFileInRHelpers("R/interpreter/default_repositories.R") }

    private fun String.trimSlash(): String {
      return if (endsWith("/")) dropLast(1) else this
    }
  }
}
