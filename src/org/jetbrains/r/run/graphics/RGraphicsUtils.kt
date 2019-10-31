// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtil
import icons.org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.packages.RHelpersUtil
import java.awt.Toolkit
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object RGraphicsUtils {
  data class ScreenParameters(
    val width: Int,
    val height: Int,
    val resolution: Int?
  )

  data class InstallProperties(
    val packagePath: String,
    val libraryPath: String,
    val packageType: String
  )

  data class InitProperties(
    val snapshotDirectory: String,
    val screenParameters: ScreenParameters,
    val scaleFactor: Double
  )

  private const val LOCAL_FOLDER_NAME = "rplugin"
  private const val PACKAGE_NAME_FORMAT = "%s_%s.%s"
  private const val PACKAGE_NAME = "rplugingraphics"
  private const val PACKAGE_VERSION = "0.2.2"
  private const val BINARIES_DIR_NAME = "r-binaries"

  private const val FULL_HD_HEIGHT = 1080
  private const val QUAD_HD_HEIGHT = 1440
  private const val ULTRA_HD_HEIGHT = 2160

  private const val MINIMAL_GRAPHICS_RESOLUTION = 75
  private const val FALLBACK_RESOLUTION = 150
  private const val FULL_HD_RESOLUTION = 300
  private const val QUAD_HD_RESOLUTION = 450
  private const val ULTRA_HD_RESOLUTION = 600

  private fun getAvailableVersions(): List<Version> {
    val binariesDirectory = RHelpersUtil.findFileInRHelpers(BINARIES_DIR_NAME)
    val directories = binariesDirectory.listFiles()
    val versions = mutableListOf<Version>()
    if (directories != null) {
      for (directory in directories) {
        if (directory.isDirectory) {
          // Convert name to version
          val name = directory.name
          val tokens = name.split('.')
          if (tokens.size == 3) {
            val major = tokens[0].toInt()
            val minor = tokens[1].toInt()
            val micro = tokens[2].toInt()
            versions.add(Version(major, minor, micro))
          }
        }
      }
    }
    if (versions.isEmpty()) {
      throw RuntimeException("No available binary versions for graphics device package")
    }
    return versions
  }

  private fun getSuitableVersion(version: Version): Version? {
    val versions = getAvailableVersions()
    for (available in versions) {
      if (available.major == version.major && available.minor == version.minor) {
        return available
      }
    }
    return null
  }

  private fun calculatePackageRelativePath(version: Version?): String {
    return if (version != null && version.`is`(3) && version.minor > 2 && version.minor <= 6) {
      "R-3.${version.minor}/${String.format(PACKAGE_NAME_FORMAT, PACKAGE_NAME, PACKAGE_VERSION, "zip")}"
    } else {
      "R/${String.format(PACKAGE_NAME_FORMAT, PACKAGE_NAME, PACKAGE_VERSION, "tar.gz")}"
    }
  }

  private fun getPackageFile(version: Version?): File {
    val packagePath = calculatePackageRelativePath(version)
    val packageFile = RHelpersUtil.findFileInRHelpers(packagePath)
    return if (packageFile.exists()) {
      packageFile
    } else {
      throw RuntimeException("Cannot find graphics library package: $packagePath")
    }
  }

  private fun calculateSystemIndependentPath(file: File): String {
    return FileUtil.toSystemIndependentName(file.absolutePath)
  }

  private fun calculateLocalLibraryPath(): String {
    val localBasePath = PathManager.getSystemPath()
    val localLibraryFile = Paths.get(localBasePath, LOCAL_FOLDER_NAME).toFile()
    if (!localLibraryFile.mkdirs() && !localLibraryFile.exists()) {
      throw RuntimeException("Could not create local library folder")
    }
    return calculateSystemIndependentPath(localLibraryFile)
  }

  fun calculateInitProperties(snapshotDirectory: String, screenParameters: ScreenParameters?): InitProperties {
    fun getScaleFactor(parameters: ScreenParameters): Double {
      val resolution = parameters.resolution
      return if (SystemInfo.isMac && resolution != null) {
        resolution.toDouble() / MINIMAL_GRAPHICS_RESOLUTION
      } else {
        1.0
      }
    }

    val path = FileUtil.toSystemIndependentName(snapshotDirectory)
    val parameters = screenParameters ?: getDefaultScreenParameters()
    val scaleFactor = getScaleFactor(parameters)
    return InitProperties(path, parameters, scaleFactor)
  }

  fun createGraphicsState(screenParameters: ScreenParameters?): RGraphicsState {
    // Note: 'FileUtil.createTempFile()' will break unit-tests
    val tmpDirectory = Files.createTempDirectory("rplugin-graphics").toFile()
    tmpDirectory.deleteOnExit()
    return RBasicGraphicsState(tmpDirectory, screenParameters)
  }

  fun getDefaultScreenParameters(): ScreenParameters {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val width = screenSize.width
    val height = screenSize.height
    val resolution = when {
      height >= ULTRA_HD_HEIGHT -> ULTRA_HD_RESOLUTION
      height >= QUAD_HD_HEIGHT -> QUAD_HD_RESOLUTION
      height >= FULL_HD_HEIGHT -> FULL_HD_RESOLUTION
      else -> FALLBACK_RESOLUTION
    }
    return ScreenParameters(width, height, resolution)
  }
}