// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.packages.RHelpersUtil
import org.jetbrains.r.rinterop.RInterop
import java.awt.Dimension
import java.awt.Toolkit
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.max

object RGraphicsUtils {
  data class ScreenParameters(
    val dimension: Dimension,
    val resolution: Int?
  ) {
    val width: Int
      get() = dimension.width

    val height: Int
      get() = dimension.height
  }

  data class InstallProperties(
    val packagePath: String,
    val libraryPath: String,
    val packageType: String
  )

  data class InitProperties(
    val snapshotDirectory: String,
    val screenParameters: ScreenParameters
  )

  private const val LOCAL_FOLDER_NAME = "rplugin"
  private const val PACKAGE_NAME_FORMAT = "%s_%s.%s"
  private const val PACKAGE_NAME = "rplugingraphics"
  private const val PACKAGE_VERSION = "0.2.2"
  private const val BINARIES_DIR_NAME = "r-binaries"

  private const val FULL_HD_HEIGHT = 1080
  private const val QUAD_HD_HEIGHT = 1440
  private const val ULTRA_HD_HEIGHT = 2160

  private const val RESOLUTION_MULTIPLIER = 4
  private const val MINIMAL_GRAPHICS_RESOLUTION = 75
  private const val FALLBACK_RESOLUTION = 150
  private const val FULL_HD_RESOLUTION = 300
  private const val QUAD_HD_RESOLUTION = 450
  private const val ULTRA_HD_RESOLUTION = 600

  private val isRetina: Boolean = SystemInfo.isMac && UIUtil.isRetina() && !ApplicationManager.getApplication().isUnitTestMode

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
    return calculateInitProperties(snapshotDirectory, screenParameters?.dimension, screenParameters?.resolution)
  }

  fun calculateInitProperties(snapshotDirectory: String, dimension: Dimension?, resolution: Int?): InitProperties {
    val path = FileUtil.toSystemIndependentName(snapshotDirectory)
    val parameters = scaleForRetina(createParameters(dimension, resolution))
    return InitProperties(path, parameters)
  }

  fun createGraphicsDevice(rInterop: RInterop, screenDimension: Dimension?, resolution: Int?): RGraphicsDevice {
    // Note: 'FileUtil.createTempFile()' will break unit-tests
    val tmpDirectory = Files.createTempDirectory("rplugin-graphics").toFile()
    tmpDirectory.deleteOnExit()
    val parameters = createParameters(screenDimension, resolution)
    return RGraphicsDevice(rInterop, tmpDirectory, parameters)
  }

  fun getDefaultScreenParameters(isFullScreenMode: Boolean = true): ScreenParameters {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val height = screenSize.height
    val resolution = when {
      height >= ULTRA_HD_HEIGHT -> ULTRA_HD_RESOLUTION
      height >= QUAD_HD_HEIGHT -> QUAD_HD_RESOLUTION
      height >= FULL_HD_HEIGHT -> FULL_HD_RESOLUTION
      else -> FALLBACK_RESOLUTION
    }
    val adjustedResolution = if (isFullScreenMode) resolution else max(resolution / RESOLUTION_MULTIPLIER, MINIMAL_GRAPHICS_RESOLUTION)
    return ScreenParameters(screenSize, adjustedResolution)
  }

  private fun createParameters(dimension: Dimension?, resolution: Int?): ScreenParameters {
    return if (dimension != null) {
      ScreenParameters(dimension, resolution)
    } else {
      val parameters = getDefaultScreenParameters(false)
      if (resolution != null) {
        parameters.copy(resolution = resolution)
      } else {
        parameters
      }
    }
  }

  private fun scaleForRetina(dimension: Dimension): Dimension =
    if (isRetina) Dimension(dimension.width * 2, dimension.height * 2) else dimension

  internal fun scaleForRetina(parameters: ScreenParameters): ScreenParameters =
    if (isRetina) ScreenParameters(scaleForRetina(parameters.dimension), parameters.resolution?.times(2)) else parameters
}