/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import com.intellij.openapi.application.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.packages.remote.RPackageManagementService
import java.util.concurrent.atomic.AtomicReference

data class DependencyVersionBound(
  val version: String,
  val strict: Boolean = false
)

data class DependencyPackage(
  val name: String,
  val lowerBound: DependencyVersionBound? = null,
  val upperBound: DependencyVersionBound? = null
) {
  fun versionSatisfies(version: String): Boolean {
    return lowerBoundSatisfies(version) && upperBoundSatisfies(version)
  }

  fun lowerBoundSatisfies(version: String): Boolean {
    if (lowerBound == null) return true
    val compareResult = RPackageVersion.compare(version, lowerBound.version) ?: return true
    return compareResult > 0 || !lowerBound.strict && compareResult == 0
  }

  fun upperBoundSatisfies(version: String): Boolean {
    if (upperBound == null) return true
    val compareResult = RPackageVersion.compare(version, upperBound.version) ?: return true
    return compareResult < 0 || !upperBound.strict && compareResult == 0
  }
}

data class PackageDescriptionInfo(
  val packageName: String,
  val version: String,
  val title: String,
  val depends: List<DependencyPackage>,
  val imports: List<DependencyPackage>,
  val suggests: List<DependencyPackage>
)

class RPackageProjectManager(private val project: Project) {

  private val rConsoleManager = RConsoleManager.getInstance(project)
  private val requiredPackageInstaller = RequiredPackageInstaller.getInstance(project)
  private val lastReceivedInfo = AtomicReference<PackageDescriptionInfo?>(null)
  private val lastConsoles = AtomicReference<List<RConsoleView>>(emptyList())

  /**
   * @return *null* if no DESCRIPTION file or is it written **pretty** bad (mandatory fields are omitted,
   * gross syntax errors, etc.)
   */
  fun getProjectPackageDescriptionInfo(): PackageDescriptionInfo? {
    return runReadAction {
      CachedValuesManager.getManager(project).getCachedValue(project) {
        val virtualFile = project.guessProjectDir()?.findChild("DESCRIPTION")
        val file = virtualFile?.let { PsiUtilCore.findFileSystemItem(project, it) } as? PsiFile
        val nullPackageDescriptionInfo: PackageDescriptionInfo? = null
        if (file == null) {
          // If no DESCRIPTION file, try again when VFS changed
          CachedValueProvider.Result.create(nullPackageDescriptionInfo, VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS)
        }
        else {
          // If DESCRIPTION file exists, try again when will file change
          CachedValueProvider.Result.create(parsePackageDescriptionInfo(file), file)
        }
      }
    }
  }

  fun loadOrSuggestToInstallMissedPackages(packageInfo: PackageDescriptionInfo? = getProjectPackageDescriptionInfo()) = invokeLater {
    if (packageInfo == null) return@invokeLater
    val consoles =
      if (ApplicationManager.getApplication().isUnitTestMode) {
        rConsoleManager.currentConsoleOrNull?.let { listOf(it) } ?: emptyList()
      }
      else rConsoleManager.consoles.toMutableList()

    val lastValue = lastReceivedInfo.get()
    // If no info or info didn't change or someone already refreshing dependencies - finish
    if (lastValue == packageInfo) {
      val lastConsolesValue = lastConsoles.get()
      if (lastConsolesValue != consoles && lastConsoles.compareAndSet(lastConsolesValue, consoles)) {
        loadMissingPackagesToConsoles(consoles - lastConsolesValue, packageInfo)
      }
      return@invokeLater
    }
    if (!lastReceivedInfo.compareAndSet(lastValue, packageInfo)) return@invokeLater

    val missingPackages = loadMissingPackagesToConsoles(consoles, packageInfo).mapNotNull {
      if (it.name == "R") null
      else RequiredPackage(it.name, it.lowerBound?.version ?: "", it.lowerBound?.strict ?: false)
    }
    if (ApplicationManager.getApplication().isUnitTestMode) return@invokeLater
    if (missingPackages.isNotEmpty()) {
      /* Try to install missing package requirements.
         Doesn't handle case when the installed version is higher than the requested version.
         Not supported, since this is a very rare case and is not trivial to implement. */
      requiredPackageInstaller
        .installPackagesWithUserPermission("Package '${packageInfo.packageName}' DESCRIPTION", missingPackages)
        .onSuccess {
          missingPackages.forEach { rPackage -> consoles.forEach { it.consoleRuntimeInfo.loadPackage(rPackage.name) } }
        }
    }
  }

  private fun loadMissingPackagesToConsoles(consoles: List<RConsoleView>, packageInfo: PackageDescriptionInfo): List<DependencyPackage> {
    if (consoles.isEmpty()) return emptyList()
    val installedPackages = consoles.first().interpreter.installedPackages.map { it.name to it.packageVersion }.toMap()
    val allPackagesToLoad = packageInfo.depends + packageInfo.imports + packageInfo.suggests
    val possibleToLoad = allPackagesToLoad.filter { rPackage ->
      val name = rPackage.name
      if (name == "R") false
      else name in installedPackages.keys && rPackage.lowerBoundSatisfies(installedPackages.getValue(name))
    }
    consoles.forEach { console ->
      val loadedPackages = console.rInterop.loadedPackages.value.keys
      possibleToLoad.map { it.name }.filter { it !in loadedPackages }.forEach { console.consoleRuntimeInfo.loadPackage(it) }
    }
    return allPackagesToLoad - possibleToLoad
  }

  private fun parsePackageDescriptionInfo(descriptionFile: PsiFile): PackageDescriptionInfo? {
    val lines = descriptionFile.text.split(System.lineSeparator(), "\n").filter { it.isNotBlank() }
    val fields = mutableMapOf<String, StringBuilder>()
    var lastTag = ""
    for (line in lines) {
      if (line.isEmpty()) continue
      if (Regex("^\\s").containsMatchIn(line)) {
        // multiline value
        if (lastTag.isEmpty()) return null // tag missed
        line.trim().let {
          if (it != ".") fields.getOrPut(lastTag) { StringBuilder() }.append(it) // dot == empty line
        }
      }
      else {
        val (tag, initValue) = Regex("(.+?):(.*)")
          .matchEntire(line)?.groupValues?.let { it[1] to it[2] }
          ?: return null // Regular line must have a tag
        lastTag = tag
        fields[lastTag] = StringBuilder(initValue.trim())
      }
    }

    val packageDescriptionInfo = fields.analyse {
      val packageName = analyseField("Package") { it.trim() } ?: return null
      val packageVersion = analyseField("Version") { it.trim() } ?: return null
      val title = analyseField("Title") { it.trim() } ?: return null
      val depends = analyseFieldOrDefault("Depends", emptyList(), ::parsePackageList)
      val imports = analyseFieldOrDefault("Imports", emptyList(), ::parsePackageList)
      val suggests = analyseFieldOrDefault("Suggests", emptyList(), ::parsePackageList)
      PackageDescriptionInfo(packageName, packageVersion, title, depends, imports, suggests)
    }
    loadOrSuggestToInstallMissedPackages(packageDescriptionInfo)
    return packageDescriptionInfo
  }

  companion object {
    private inline fun Map<String, StringBuilder>.analyse(analyser: PackageDescriptionInfoAnalyser.() -> PackageDescriptionInfo?)
      : PackageDescriptionInfo? {
      return PackageDescriptionInfoAnalyser(this.mapValues { it.value.toString() }).run { analyser() }
    }

    private class PackageDescriptionInfoAnalyser(private val fields: Map<String, String>) {

      fun <T> analyseField(fieldName: String, analyser: (String) -> T): T? {
        return analyseFieldOrDefault(fieldName, null, analyser)
      }

      fun <T> analyseFieldOrDefault(fieldName: String, defaultValue: T, analyser: (String) -> T): T {
        return fields[fieldName]?.let { analyser(it) } ?: defaultValue
      }
    }

    private fun parsePackageList(unparsedPackageList: String): List<DependencyPackage> {
      data class SplitPackageInfo(val name: String, val comparisonOp: String, val version: String)

      fun foldBounds(bounds: List<SplitPackageInfo>, isLower: Boolean): DependencyVersionBound? {
        val initialVersion = if (isLower) "" else "z"
        val opList = if (isLower) listOf(">", ">=", "==") else listOf("<", "<=", "==")
        val strictOp = if (isLower) ">" else "<"
        val compareResult = { a: String, b: String -> if (isLower) a < b else a > b }
        return bounds.filter { it.comparisonOp in opList }.fold(DependencyVersionBound(initialVersion)) { acc, info ->
          val infoStrict = info.comparisonOp == strictOp
          if (compareResult(acc.version, info.version) || (acc.version == info.version && acc.strict < infoStrict)) {
            DependencyVersionBound(info.version, infoStrict)
          }
          else acc
        }.let { if (it.version == initialVersion) null else it }
      }
      return unparsedPackageList
        .split(',')
        .mapNotNull { packageWithVersion ->
          val groups = PACKAGE_DEPENDENCY_REGEX.matchEntire(packageWithVersion)?.groupValues ?: return@mapNotNull null
          val name = groups.getOrNull(1)?.trim() ?: return@mapNotNull null
          val comparisonOp = groups.getOrNull(3)?.trim() ?: ""
          val version = groups.getOrNull(4)?.trim() ?: ""
          SplitPackageInfo(name, comparisonOp, version)
        }
        .groupBy { it.name }
        .map { (name, bounds) ->
          val lowerBound = foldBounds(bounds, true)
          val upperBound = foldBounds(bounds, false)
          DependencyPackage(name, lowerBound, upperBound)
        }
    }

    private val PACKAGE_DEPENDENCY_REGEX = Regex("(.+?)( \\((>=|<=|>|<|==) (.+?)\\))?") // E.g. R (>= 3.6)

    fun getInstance(project: Project): RPackageProjectManager {
      return ServiceManager.getService(project, RPackageProjectManager::class.java)
    }
  }
}