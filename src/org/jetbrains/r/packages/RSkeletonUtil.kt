/*
 * Copyright 2011 Holger Brandl
 *
 * This code is licensed under BSD. For details see
 * http://www.opensource.org/licenses/bsd-license.php
 */

package org.jetbrains.r.packages

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.RMultiOutputProcessor
import org.jetbrains.r.packages.LibrarySummary.RLibraryPackage
import org.jetbrains.r.packages.remote.RepoUtils
import org.jetbrains.r.skeleton.RSkeletonFileType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Integer.min
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


object RSkeletonUtil {
  private const val CUR_SKELETON_VERSION = 7
  const val SKELETON_DIR_NAME = "r_skeletons"
  private const val MAX_THREAD_POOL_SIZE = 4
  private const val FAILED_SUFFIX = ".failed"
  private const val PRIORITY_PREFIX = "## Package priority: "
  private const val MAX_BUCKET_SIZE = 25 // 4 threads consume a total of ~1GB memory

  private val LOG = Logger.getInstance("#" + RSkeletonUtil::class.java.name)

  fun checkVersion(skeletonsDirectoryPath: String) {
    val skeletonsDirectory = File(skeletonsDirectoryPath)
    val versionFile = File(skeletonsDirectory, "skeletons-version")
    if (versionFile.exists() && versionFile.readText() == CUR_SKELETON_VERSION.toString()) {
      return
    }
    skeletonsDirectory.deleteRecursively()
    if (!skeletonsDirectory.mkdirs()) {
      if (!skeletonsDirectory.exists())
        throw IOException("Can't create skeletons directory")
    }

    versionFile.printWriter().use {
      it.print(CUR_SKELETON_VERSION)
    }
  }

  fun updateSkeletons(rInterpreter: RInterpreter, project: Project, progressIndicator: ProgressIndicator? = null): Boolean {
    checkVersion(rInterpreter.skeletonsDirectory)
    val generationMap = HashMap<String, List<RPackage>>()
    for (skeletonPath in rInterpreter.skeletonPaths) {
      File(skeletonPath).takeIf { !it.exists() }?.let { FileUtil.createDirectory(it) }
      val libraryPath = rInterpreter.findLibraryPathBySkeletonPath(skeletonPath)
      if (libraryPath == null) {
        LOG.error("Cannot find library path for $skeletonPath")
        continue
      }
      val package2skeletonFile = getSkeletonFiles(skeletonPath)
      val currentPackages = package2skeletonFile.keys
      val installedPackages = rInterpreter.installedPackages.filter { it.libraryPath == libraryPath }
                                                            .map { RPackage(it.packageName, it.packageVersion) }

      val outdatedPackages = currentPackages.subtract(installedPackages)
      val newPackages = installedPackages.subtract(currentPackages).filterNot { isBanned(it.name) }

      outdatedPackages.forEach {
        FileUtil.asyncDelete(package2skeletonFile[it] ?: return@forEach)
      }

      generationMap[skeletonPath] = newPackages
    }
    return generateSkeletons(generationMap, rInterpreter, project, progressIndicator)
  }

  internal fun generateSkeletons(generationMap: Map<String, List<RPackage>>,
                                 rInterpreter: RInterpreter,
                                 project: Project,
                                 progressIndicator: ProgressIndicator? = null): Boolean {
    var result = false

    val es = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE)

    val promises = mutableListOf<AsyncPromise<Boolean>>()
    val fullSize = generationMap.values.map { it.size }.sum()
    for ((skeletonPath, _newPackages) in generationMap) {
      if (_newPackages.isEmpty()) continue
      val newPackages = _newPackages.shuffled() // to increase the probability of uniform distribution between threads
      val skeletonsDir = File(skeletonPath)
      var bucketSize = newPackages.size / MAX_THREAD_POOL_SIZE
      if (newPackages.size % MAX_THREAD_POOL_SIZE != 0) ++bucketSize
      bucketSize = min(bucketSize, MAX_BUCKET_SIZE)
      for (i in newPackages.indices step bucketSize) {
        val rPackages = (i until i + bucketSize).mapNotNull { newPackages.getOrNull(it) }
        val skeletonFiles = rPackages.map { File(skeletonsDir, it.skeletonFileName) }
        val indicator: ProgressIndicator? = progressIndicator ?: ProgressIndicatorProvider.getInstance().progressIndicator
        val skeletonProcessor = RSkeletonProcessor(es, rInterpreter, indicator, fullSize, rPackages, skeletonFiles)
        promises.add(skeletonProcessor.runSkeletonHelper())
      }
    }

    for (promise in promises) {
      promise.blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT)?.let { result = result || it } // Wait for all helpers
    }

    try {
      es.shutdown()
      es.awaitTermination(1, TimeUnit.HOURS)
    }
    catch (e: InterruptedException) {
      e.printStackTrace()
    }
    return result
  }

  fun parsePackageAndVersionFromSkeletonFilename(nameWithoutExtension: String): Pair<String, String>? =
    nameWithoutExtension.takeIf { it.contains('-') }
                        ?.split('-', limit = 2)
                        ?.takeIf { it.size == 2 }
                        ?.let { Pair(it[0], it[1])}

  internal fun getSkeletonFiles(skeletonPath: String): Map<RPackage, File> {
    val skeletonDirectory = File(skeletonPath)
    if (!skeletonDirectory.exists() || !skeletonDirectory.isDirectory) {
      return emptyMap()
    }
    return skeletonDirectory.listFiles { _, name -> name.endsWith(".${RSkeletonFileType.EXTENSION}") }?.mapNotNull {
      val (name, version) = RPackage.createRPackageBySkeletonFileName(it.name) ?: return@mapNotNull null
      RPackage(name, version) to it
    }?.toMap() ?: mapOf()
  }

  fun getPriorityFromSkeletonFile(file: File): RPackagePriority? {
    return try {
      val priority = file.inputStream().use {
        RLibraryPackage.parseFrom(it).priority
      }
      return when (priority) {
        RLibraryPackage.Priority.NA -> RPackagePriority.NA
        RLibraryPackage.Priority.BASE -> RPackagePriority.BASE
        RLibraryPackage.Priority.RECOMMENDED -> RPackagePriority.RECOMMENDED
        else -> null
      }
    } catch (e: Exception) {
      LOG.warn("Failed to read package priority from skeleton file $file", e)
      null
    }
  }

  private fun isBanned(packageName: String) =
    packageName == "tcltk" && SystemInfo.isMac ||
    packageName == "translations"

  private const val invalidPackageFormat = "Invalid package summary format"

  private fun convertToBinFormat(packageName: String, packageSummary: String ): RLibraryPackage {
    val packageBuilder = RLibraryPackage.newBuilder().setName(packageName)
    packageBuilder.setName(packageName)
    val lines: List<String> = packageSummary.lines()
    if (lines.isEmpty()) throw IOException("Empty summary")

    val priority = when (val it = lines[0].trim()) {
      "", "NA" -> RLibraryPackage.Priority.NA
      "BASE" -> RLibraryPackage.Priority.BASE
      "RECOMMENDED" -> RLibraryPackage.Priority.RECOMMENDED
      "OPTIONAL" -> RLibraryPackage.Priority.OPTIONAL
      else -> {
        LOG.error("Unknown priority for package $packageName: $it",
                  Attachment("$packageName.RSummary", packageSummary))
        RLibraryPackage.Priority.NA
      }
    }
    packageBuilder.setPriority(priority)

    val symbols = lines.subList(1, lines.size)

    var index = 0
    while (index < symbols.size) {
      val lineNum = index + 1
      val line = symbols[index]
      index++

      val parts = line.split('\u0001')

      if (parts.size < 3) {
        throw IOException("Too short line $lineNum: " + line)
      }

      val methodName = parts[0]
      val exported = parts[1] == "TRUE"
      val builder = LibrarySummary.RLibrarySymbol.newBuilder()
        .setName(methodName)
        .setExported(exported)
      val typesNumber = parts[2].toInt()

      val typesEndIndex = 3 + typesNumber
      if (parts.size < typesEndIndex) {
        throw IOException("Expected $typesNumber types in line $lineNum: " + line)
      }

      val types = parts.subList(3, typesEndIndex)

      if (types.contains("function") && typesEndIndex < parts.size) {
        //No "function" description for exported symbols like `something <- .Primitive("some_primitive")`
        builder.setType(LibrarySummary.RLibrarySymbol.Type.FUNCTION)

        val signature = parts[typesEndIndex]
        val prefix = "function ("
        if (!signature.startsWith(prefix) || !signature.endsWith(") ")) {
          throw IOException("Invalid function description at $lineNum: " + signature)
        }

        val parameters = signature.substring(prefix.length, signature.length - 2)
        builder.setParameters(parameters)

        if (parts.size > typesEndIndex + 1) {
          val extraNamedArgsBuilder = LibrarySummary.RLibrarySymbol.ExtraNamedArguments.newBuilder()
          extraNamedArgsBuilder.addAllArgNames(parts[typesEndIndex + 1].split(";"))
          extraNamedArgsBuilder.addAllFunArgNames(parts[typesEndIndex + 2].split(";"))
          builder.setExtraNamedArguments(extraNamedArgsBuilder)
        }
      }
      else if (types.contains("data.frame")) {
        builder.setType(LibrarySummary.RLibrarySymbol.Type.DATASET)
      }
      packageBuilder.addSymbols(builder.build())
    }
    return packageBuilder.build()
  }

  private class RSkeletonProcessor(private val es: ExecutorService,
                                   private val rInterpreter: RInterpreter,
                                   private val indicator: ProgressIndicator?,
                                   private val allNewPackagesCnt: Int,
                                   private val rPackages: List<RPackage>,
                                   private val skeletonFiles: List<File>) : RMultiOutputProcessor {

    private val resPromise = AsyncPromise<Boolean>()
    private var curPackage: Int = -1
    private val extraNamedArgumentsHelperPath = rInterpreter.uploadHelperToHost(extraNamedArgumentsHelper)
    private val packageNames = rPackages.map { it.name }
    private var hasGeneratedSkeletons = false

    override fun beforeStart() {
      indicator?.isIndeterminate = false
      nextPackageProcess()
    }

    override fun onOutputAvailable(output: String) {
      val rPackage = rPackages[curPackage]
      val skeletonFile = skeletonFiles[curPackage]
      nextPackageProcess()
      try {
        if (output.startsWith("intellij-cannot-load-package")) {
          LOG.warn("Cannot load package $rPackage in R interpreter")
          return
        }
        val binPackage: RLibraryPackage = convertToBinFormat(rPackage.name, output)
        FileOutputStream(skeletonFile).use { binPackage.writeTo(it) }
        hasGeneratedSkeletons = true
      }
      catch (e: Throwable) {
        val attachments = arrayOf(Attachment("$rPackage.RSummary", output))
        LOG.error("Failed to generate skeleton for '$rPackage'. The reason was:", e, *attachments)
      }
    }

    override fun onTerminated(exitCode: Int, stderr: String) {
      if (exitCode != 0) {
        LOG.error("Failed to generate skeleton for '" + rPackages[curPackage] + "'. The error was:\n\n" + stderr +
                  "\n\nIf you think this issue with plugin and not your R installation, please file a ticket")
        if (curPackage < rPackages.size - 1) {
          runSkeletonHelper() // Rerun helper for tail
          return
        }
      }
      resPromise.setResult(hasGeneratedSkeletons)
    }

    /**
     * @return an [AsyncPromise] that will be set when generation is complete for all [rPackages].
     * Set to `true` if at least 1 skeleton is generated successfully. `False` otherwise
     */
    fun runSkeletonHelper(): AsyncPromise<Boolean> {
      val resPromise = resPromise
      es.submit {
        val packageNames = packageNames.subList(curPackage + 1, packageNames.size)
        rInterpreter.runMultiOutputHelper(RepoUtils.PACKAGE_SUMMARY, null,
                                          listOf(extraNamedArgumentsHelperPath) + packageNames, this)
      }
      return resPromise
    }

    private fun nextPackageProcess() {
      ++curPackage
      if (curPackage < rPackages.size) {
        indicator?.apply {
          fraction += 1.0 / allNewPackagesCnt
          text = "Generating bin for '${rPackages[curPackage]}'"
        }
      }
    }
    companion object {
      private val extraNamedArgumentsHelper = RPluginUtil.findFileInRHelpers("R/extraNamedArguments.R")
    }
  }
}

data class RPackage(val name: String, val version: String) {
  val skeletonFileName
    get() = "$name-${version}.${RSkeletonFileType.EXTENSION}"

  companion object {
    private val SKELETON_FILE_REGEX = "([^-]*)-(.*)\\.${RSkeletonFileType.EXTENSION}".toRegex()

    /**
     * if [file] type is Skeleton File Type, returns package and version which was used for its generation or null otherwise
     */
    fun getOrCreateRPackageBySkeletonFile(file: PsiFile): RPackage? {
      if (file.virtualFile.fileType != RSkeletonFileType) return null
      return CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result<RPackage>(createRPackageBySkeletonFileName(file.virtualFile.name), file)
      }
    }

    fun createRPackageBySkeletonFileName(name: String): RPackage? = SKELETON_FILE_REGEX.matchEntire(name)?.let {
      RPackage(it.groupValues[1], it.groupValues[2])
    }
  }
}
