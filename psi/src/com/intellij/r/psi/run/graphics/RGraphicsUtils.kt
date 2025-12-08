package com.intellij.r.psi.run.graphics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JreHiDpiUtil
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.Toolkit
import kotlin.math.max

object RGraphicsUtils {
  const val DEFAULT_RESOLUTION = 72

  private const val FULL_HD_HEIGHT = 1080
  private const val QUAD_HD_HEIGHT = 1440
  private const val ULTRA_HD_HEIGHT = 2160

  private const val RESOLUTION_MULTIPLIER = 4
  private const val MINIMAL_GRAPHICS_RESOLUTION = 75
  private const val FALLBACK_RESOLUTION = 150
  private const val FULL_HD_RESOLUTION = 300
  private const val QUAD_HD_RESOLUTION = 450
  private const val ULTRA_HD_RESOLUTION = 600

  fun createParameters(parameters: RGraphicsUtils.ScreenParameters?): RGraphicsUtils.ScreenParameters {
    return createParameters(parameters?.dimension, parameters?.resolution)
  }

  fun getScreenSize(): Dimension {
    return Toolkit.getDefaultToolkit().screenSize
  }

  fun createParameters(dimension: Dimension?, resolution: Int?): ScreenParameters {
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

  fun getDefaultScreenParameters(isFullScreenMode: Boolean = true): ScreenParameters {
    val screenSize = getScreenSize()
    val resolution = getDefaultResolution(screenSize, isFullScreenMode)
    return ScreenParameters(screenSize.downscaleIf(!isFullScreenMode), resolution)
  }

  private fun Dimension.downscaleIf(condition: Boolean): Dimension {
    return if (condition) Dimension(width / 2, height / 2) else this
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

  /*internal*/ val isHiDpi = (JreHiDpiUtil.isJreHiDPI(null as GraphicsConfiguration?) || UIUtil.isRetina()) &&
                         !ApplicationManager.getApplication().isUnitTestMode

  private fun scaleForHiDpi(dimension: Dimension): Dimension =
    if (isHiDpi) Dimension(dimension.width * 2, dimension.height * 2) else dimension

  fun scaleForHiDpi(parameters: ScreenParameters): ScreenParameters =
    if (isHiDpi) ScreenParameters(scaleForHiDpi(parameters.dimension), parameters.resolution?.times(2)) else parameters

  internal fun downscaleForHiDpi(resolution: Int): Int =
    if (isHiDpi) resolution / 2 else resolution

  data class ScreenParameters(
    val dimension: Dimension,
    val resolution: Int?
  ) {
    val width: Int
      get() = dimension.width

    val height: Int
      get() = dimension.height
  }
}