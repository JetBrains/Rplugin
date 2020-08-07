// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.execution.ExecutionException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.webcore.packaging.RepoPackage
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.OperatingSystem
import org.jetbrains.r.interpreter.RInterpreterStateManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.packages.RPackageVersion
import org.jetbrains.r.rinterop.RInterop
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import javax.swing.text.MutableAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator

object RepoUtils {
  private val LOGGER = Logger.getInstance(RepoUtils::class.java)
  private val PACKAGE_DETAILS_KEY = Key.create<Pair<Map<String, RRepoPackage>, List<String>>>("org.jetbrains.r.packages.remote.packageDetails")

  private const val CRAN_URL = "https://cran.r-project.org/web/packages/available_packages_by_name.html"
  private const val REPO_URL_SUFFIX_SOURCE = "/src"
  private const val REPO_URL_SUFFIX_BINARY = "/bin"

  private const val AVAILABLE_PACKAGES_REFRESH_INTERVAL = 7 * 24 * 60 * 60 * 1000L // Update every week
  private const val PACKAGE_DESCRIPTIONS_REFRESH_INTERVAL = AVAILABLE_PACKAGES_REFRESH_INTERVAL

  val PACKAGE_SUMMARY = RPluginUtil.findFileInRHelpers("R/package_summary.R")
  val DECOMPILER_SCRIPT = RPluginUtil.findFileInRHelpers("R/extract_symbol.R")

  const val CRAN_URL_PLACEHOLDER = "@CRAN@"

  fun setPackageDetails(project: Project, repoPackages: List<RRepoPackage>, repoUrls: List<String>) {
    RAvailablePackageCache.getInstance(project).apply {
      values = repoPackages
      urls = repoUrls
    }
    setPackageDetailsWithoutCache(project, repoPackages, repoUrls)
  }

  private fun setPackageDetailsWithoutCache(project: Project, repoPackages: List<RRepoPackage>, repoUrls: List<String>) {
    val names2packages = repoPackages.asSequence().map { Pair(it.name, it) }.toMap()  // List is big enough
    project.putUserData(PACKAGE_DETAILS_KEY, Pair(names2packages, repoUrls))
  }

  fun getPackageDetails(project: Project): Map<String, RRepoPackage>? {
    return getCachedPackageDetails(project)?.first
  }

  fun getFreshPackageDetails(project: Project, expectedRepoUrls: List<String>): Map<String, RRepoPackage>? {
    return getCachedPackageDetails(project)?.let { details ->
      val actualRepoUrls = details.second
      if (actualRepoUrls == expectedRepoUrls) details.first else null
    }
  }

  private fun getCachedPackageDetails(project: Project): Pair<Map<String, RRepoPackage>, List<String>>? {
    return getSessionPackagesCache(project) ?: getProjectPackagesCache(project)?.let {
      setPackageDetailsWithoutCache(project, it.first, it.second)
      getSessionPackagesCache(project)
    }
  }

  private fun getSessionPackagesCache(project: Project): Pair<Map<String, RRepoPackage>, List<String>>? {
    return project.getUserData(PACKAGE_DETAILS_KEY)
  }

  private fun getProjectPackagesCache(project: Project): Pair<List<RRepoPackage>, List<String>>? {
    val cache = RAvailablePackageCache.getInstance(project)
    return getCachedValues(cache, AVAILABLE_PACKAGES_REFRESH_INTERVAL)?.let { packages ->
      Pair(packages, cache.urls)
    }
  }

  fun resetPackageDetails(project: Project) {
    RAvailablePackageCache.getInstance(project).values = listOf()
    project.putUserData(PACKAGE_DETAILS_KEY, null)
  }

  fun getPackageDescriptions(): Map<String, String> {
    fun createPackageDescriptions(): Map<String, String> {
      val names2descriptions = mutableMapOf<String, String>()
      val callback = object : HTMLEditorKit.ParserCallback() {
        private var isInTable: Boolean = false
        private var packageName: String? = null
        private lateinit var tag: HTML.Tag

        override fun handleStartTag(tag: HTML.Tag, set: MutableAttributeSet?, i: Int) {
          this.tag = tag
          if (tag.toString() == "table") {
            isInTable = true
          }
        }

        override fun handleText(data: CharArray, pos: Int) {
          if (isInTable) {
            val tagName = tag.toString()
            val name = packageName
            if (name == null && tagName == "a") {
              packageName = data.joinToString("")
            } else if (name != null && tagName == "td") {
              names2descriptions[name] = data.joinToString("")
              packageName = null
            }
          }
        }
      }

      try {
        val repositoryUrl = URL(CRAN_URL)
        val inputStream = repositoryUrl.openStream()
        InputStreamReader(inputStream).use { reader ->
          ParserDelegator().parse(reader, callback, true)
        }
      } catch (e: IOException) {
        LOGGER.warn("Couldn't get package details", e)
      }
      return names2descriptions
    }

    fun getApplicationCache(): Map<String, String>? {
      fun checkIsUpToDate(lastUpdate: Long): Boolean {
        return System.currentTimeMillis() - lastUpdate < PACKAGE_DESCRIPTIONS_REFRESH_INTERVAL
      }

      val cache = RPackageDescriptionCache.getInstance()
      val descriptions = cache.descriptions
      return if (checkIsUpToDate(cache.lastUpdate) && descriptions.isNotEmpty()) descriptions else null
    }

    fun setApplicationCache(descriptions: Map<String, String>) {
      RPackageDescriptionCache.getInstance().descriptions = descriptions
    }

    return getApplicationCache() ?: createPackageDescriptions().also {
      setApplicationCache(it)
    }
  }

  fun resetPackageDescriptions() {
    RPackageDescriptionCache.getInstance().descriptions = mapOf()
  }

  private fun <E>getCachedValues(cache: RCache<E>, refreshInterval: Long): List<E>? {
    val values = cache.values
    return if (System.currentTimeMillis() - cache.lastUpdate < refreshInterval && values.isNotEmpty()) values else null
  }

  private fun getInterop(suggested: RInterop?, project: Project): RInterop {
    return suggested ?: RInterpreterStateManager.getCurrentStateBlocking(project, RInterpreterUtil.DEFAULT_TIMEOUT)?.rInterop ?:
           throw ExecutionException("Cannot get rInterop for packaging task. Please, specify path to the R executable")
  }

  private fun getPackageVersion(packageName: String, rInterop: RInterop): String? {
    val versionOutput = rInterop.repoGetPackageVersion(packageName)
    return if (versionOutput.stderr.isBlank()) {  // Note: stderr won't be blank if package is missing
      versionOutput.stdout.let {
        if (it.isNotBlank()) it else throw RuntimeException("Cannot get any response from interpreter")
      }
    } else {
      null
    }
  }

  fun installPackage(interop: RInterop?, project: Project, repoPackage: RepoPackage, repoUrls: List<String>) {
    updatePackage(interop, project, repoPackage, repoUrls)
  }

  fun updatePackage(interop: RInterop?, project: Project, repoPackage: RepoPackage, repoUrls: List<String>) {
    val rInterop = getInterop(interop, project)
    if (repoUrls.isEmpty()) {
      throw ExecutionException("Unknown repo URL for package '${repoPackage.name}'")
    }
    val urls = repoUrls.map { trimRepoUrlSuffix(it) }

    // Ensure writable library path exists => interpreter won't ask during package installation
    val interpreter = rInterop.interpreter
    val state = rInterop.state
    val (libraryPath, isUserDirectoryCreated) =
      interpreter.getGuaranteedWritableLibraryPath(state.libraryPaths, state.userLibraryPath)

    val fallbackMethod = getFallbackDownloadMethod(interpreter.hostOS)
    val arguments = getInstallArguments(urls, libraryPath)
    rInterop.runWithPackageUnloaded(repoPackage.name) {
      repoInstallPackage(repoPackage.name, fallbackMethod, arguments)
    }

    if (isUserDirectoryCreated) {
      rInterop.repoAddLibraryPath(libraryPath)
      state.updateState().blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
      RLibraryWatcher.getInstance(project).updateRootsToWatch(state)
    }

    // It's rather hard to get installation status from 'updateOutput'
    // since it's messed up with log messages like "Installing into /username/.R/libs...".
    // Instead we can additionally check that package is actually installed
    // by requesting its version and comparing to the latest one.
    // One important implication of that approach is that installation will be considered successful
    // if networking is off but requested package is already up-to-date
    val version = getPackageVersion(repoPackage.name, rInterop)
    if (version != null) {
      if (RPackageVersion.isNewerOrSame(version, repoPackage.latestVersion)) {
        return
      } else {
        LOGGER.warn("updatePackage(): Expected version = ${repoPackage.latestVersion}, got = $version")
      }
    }
    throw ExecutionException("Cannot install package '${repoPackage.name}'. Check console for process output")
  }

  private fun RInterop.runWithPackageUnloaded(packageName: String, task: RInterop.() -> Unit) {
    if (isLibraryLoaded(packageName)) {
      unloadLibrary(packageName, false).blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
      task()
      loadLibrary(packageName).blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
    } else {
      task()
    }
  }

  private fun trimRepoUrlSuffix(repoUrl: String): String {
    val index = findRepoUrlSuffixIndex(repoUrl, listOf(REPO_URL_SUFFIX_SOURCE, REPO_URL_SUFFIX_BINARY))
    return if (index != null) repoUrl.substring(0, index) else repoUrl
  }

  private fun findRepoUrlSuffixIndex(repoUrl: String, suffices: List<String>): Int? {
    for (suffix in suffices) {
      val index = repoUrl.indexOf(suffix)
      if (index != -1) {
        return index
      }
    }
    return null
  }

  private fun getFallbackDownloadMethod(operatingSystem: OperatingSystem): String? {
    return when (operatingSystem) {
      OperatingSystem.LINUX -> "wget"
      OperatingSystem.MAC_OS -> "curl"
      else -> null
    }
  }

  private fun getInstallArguments(urls: List<String>, libraryPath: String): Map<String, String> {
    return mutableMapOf<String, String>().also {
      if (ApplicationManager.getApplication().isUnitTestMode) {
        it["type"] = "'source'"
      }
      it["repos"] = getInstallReposArgument(urls)
      //it["INSTALL_opts"] = "c('--no-lock')"  // TODO [mine]: uncomment this in case of "cannot unlock..." issues
      it["verbose"] = "FALSE"
      it["lib"] = "\"${StringUtil.escapeStringCharacters(libraryPath)}\""
    }
  }

  private fun getInstallReposArgument(urls: List<String>): String {
    return urls.joinToString(", ", "c(", ")") { url ->
      "\"${StringUtil.escapeStringCharacters(url)}\""
    }
  }

  @Throws(ExecutionException::class)
  fun uninstallPackage(interop: RInterop?, project: Project, repoPackage: RInstalledPackage) {
    val packageName = repoPackage.name
    val rInterop = getInterop(interop, project)
    if (!checkPackageInstalled(packageName, rInterop)) {
      throw ExecutionException("Cannot remove package '$packageName'. It is not installed")
    }
    val libraryPath = rInterop.state.getLibraryPathByName(packageName)
                      ?: throw ExecutionException("Cannot get library path for package '$packageName'")
    if (!libraryPath.isWritable) {
      throw ExecutionException("Cannot remove package '$packageName'. Library path is not writable")
    }
    if (rInterop.isLibraryLoaded(packageName)) {
      // Note: I guess there is no need to keep a dynamic library around since a package is deleted anyway
      rInterop.unloadLibrary(packageName, withDynamicLibrary = true).blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
    }
    rInterop.repoRemovePackage(packageName, libraryPath.path)
    val version = getPackageVersion(packageName, rInterop)
    if (version != null && RPackageVersion.isSame(version, repoPackage.version)) {
      throw ExecutionException("Cannot remove package '$packageName'. Check console for process output")
    }
  }

  private fun checkPackageInstalled(packageName: String, rInterop: RInterop): Boolean {
    val checkOutput = rInterop.repoCheckPackageInstalled(packageName)
    return checkOutput.stdout == "TRUE"
  }

  fun formatDetails(repoPackage: RRepoPackage?): String {
    fun String.makeBlock(header: String?): String {
      return """
        ${if (header != null) "<h3>$header</h3>" else ""}
        $this
        <br/>
      """
    }

    val begin = """
      <html>
        <body>
    """
    val builder = StringBuilder(begin)
    if (repoPackage != null) {
      val description = getPackageDescriptions()[repoPackage.name]
      builder.append(description?.makeBlock(null) ?: "")
      builder.append(repoPackage.latestVersion?.makeBlock("Version") ?: "")
      builder.append(repoPackage.depends?.makeBlock("Depends") ?: "")
    }
    val end = """
        </body>
      </html>
    """
    builder.append(end)
    return builder.toString()
  }
}
