/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

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
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.common.emptyExpiringList
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.interpreter.RInterpreterUtil.DEFAULT_TIMEOUT
import org.jetbrains.r.packages.*
import org.jetbrains.r.rinterop.RInterop
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class RInterpreterImpl(private val versionInfo: Map<String, String>,
                       override val interpreterPath: String,
                       private val project: Project) : RInterpreter {
  @Volatile
  private var state = State.EMPTY

  @Volatile
  private var updatePromise: Promise<Unit>? = null

  override val version: Version = buildVersion(versionInfo)
  override val interpreterName: String get() = versionInfo["version.string"]?.replace(' ', '_')  ?: "unnamed"
  override val installedPackages get() = state.installedPackages
  override val libraryPaths get() = state.libraryPaths
  override val userLibraryPath get() = state.userLibraryPath
  override val isUpdating get() = updatePromise != null

  private val name2PsiFile = ContainerUtil.createConcurrentSoftKeySoftValueMap<String, PsiFile?>()
  private val updateEpoch = AtomicInteger(0)

  override val interop: RInterop
    get() = getConsoleForInterpreter(this, project).rInterop

  override val skeletonRoots: Set<VirtualFile>
    get() {
      val current = state
      val currentSkeletonRoots = current.skeletonRoots
      if (current.skeletonPaths.size != currentSkeletonRoots.size || !currentSkeletonRoots.all { it.isValid }) {
        if (project.isOpen && !project.isDisposed) {
          updateState().onSuccess {
            RInterpreterUtil.updateIndexableSet(project)
          }
        }
        return currentSkeletonRoots.filter { it.isValid }.toSet()
      }
      return currentSkeletonRoots
    }

  override fun getPackageByName(name: String) = state.name2installedPackages[name]

  override fun getLibraryPathByName(name: String) = state.name2libraryPaths[name]

  override fun getProcessOutput(scriptText: String) = runScript(scriptText, interpreterPath, project.basePath!!)



  override fun getSkeletonFileByPackageName(name: String): PsiFile? {
    val cached = name2PsiFile[name]
    if (cached != null && cached.isValid) {
      return cached
    }
    val rInstalledPackage = getPackageByName(name) ?: return null
    val skeletonFileName = RPackage(rInstalledPackage.name, rInstalledPackage.version).skeletonFileName
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

  @Synchronized
  override fun updateState(): Promise<Unit> {
    return updatePromise ?: createUpdatePromise().also {
      updatePromise = it
    }
  }

  private fun createUpdatePromise(): Promise<Unit> {
    return runAsync { doUpdateState() }
      .onProcessed { resetUpdatePromise() }
      .onError { LOG.error("Unable to update state", it) }
  }

  @Synchronized
  private fun resetUpdatePromise() {
    updatePromise = null
  }

  private fun doUpdateState() {
    state = State.EMPTY
    updateEpoch.incrementAndGet()
    name2PsiFile.clear()
    val installedPackages = loadInstalledPackages()
    val name2installedPackages = installedPackages.asSequence().map { it.packageName to it }.toMap()
    val libraryPaths = loadLibraryPaths()
    val name2libraryPaths = mapNamesToLibraryPaths(installedPackages, libraryPaths)
    val skeletonPaths = libraryPaths.map { libraryPath -> libraryPathToSkeletonPath(libraryPath) }
    val skeletonRoots = skeletonPaths.mapNotNull { path -> VfsUtil.findFile(Paths.get(path), true) }.toSet()
    state = State(libraryPaths, skeletonPaths, skeletonRoots, makeExpiring(installedPackages), name2installedPackages,
                  name2libraryPaths, getUserPath())
  }

  private fun <E>makeExpiring(values: List<E>): ExpiringList<E> {
    val usedUpdateEpoch = updateEpoch.get()
    return ExpiringList(values) {
      usedUpdateEpoch < updateEpoch.get()
    }
  }

  private fun getUserPath(): String {
    val lines = runHelper(GET_ENV_HELPER, "R_LIBS_USER")
    val firstLine = lines[0]
    if (firstLine.isNotBlank()) {
      return firstLine.expandTilde()
    } else {
      throw RuntimeException("Cannot get user library path")
    }
  }

  private fun mapNamesToLibraryPaths(packages: List<RInstalledPackage>, libraryPaths: List<VirtualFile>): Map<String, VirtualFile> {
    return packages.asSequence()
      .mapNotNull { rPackage ->
        libraryPaths.find { it.path == rPackage.libraryPath }?.let { vf -> Pair(rPackage.packageName, vf) }
      }
      .toMap()
  }

  private fun loadLibraryPaths(): List<VirtualFile> {
    val lines = runHelper(LIBRARY_PATHS_HELPER)
    val paths = lines.filter { it.isNotBlank() }
    return paths.mapNotNull { VfsUtil.findFileByIoFile(File(it), true) }.toList().also {
      if (it.isEmpty()) LOG.error("Got empty library paths, output: ${lines}")
    }
  }

  private fun loadInstalledPackages(): List<RInstalledPackage> {
    val text = RInterpreterUtil.runHelper(interpreterPath, INSTALLED_PACKAGES_HELPER, project.basePath, arrayOf<String>().toList())
    val lines = text.split("!!!JETBRAINS_RPLUGIN!!!")
    return if (lines.isNotEmpty()) {
      val obtained = lines.asSequence()
        .filter { it.isNotBlank() }
        .map {
          val splitLine = it.split("^^^JETBRAINS_RPLUGIN^^^")
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
            val title = splitLine[4].trim()
            val url = splitLine[5].trim()
            RInstalledPackage(packageName, version, priority, libraryPath, mapOf("Title" to title, "URL" to url))
          }
          catch (e: Throwable) {
            throw RuntimeException("failed to split package-version in line '$it'", e)
          }
        }

      // Obtained sequence contains duplicates of the same packages but for different versions.
      // The most recent ones go first.
      // Also it's not sorted by package names
      val names2packages = TreeMap<String, RInstalledPackage>(String.CASE_INSENSITIVE_ORDER)
      for (rPackage in obtained) {
        names2packages.getOrPut(rPackage.packageName, { rPackage })
      }
      names2packages.values.toList()
    } else {
      listOf()
    }
  }

  private fun buildVersion(versionInfo: Map<String, String>): Version {
    val major = versionInfo["major"]?.toInt() ?: 0
    val minorAndUpdate = versionInfo["minor"]?.split(".")
    val minor = if (minorAndUpdate?.size == 2) minorAndUpdate[0].toInt() else 0
    val update = if (minorAndUpdate?.size == 2) minorAndUpdate[1].toInt() else 0
    return Version(major, minor, update)
  }

  private fun runHelper(helper: File, vararg args: String) =
    RInterpreterUtil.runHelper(interpreterPath, helper, project.basePath, args.toList()).lines()

  companion object {
    val LOG = Logger.getInstance(RInterpreterImpl::class.java)

    private val GET_ENV_HELPER = RHelpersUtil.findFileInRHelpers("R/interpreter/get_env.R")
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

    private fun getConsoleForInterpreter(interpreter: RInterpreter, project: Project): RConsoleView {
      val current = RConsoleManager.getInstance(project).currentConsoleOrNull
      return if (current != null && current.interpreterPath == interpreter.interpreterPath) {
        current
      } else {
        RConsoleManager.runConsole(project)
          .onError { LOG.error("Cannot run new console for interpreter", it) }
          .blockingGet(DEFAULT_TIMEOUT) ?: throw RuntimeException("Cannot run new console")
      }
    }

    private fun String.expandTilde(): String {
      return if (startsWith("~" + File.separator)) {
        System.getProperty("user.home") + substring(1)
      } else {
        this
      }
    }
  }

  override val skeletonPaths: List<String>
    get() = state.skeletonPaths

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
                           val skeletonPaths: List<String>,
                           val skeletonRoots: Set<VirtualFile>,
                           val installedPackages: ExpiringList<RInstalledPackage>,
                           val name2installedPackages: Map<String, RInstalledPackage>,
                           val name2libraryPaths: Map<String, VirtualFile>,
                           val userLibraryPath: String) {
    companion object {
      val EMPTY = State(listOf(), listOf(), setOf(), emptyExpiringList(), mapOf(), mapOf(), "")
    }
  }
}