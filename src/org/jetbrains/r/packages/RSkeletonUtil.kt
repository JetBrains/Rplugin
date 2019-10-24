/*
 * Copyright 2011 Holger Brandl
 *
 * This code is licensed under BSD. For details see
 * http://www.opensource.org/licenses/bsd-license.php
 */

package org.jetbrains.r.packages

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.r.bin.RBinFileType
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.packages.LibrarySummary.RLibraryPackage
import org.jetbrains.r.packages.remote.RepoUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit



object RSkeletonUtil {
  const val CUR_SKELETON_VERSION = 1
  const val SKELETON_DIR_NAME = "r_skeletons"
  private const val MAX_THREAD_POOL_SIZE = 4
  private const val FAILED_SUFFIX = ".failed"

  private const val PRIORITY_PREFIX = "## Package priority: "

  private val LOG = Logger.getInstance("#" + RSkeletonUtil::class.java.name)

  fun checkVersion(skeletonsDirectoryPath: String) {
    val skeletonsDirectory = File(skeletonsDirectoryPath)
    val versionFile = File(skeletonsDirectory, "skeletons-version")
    if (versionFile.exists() && versionFile.readText() == CUR_SKELETON_VERSION.toString()) {
      return
    }
    skeletonsDirectory.deleteRecursively()
    if (!skeletonsDirectory.mkdirs()) {
      throw IOException("Can't create skeletons directory")
    }

    versionFile.printWriter().use {
      it.print(CUR_SKELETON_VERSION)
    }
  }

  fun updateSkeletons(rInterpreter: RInterpreter): Boolean {
    checkVersion(rInterpreter.skeletonsDirectory)
    var result = false
    for (skeletonPath in rInterpreter.skeletonPaths) {
      File(skeletonPath).takeIf { !it.exists() }?.let { FileUtil.createDirectory(it) }
      val libraryPath = rInterpreter.findLibraryPathBySkeletonPath(skeletonPath)
      if (libraryPath == null) {
        LOG.error("Cannot find library path for $skeletonPath")
        continue
      }
      val package2skeletonFile = getSkeletonFiles(skeletonPath, libraryPath)
      val currentPackages = package2skeletonFile.keys
      val installedPackages = rInterpreter.installedPackages.filter { it.libraryPath == libraryPath }

      val outdatedPackages = currentPackages.subtract(installedPackages)
      val newPackages = installedPackages.subtract(currentPackages).filterNot { isBanned(it) }

      if (newPackages.isEmpty() && outdatedPackages.isEmpty()) {
        continue
      }

      outdatedPackages.forEach {
        FileUtil.asyncDelete(package2skeletonFile[it] ?: return@forEach)
      }

      result = result || generateSkeletons(skeletonPath, newPackages, rInterpreter)
    }
    return result
  }

  fun generateSkeletons(skeletonPath: String, newPackages: List<RPackage>, rInterpreter: RInterpreter): Boolean {
    var result = false
    val es = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE)

    val skeletonsDir = File(skeletonPath)
    for ((processed, rPackage) in newPackages.withIndex()) {
      val skeletonFile = File(skeletonsDir, rPackage.getLibraryBinFileName())
      val finalProcessed = processed + 1
      val indicator: ProgressIndicator? = ProgressIndicatorProvider.getInstance().progressIndicator
      es.submit {
        try {
          LOG.info("building bin for package '$rPackage'")
          indicator?.apply {
            fraction = finalProcessed.toDouble() / newPackages.size
            text = "Generating bin for '$rPackage'"
          }

          val packageName = rPackage.packageName
          val output = rInterpreter.runHelperWithArgs(RepoUtils.PACKAGE_SUMMARY, packageName)
          if (output.exitCode != 0) {
            LOG.error("Failed to generate skeleton for '" + rPackage + "'. The error was:\n\n" +
                      output.stderr +
                      "\n\nIf you think this issue with plugin and not your R installation, please file a ticket")
            return@submit
          }
          val binPackage: RLibraryPackage = convertToBinFormat(packageName, output.stdout)

          FileOutputStream(skeletonFile).use {
            binPackage.writeTo(it)
          }
          result = true
        }
        catch (e: IOException) {
          LOG.error("Failed to generate skeleton for '$rPackage'. The reason was:", e)
        }
        catch (e: Throwable) {
          LOG.error("Unexpected error: ", e)
        }
      }
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

  fun getSkeletonFiles(skeletonPath: String, libraryPath: String): Map<RPackage, File> {
    val skeletonDirectory = File(skeletonPath)
    if (!skeletonDirectory.exists() || !skeletonDirectory.isDirectory) {
      return emptyMap()
    }
    return skeletonDirectory.listFiles { _, name -> name.endsWith(".${RBinFileType.DOT_R_BIN_EXTENSION}") }?.mapNotNull {
      val nameWithoutExtension = it.nameWithoutExtension
      if (!nameWithoutExtension.contains('-')) {
        return@mapNotNull null
      }
      val nameAndVersion = nameWithoutExtension.split('-', limit = 2)
      if (nameAndVersion.size != 2) {
        return@mapNotNull null
      }
      val priority = getPriorityFromSkeletonFile(it)
      RPackage(nameAndVersion[0], nameAndVersion[1], priority, libraryPath) to it
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

  private fun isBanned(rPackage: RPackage) =
    rPackage.packageName == "tcltk" && SystemInfo.isMac ||
    rPackage.packageName == "translations"

  private const val invalidPackageFormat = "Invalid package summary format"

  private fun convertToBinFormat(packageName: String, packageSummary: String ): RLibraryPackage {
    val packageBuilder = RLibraryPackage.newBuilder().setName(packageName)
    packageBuilder.setName(packageName)
    val lines: List<String> = packageSummary.split("\n")
    if (lines.isEmpty()) throw IOException("Empty summary")

    val priority = when (val it = lines[0]) {
      "NA" -> RLibraryPackage.Priority.NA
      "BASE" -> RLibraryPackage.Priority.BASE
      "RECOMMENDED" -> RLibraryPackage.Priority.RECOMMENDED
      else -> throw IOException("Unknown priority: $it")
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

        val signatureStart = parts[typesEndIndex]
        val prefix = "function ("
        if (!signatureStart.startsWith(prefix)) {
          throw IOException("Invalid function description at $lineNum: " + line)
        }

        val signature = StringBuilder()
        signature.append(signatureStart)

        while (index < symbols.size && symbols[index] != "NULL") {
          signature.append(symbols[index])
          index++
        }
        index++

        if (index > symbols.size) {
          throw IOException("Invalid format at the end of file")
        }

        if (!signature.endsWith(") ")) {
          throw IOException("Invalid function signature at $lineNum: " + signature)
        }

        val parameters = signature.substring(prefix.length, signature.length - 2)
        builder.setParameters(parameters)
      }

      packageBuilder.addSymbols(builder.build())
    }
    return packageBuilder.build()
  }
}
