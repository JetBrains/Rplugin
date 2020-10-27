// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.JreHiDpiUtil
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.rinterop.RInterop
import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.Toolkit
import java.io.File
import java.nio.file.Files
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

  private const val FULL_HD_HEIGHT = 1080
  private const val QUAD_HD_HEIGHT = 1440
  private const val ULTRA_HD_HEIGHT = 2160

  private const val RESOLUTION_MULTIPLIER = 4
  private const val MINIMAL_GRAPHICS_RESOLUTION = 75
  private const val FALLBACK_RESOLUTION = 150
  private const val FULL_HD_RESOLUTION = 300
  private const val QUAD_HD_RESOLUTION = 450
  private const val ULTRA_HD_RESOLUTION = 600

  private val DEFAULT_DIMENSION = Dimension(1920, 1080)

  const val DEFAULT_RESOLUTION = 72
  val DEFAULT_PARAMETERS = ScreenParameters(DEFAULT_DIMENSION, DEFAULT_RESOLUTION)

  internal val isHiDpi = (JreHiDpiUtil.isJreHiDPI(null as GraphicsConfiguration?) || UIUtil.isRetina()) &&
                         !ApplicationManager.getApplication().isUnitTestMode

  fun createParameters(parameters: ScreenParameters?): ScreenParameters {
    return createParameters(parameters?.dimension, parameters?.resolution)
  }

  fun createGraphicsDevice(rInterop: RInterop): RGraphicsDevice {
    return createGraphicsDevice(rInterop, DEFAULT_DIMENSION, DEFAULT_RESOLUTION)
  }

  fun createGraphicsDevice(rInterop: RInterop, screenDimension: Dimension?, resolution: Int?): RGraphicsDevice {
    val tmpDirectory = createTempDeviceDirectory()
    val parameters = createParameters(screenDimension, resolution)
    return RGraphicsDevice(rInterop, tmpDirectory, parameters, true)
  }

  private fun createTempDeviceDirectory(): File {
    // Note: 'FileUtil.createTempFile()' will break unit-tests
    return Files.createTempDirectory("rplugin-graphics").toFile().apply {
      deleteOnExit()
    }
  }

  fun getDefaultScreenParameters(isFullScreenMode: Boolean = true): ScreenParameters {
    val screenSize = getScreenSize()
    val resolution = getDefaultResolution(screenSize, isFullScreenMode)
    return ScreenParameters(screenSize.downscaleIf(!isFullScreenMode), resolution)
  }

  fun getDefaultResolution(isFullScreenMode: Boolean): Int {
    return getDefaultResolution(getScreenSize(), isFullScreenMode)
  }

  private fun getDefaultResolution(screenSize: Dimension, isFullScreenMode: Boolean): Int {
    val height = screenSize.height
    val resolution = when {
      height >= ULTRA_HD_HEIGHT -> ULTRA_HD_RESOLUTION
      height >= QUAD_HD_HEIGHT -> QUAD_HD_RESOLUTION
      height >= FULL_HD_HEIGHT -> FULL_HD_RESOLUTION
      else -> FALLBACK_RESOLUTION
    }
    return if (isFullScreenMode) resolution else max(resolution / RESOLUTION_MULTIPLIER, MINIMAL_GRAPHICS_RESOLUTION)
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

  private fun getScreenSize(): Dimension {
    return Toolkit.getDefaultToolkit().screenSize
  }

  private fun scaleForHiDpi(dimension: Dimension): Dimension =
    if (isHiDpi) Dimension(dimension.width * 2, dimension.height * 2) else dimension

  fun scaleForHiDpi(parameters: ScreenParameters): ScreenParameters =
    if (isHiDpi) ScreenParameters(scaleForHiDpi(parameters.dimension), parameters.resolution?.times(2)) else parameters

  internal fun downscaleForHiDpi(resolution: Int): Int =
    if (isHiDpi) resolution / 2 else resolution

  private fun Dimension.downscaleIf(condition: Boolean): Dimension {
    return if (condition) Dimension(width / 2, height / 2) else this
  }
}