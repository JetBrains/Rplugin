/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.google.common.collect.Lists
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.RInterpreterUtil.DEFAULT_TIMEOUT
import org.jetbrains.r.interpreter.RInterpreterUtil.EDT_TIMEOUT
import org.jetbrains.r.packages.RHelpersUtil
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.RPackagePriority
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.packages.remote.RDefaultRepository
import org.jetbrains.r.packages.remote.RMirror
import org.jetbrains.r.packages.remote.RRepoPackage
import java.io.File
import java.nio.file.Paths
import java.util.*

class RInterpreterImpl(private val versionInfo: Map<String, String>,
                       override val interpreterPath: String,
                       private val project: Project) : RInterpreter {
  @Volatile
  private var state = State.EMPTY

  override val version: Version = buildVersion(versionInfo)
  override val interpreterName: String get() = versionInfo["version.string"]?.replace(' ', '_')  ?: "unnamed"
  override val installedPackages get() = state.installedPackages
  override val libraryPaths get() = state.libraryPaths
  override val userLibraryPath get() = state.userLibraryPath
  override val cranMirrors get() = state.cranMirrors
  override val defaultRepositories get() = state.defaultRepositories

  private val name2PsiFile = ContainerUtil.createConcurrentSoftKeySoftValueMap<String, PsiFile?>()

  @Volatile
  override var skeletonRoots: Set<VirtualFile> = emptySet()
    get() {
      if (field.isEmpty()) {
        field = skeletonPaths.mapNotNull { path -> VfsUtil.findFile(Paths.get(path), false) }.toSet()
      }
      return field
    }
    private set

  override fun getAvailablePackages(repoUrls: List<String>): Promise<List<RRepoPackage>> {
    return runAsync {
      val lines = forceRunHelper(AVAILABLE_PACKAGES_HELPER, repoUrls)
      lines.mapNotNull { line ->
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
  }

  override fun getPackageByName(name: String) = state.name2installedPackages[name]

  override fun getProcessOutput(scriptText: String) = runScript(scriptText, interpreterPath, project.basePath!!)

  override fun runHelperWithArgs(helper: File, vararg args: String): ProcessOutput {
    val command = Lists.newArrayList<String>(
      interpreterPath,
      "--slave",
      "-f", helper.getAbsolutePath(),
      "--args")

    Collections.addAll(command, *args)

    val processHandler = CapturingProcessHandler(GeneralCommandLine(command).withWorkDirectory(project.basePath!!))
    val output = processHandler.runProcess(DEFAULT_TIMEOUT)

    if (output.exitCode != 0) {
      LOG.warn("Failed to run script. Exit code: " + output.exitCode)
      LOG.warn(output.stderr)
    }

    return output
  }

  override fun getSkeletonFileByPackageName(name: String): PsiFile? {
    val cached = name2PsiFile[name]
    if (cached != null && cached.isValid) {
      return cached
    }
    val skeletonFileName = getPackageByName(name)?.getLibraryBinFileName() ?: return null
    skeletonRoots.forEach { skeletonRoot ->
      skeletonRoot.findChild(skeletonFileName)?.let { virtualFile ->
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
        name2PsiFile[name] = psiFile
        return psiFile
      }
    }
    return null
  }

  private fun getHelpersRoot(): File {
    @NonNls val jarPath = PathUtil.getJarPathForClass(RHelpersUtil::class.java)
    if (jarPath.endsWith(".jar")) {
      val jarFile = File(jarPath)

      LOG.assertTrue(jarFile.exists(), "jar file cannot be null")
      return jarFile.parentFile.parentFile
    }

    return File(jarPath)
  }

  override fun updateState() {
    val cachedMirrors = state.cranMirrors
    state = State.EMPTY
    name2PsiFile.clear()
    val installedPackages = loadInstalledPackages()
    val name2installedPackages = installedPackages.map { it.packageName to it }.toMap()
    val mirrors = if (cachedMirrors.isNotEmpty()) cachedMirrors else getMirrors()
    state = State(loadLibraryPaths(), installedPackages, name2installedPackages, getUserPath(), mirrors, getRepositories())
  }

  private fun getUserPath(): String {
    val lines = forceRunHelper(GET_ENV_HELPER, listOf("R_LIBS_USER"))
    val firstLine = lines[0]
    if (firstLine.isNotBlank()) {
      return firstLine.expandTilde()
    } else {
      throw RuntimeException("Cannot get user library path")
    }
  }

  private fun getMirrors(): List<RMirror> {
    fun parseMirrors(lines: List<String>): List<RMirror> {
      return lines
        .mapNotNull { line ->
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

    val lines = forceRunHelper(CRAN_MIRRORS_HELPER, listOf())
    return parseMirrors(lines)
  }

  private fun getRepositories(): List<RDefaultRepository> {
    val lines = forceRunHelper(DEFAULT_REPOSITORIES_HELPER, listOf())
    return parseStrings(lines).map { RDefaultRepository(it) }
  }

  private fun loadLibraryPaths(): List<VirtualFile> {
    val lines = forceRunHelper(LIBRARY_PATHS_HELPER, listOf())
    val paths = parseStrings(lines)
    return paths.mapNotNull { VfsUtil.findFileByIoFile(File(it), true) }.toList()
  }

  private fun loadInstalledPackages(): List<RPackage> {
    val lines = forceRunHelper(INSTALLED_PACKAGES_HELPER, listOf())
    return if (lines.isNotEmpty()) {
      val obtained = lines.asSequence()
        .filter { it.isNotBlank() }
        .map {
          val splitLine = it.split("\t")
          try {
            val libraryPath = splitLine[0].trim()
            val packageName = splitLine[1].trim()
            val version = splitLine[2].trim()
            val priority = splitLine[3].trim().let { token ->
              when (token) {
                "base" -> RPackagePriority.BASE
                "recommended" -> RPackagePriority.RECOMMENDED
                "NA" -> RPackagePriority.NA
                else -> {
                  LOG.warn("Unsupported priority: $token")
                  RPackagePriority.NA
                }
              }
            }
            RPackage(packageName, version, priority, libraryPath)
          }
          catch (e: Throwable) {
            throw RuntimeException("failed to split package-version in line '$it'", e)
          }
        }

      // Obtained sequence contains duplicates of the same packages but for different versions.
      // The most recent ones go first.
      // Also it's not sorted by package names
      val names2packages = TreeMap<String, RPackage>(String.CASE_INSENSITIVE_ORDER)
      for (rPackage in obtained) {
        names2packages.getOrPut(rPackage.packageName, { rPackage })
      }
      names2packages.values.toList()
    } else {
      listOf()
    }
  }

  private fun forceRunHelper(helper: File, args: List<String>): List<String> {
    val scriptName = helper.name
    val time = System.currentTimeMillis()
    try {
      val result = runAsync { runHelperWithArgs(helper, *args.toTypedArray()) }
                     .onError { LOG.error(it) }
                     .blockingGet(DEFAULT_TIMEOUT) ?: throw RuntimeException("Timeout for helper '$scriptName'")
      if (result.exitCode != 0) {
        throw RuntimeException("Helper '$scriptName' has non-zero exit code: ${result.exitCode}")
      }
      if (result.stderr.isNotBlank()) {
        throw RuntimeException("Failed to run helper '$scriptName':\n${result.stderr}")
      }
      if (result.stdout.isBlank()) {
        throw RuntimeException("Cannot get any output from helper '$scriptName'")
      }
      return result.stdout.lines()
    } finally {
      LOG.warn("Running ${scriptName} took ${System.currentTimeMillis() - time}ms")
    }
  }

  private fun buildVersion(versionInfo: Map<String, String>): Version {
    val major = versionInfo["major"]?.toInt() ?: 0
    val minorAndUpdate = versionInfo["minor"]?.split(".")
    val minor = if (minorAndUpdate?.size == 2) minorAndUpdate[0].toInt() else 0
    val update = if (minorAndUpdate?.size == 2) minorAndUpdate[1].toInt() else 0
    return Version(major, minor, update)
  }

  companion object {
    val LOG = Logger.getInstance(RInterpreterImpl::class.java)

    private const val HTTPS_SUFFIX = "[https]"
    private const val WORD_DELIMITER = " "
    private const val GROUP_DELIMITER = "\t"
    private val whiteSpaceRegex = "\\s".toRegex()

    private val AVAILABLE_PACKAGES_HELPER = RHelpersUtil.findFileInRHelpers("R/interpreter/available_packages.R")
    private val GET_ENV_HELPER = RHelpersUtil.findFileInRHelpers("R/interpreter/get_env.R")
    private val CRAN_MIRRORS_HELPER = RHelpersUtil.findFileInRHelpers("R/interpreter/cran_mirrors.R")
    private val DEFAULT_REPOSITORIES_HELPER = RHelpersUtil.findFileInRHelpers("R/interpreter/default_repositories.R")
    private val LIBRARY_PATHS_HELPER = RHelpersUtil.findFileInRHelpers("R/interpreter/library_paths.R")
    private val INSTALLED_PACKAGES_HELPER = RHelpersUtil.findFileInRHelpers("R/interpreter/installed_packages.R")

    // TODO: run via helper
    fun loadInterpreterVersionInfo(interpreterPath: String, workingDirectory: String): Map<String, String> {
      return runScript("version", interpreterPath, workingDirectory)?.stdoutLines?.map { it.split(' ', limit = 2) }
               ?.filter { it.size == 2 }?.map { it[0] to it[1].trim() }?.toMap() ?: emptyMap()
    }

    private fun runScript(scriptText: String, interpreterPath: String, workingDirectory: String): ProcessOutput? {
      val commandLine = arrayOf<String>(interpreterPath,  "--quiet", "--slave", "-e", scriptText)

      try {
        val processHandler = CapturingProcessHandler(GeneralCommandLine(*commandLine).withWorkDirectory(workingDirectory))
        return processHandler.runProcess(DEFAULT_TIMEOUT)
      }
      catch (e: Throwable) {
        LOG.info("Failed to run R executable: \n" +
                 "Interpreter path " + interpreterPath + "0\n" +
                 "Exception occurred: " + e.message)
      }

      return null
    }

    private fun String.expandTilde(): String {
      return if (startsWith("~" + File.separator)) {
        System.getProperty("user.home") + substring(1)
      } else {
        this
      }
    }

    private fun parseStrings(lines: List<String>): List<String> {
      return lines.mapNotNull { line ->
        val items = line.split(WORD_DELIMITER).filter { it.isNotBlank() }
        if (items.count() >= 2 && items[1].length > 2) {
          items[1].let { it.substring(1, it.length - 1) }
        } else {
          null
        }
      }
    }
  }

  override val skeletonPaths: List<String>
    get() = libraryPaths.map { libraryPath -> libraryPathToSkeletonPath(libraryPath) }

  override fun findLibraryPathBySkeletonPath(skeletonPath: String): String?  =
    libraryPaths.firstOrNull { Paths.get(libraryPathToSkeletonPath(it)) == Paths.get(skeletonPath) }?.path

  override val skeletonsDirectory: String
    get() = "${PathManager.getSystemPath()}${File.separator}${RSkeletonUtil.SKELETON_DIR_NAME}"

  private fun libraryPathToSkeletonPath(libraryPath: VirtualFile) =
    Paths.get(
      skeletonsDirectory,
      "${File.separator}${interpreterName}${File.separator}${hash(libraryPath.path)}${File.separator}"
    ).toString()

  private fun hash(libraryPath: String): String {
    return Paths.get(libraryPath).joinToString(separator = "", postfix = "-${libraryPath.hashCode()}") { it.toString().subSequence(0, 1) }
  }

  private data class State(val libraryPaths: List<VirtualFile>,
                           val installedPackages: List<RPackage>,
                           val name2installedPackages: Map<String, RPackage>,
                           val userLibraryPath: String,
                           val cranMirrors: List<RMirror>,
                           val defaultRepositories: List<RDefaultRepository>) {
    companion object {
      val EMPTY = State(listOf(), listOf(), mapOf(), "", listOf(), listOf())
    }
  }
}