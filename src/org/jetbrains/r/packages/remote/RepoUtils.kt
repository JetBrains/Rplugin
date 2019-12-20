// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.webcore.packaging.InstalledPackage
import com.intellij.webcore.packaging.RepoPackage
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.RLibraryWatcher
import org.jetbrains.r.packages.RHelpersUtil
import org.jetbrains.r.rinterop.RInterop
import java.io.File
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
  private const val CRAN_MIRRORS_REFRESH_INTERVAL = AVAILABLE_PACKAGES_REFRESH_INTERVAL

  val PACKAGE_SUMMARY = RHelpersUtil.findFileInRHelpers("R/package_summary.R")
  val DECOMPILER_SCRIPT = RHelpersUtil.findFileInRHelpers("R/extract_symbol.R")

  const val CRAN_URL_PLACEHOLDER = "@CRAN@"

  var cachedMirrors: List<RMirror>?
    get() {
      return getCachedValues(RMirrorCache.getInstance(), CRAN_MIRRORS_REFRESH_INTERVAL)
    }
    set(newMirrors) {
      RMirrorCache.getInstance().values = newMirrors ?: emptyList()
    }

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

  private fun getInterpreter(suggested: RInterpreter?, project: Project): RInterpreter {
    return suggested ?: RInterpreterManager.getInstance(project).interpreter ?:
           throw ExecutionException("Cannot get interpreter for packaging task. Please, specify path to the R executable")
  }

  private fun getConsoleForCurrentInterpreter(interpreter: RInterpreter, project: Project): RConsoleView {
    val current = RConsoleManager.getInstance(project).currentConsoleOrNull
    return if (current != null && current.interpreterPath == interpreter.interpreterPath) {
      current
    } else {
      RConsoleManager.runConsole(project)
        .onError { LOGGER.error("Cannot run new console for packaging task", it) }
        .blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT) ?: throw RuntimeException("Cannot run new console")
    }
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

  fun installPackage(rInterpreter: RInterpreter?, project: Project, repoPackage: RepoPackage) {
    updatePackage(rInterpreter, project, repoPackage)
  }

  fun updatePackage(rInterpreter: RInterpreter?, project: Project, repoPackage: RepoPackage) {
    val interpreter = getInterpreter(rInterpreter, project)
    val rInterop = getConsoleForCurrentInterpreter(interpreter, project).rInterop
    val repoUrl = repoPackage.repoUrl ?: throw ExecutionException("Unknown repo URL for package '${repoPackage.name}'")
    val url = trimRepoUrlSuffix(repoUrl)

    // Ensure writable library path exists => interpreter won't ask during package installation
    val (libraryPath, isUserDirectoryCreated) = getGuaranteedWritableLibraryPath(interpreter)

    val arguments = getInstallArguments(url, libraryPath)
    rInterop.repoInstallPackage(repoPackage.name, arguments)

    if (isUserDirectoryCreated) {
      rInterop.repoAddLibraryPath(libraryPath)
      interpreter.updateState().blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)
      RLibraryWatcher.getInstance(project).registerRootsToWatch(interpreter.libraryPaths)
    }

    // It's rather hard to get installation status from 'updateOutput'
    // since it's messed up with log messages like "Installing into /username/.R/libs...".
    // Instead we can additionally check that package is actually installed
    // by requesting its version and comparing to the latest one.
    // One important implication of that approach is that installation will be considered successful
    // if networking is off but requested package is already up-to-date
    val version = getPackageVersion(repoPackage.name, rInterop)
    if (version != null) {
      if (areSameVersions(version, repoPackage.latestVersion)) {
        return
      } else {
        LOGGER.warn("updatePackage(): Expected version = ${repoPackage.latestVersion}, got = $version")
      }
    }
    throw ExecutionException("Cannot install package '${repoPackage.name}'. Check console for process output")
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

  // Note: returns pair of writable path and indicator whether new library path was created
  private fun getGuaranteedWritableLibraryPath(interpreter: RInterpreter): Pair<String, Boolean> {
    val writable = findWritableLibraryPath(interpreter)
    return if (writable != null) {
      Pair(writable, false)
    } else {
      tryCreateUserLibraryPath(interpreter)
    }
  }

  private fun findWritableLibraryPath(interpreter: RInterpreter): String? {
    return interpreter.libraryPaths.find { it.isWritable }?.path?.let { path ->
      FileUtil.toSystemIndependentName(path)
    }
  }

  private fun tryCreateUserLibraryPath(interpreter: RInterpreter): Pair<String, Boolean> {
    val path = FileUtil.toSystemIndependentName(interpreter.userLibraryPath)
    return Pair(path, File(path).mkdirs())
  }

  private fun getInstallArguments(url: String, libraryPath: String): Map<String, String> {
    return mutableMapOf<String, String>().also {
      it["repos"] = "'$url'"
      it["dependencies"] = "TRUE"
      //it["INSTALL_opts"] = "c('--no-lock')"  // TODO [mine]: uncomment this in case of "cannot unlock..." issues
      it["verbose"] = "FALSE"
      it["lib"] = "'$libraryPath'"
    }
  }

  private fun areSameVersions(aVersion: String?, bVersion: String?): Boolean {
    return aVersion?.unifyVersion() == bVersion?.unifyVersion()
  }

  private fun String.unifyVersion(): String {
    return replace('-', '.')
  }

  @Throws(ExecutionException::class)
  fun uninstallPackage(rInterpreter: RInterpreter?, project: Project, repoPackage: InstalledPackage) {
    val packageName = repoPackage.name
    val interpreter = getInterpreter(rInterpreter, project)
    val rInterop = getConsoleForCurrentInterpreter(interpreter, project).rInterop
    if (!checkPackageInstalled(packageName, rInterop)) {
      throw ExecutionException("Cannot remove package '$packageName'. It is not installed")
    }
    val libraryPath = getPackageLibraryPath(packageName, interpreter)
    if (!libraryPath.isWritable) {
      throw ExecutionException("Cannot remove package '$packageName'. Library path is not writable")
    }
    rInterop.repoRemovePackage(packageName, FileUtil.toSystemIndependentName(libraryPath.path))
    val version = getPackageVersion(packageName, rInterop)
    if (version != null && areSameVersions(version, repoPackage.version)) {
      throw ExecutionException("Cannot remove package '$packageName'. Check console for process output")
    }
  }

  private fun checkPackageInstalled(packageName: String, rInterop: RInterop): Boolean {
    val checkOutput = rInterop.repoCheckPackageInstalled(packageName)
    return checkOutput.stdout == "TRUE"
  }

  private fun getPackageLibraryPath(packageName: String, interpreter: RInterpreter): VirtualFile {
    return interpreter.getLibraryPathByName(packageName) ?: throw ExecutionException("Cannot get library path for package '$packageName'")
  }

  fun formatDetails(project: Project, packageName: String): String {
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
    getPackageDetails(project)?.get(packageName)?.let {
      val description = getPackageDescriptions()[packageName]
      builder.append(description?.makeBlock(null) ?: "")
      builder.append(it.latestVersion?.makeBlock("Version") ?: "")
      builder.append(it.depends?.makeBlock("Depends") ?: "")

      // TODO [mine]: URL string causes dialog to show a blank description page for Bioconductor packages on MacOS
      //builder.append(it.repoUrl?.makeBlock("Repository") ?: "")
    }
    val end = """
        </body>
      </html>
    """
    builder.append(end)
    return builder.toString()
  }
}
