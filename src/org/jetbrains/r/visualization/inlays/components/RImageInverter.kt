/*
 * Copyright 2000-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays.components

import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

/**
 * copy of ImageInverter for decoupling from visualization module
 */
internal class RImageInverter(foreground: Color, background: Color) {
  private val rgb = FloatArray(3)
  private val hsl = FloatArray(3)
  private val whiteHsl = FloatArray(3)
  private val blackHsl = FloatArray(3)

  init {
    foreground.getRGBColorComponents(rgb)
    convertRGBtoHSL(rgb, whiteHsl)
    background.getRGBColorComponents(rgb)
    convertRGBtoHSL(rgb, blackHsl)
  }

  /**
   * Check if [image] should be inverted in dark themes.
   *
   * @param brightnessThreshold images with average brightness exceeding the threshold will be recommended for inversion
   *
   * @return true if it's recommended to invert the image
   */
  fun shouldInvert(image: BufferedImage, brightnessThreshold: Double = 0.7): Boolean {
    val colors = getImageSample(image)
    val numberOfColorsInComplexImage = 5000
    val numberOfPixels = colors.size
    val numberOfColorsThreshold = min(numberOfPixels / 3, numberOfColorsInComplexImage)
    val hasAlpha = image.colorModel.hasAlpha()

    val averageBrightness = colors.map { getBrightness(it, hasAlpha) }.sum() / numberOfPixels
    val numberOfColors = colors.toSet()

    return (averageBrightness > brightnessThreshold && numberOfColors.size < numberOfColorsThreshold) ||
           hasLightBackground(colors, hasAlpha, brightnessThreshold) == true
  }

  /**
   * Get part of image for color analysis.
   *
   * For narrow/low images all image pixels are returned.
   * For regular images the result is a concatenation of areas in image corners and at the central area.
   */
  private fun getImageSample(image: BufferedImage): IntArray {
    if (image.height < 10 || image.width < 10) {
      val colors = IntArray(image.height * image.width)
      image.getRGB(0, 0, image.width, image.height, colors, 0, image.width)
      return colors
    }

    val defaultSpotSize = min(max(image.height / 10, image.width / 10), min(image.height, image.width))
    val spotHeight = min(image.height, defaultSpotSize)
    val spotWidth = min(image.width, defaultSpotSize)

    val spotSize = spotHeight * spotWidth
    val colors = IntArray(spotSize * 5)

    image.getRGB(0, 0, spotWidth, spotHeight, colors, 0, spotWidth)
    image.getRGB(image.width - spotWidth, 0, spotWidth, spotHeight, colors, spotSize, spotWidth)
    image.getRGB(0, image.height - spotHeight, spotWidth, spotHeight, colors, 2 * spotSize, spotWidth)
    image.getRGB(image.width - spotWidth, image.height - spotHeight, spotWidth, spotHeight, colors, 3 * spotSize, spotWidth)

    // We operate on integers so dividing and multiplication with the same number is not trivial operation 
    val centralSpotX = image.width / spotWidth / 2 * spotWidth
    val centralSpotY = image.height / spotHeight / 2 * spotHeight

    image.getRGB(centralSpotX, centralSpotY, spotWidth, spotHeight, colors, 4 * spotSize, spotWidth)

    return colors
  }

  private fun getBrightness(argb: Int, hasAlpha: Boolean): Float {
    val color = Color(argb, hasAlpha)
    val hsb = FloatArray(3)
    Color.RGBtoHSB(color.red, color.green, color.blue, hsb)
    return hsb[2]
  }

  /**
   * Try to guess whether the image has light background.
   *
   * The background is defined as a large fraction of pixels with the same color.
   */
  private fun hasLightBackground(colors: IntArray, hasAlpha: Boolean, brightnessThreshold: Double): Boolean? {
    val dominantColorPair = colors.groupBy { it }.maxByOrNull { it.value.size } ?: return null
    val dominantColor = dominantColorPair.key
    val dominantPixels = dominantColorPair.value

    return dominantPixels.size.toDouble() / colors.size > 0.5 && getBrightness(dominantColor, hasAlpha) > brightnessThreshold
  }

  fun invert(color: Color): Color {
    val alpha = invert(color.rgb)
    val argb = convertHSLtoRGB(hsl, alpha)
    return Color(argb, true)
  }

  fun invert(image: BufferedImage): BufferedImage =
    createImageWithInvertedPalette(image).also { outputImage ->
      invertInPlace(image, outputImage)
    }

  fun invert(content: ByteArray): ByteArray {
    val image = ImageIO.read(ByteArrayInputStream(content)) ?: return content
    val outputImage = createImageWithInvertedPalette(image)
    invertInPlace(image, outputImage)
    return ByteArrayOutputStream().use { outputStream ->
      ImageIO.write(outputImage, "png", outputStream)
      outputStream.flush()
      outputStream.toByteArray()
    }
  }

  private fun invertInPlace(image: BufferedImage, outputImage: BufferedImage) {
    val rgbArray = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    if (rgbArray.isEmpty()) return
    // Usually graph data contains regions with same color. Previous converted color may be reused.
    var prevArgb = rgbArray[0]
    var prevConverted = convertHSLtoRGB(hsl, invert(prevArgb))
    for (i in rgbArray.indices) {
      val argb = rgbArray[i]
      if (argb != prevArgb) {
        prevArgb = argb
        prevConverted = convertHSLtoRGB(hsl, invert(argb))
      }
      rgbArray[i] = prevConverted
    }
    outputImage.setRGB(0, 0, image.width, image.height, rgbArray, 0, image.width)
  }

  private fun createImageWithInvertedPalette(image: BufferedImage): BufferedImage {
    val model = image.colorModel
    if (model !is IndexColorModel) {
      return image
    }
    val palette = IntArray(model.mapSize)
    model.getRGBs(palette)
    for ((index, argb) in palette.withIndex()) {
      val alpha = invert(argb)
      palette[index] = convertHSLtoRGB(hsl, alpha)
    }

    // UIUtil.createImage() scales the image for HiDPI. It's undesired in this particular case.
    @Suppress("UndesirableClassUsage")
    return BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_INDEXED)
  }

  // Note: returns alpha, resulting color resides in `hsl`
  private fun invert(argb: Int): Float {
    val alpha = ((argb shr 24) and 255) / 255f
    rgb[R] = ((argb shr 16) and 255) / 255f
    rgb[G] = ((argb shr 8) and 255) / 255f
    rgb[B] = ((argb) and 255) / 255f
    convertRGBtoHSL(rgb, hsl)
    hsl[SATURATION] = hsl[SATURATION] * (50.0f + whiteHsl[SATURATION]) / 1.5f / 100f
    hsl[LUMINANCE] = (100 - hsl[LUMINANCE]) * (whiteHsl[LUMINANCE] - blackHsl[LUMINANCE]) / 100f + blackHsl[LUMINANCE]
    return alpha
  }

  companion object {
    private const val SATURATION = 1
    private const val LUMINANCE = 2
    private const val R = 0
    private const val G = 1
    private const val B = 2
  }
}


/**
 * The original source code was posted here: https://tips4java.wordpress.com/2009/07/05/hsl-color/
 *
 * According to https://tips4java.wordpress.com/about/
 * "We assume no responsibility for the code. You are free to use and/or modify and/or distribute any or all code posted
 *  on the Java Tips Weblog without restriction. A credit in the code comments would be nice, but not in any way mandatory."
 */

/**
 * Convert RGB floats to HSL floats.
 *
 * @param rgb input rgb floats from 0 to 1.
 * @param result output hsl floats, Hue is from 0 to 360, s and l are from 0 to 100.
 */
private fun convertRGBtoHSL(rgb: FloatArray, result: FloatArray) { //  Get RGB values in the range 0 - 1
  val r = rgb[0]
  val g = rgb[1]
  val b = rgb[2]
  //	Minimum and Maximum RGB values are used in the HSL calculations
  val min = min(r, min(g, b))
  val max = max(r, max(g, b))
  //  Calculate the Hue
  var h = 0f
  if (max == min) h = 0f else if (max == r) h = (60 * (g - b) / (max - min) + 360) % 360 else if (max == g) h = 60 * (b - r) / (max - min) + 120 else if (max == b) h = 60 * (r - g) / (max - min) + 240
  //  Calculate the Luminance
  val l = (max + min) / 2
  //  Calculate the Saturation
  var s = 0f
  s = if (max == min) 0f else if (l <= .5f) (max - min) / (max + min) else (max - min) / (2 - max - min)
  result[0] = h
  result[1] = s * 100
  result[2] = l * 100
}


/**
 * Convert HSL values to an RGB value.
 *
 * @param hslFloats  hsl floats, Hue is from 0 to 360, s and l are from 0 to 100.
 * @param alpha  the alpha value between 0 - 1
 *
 * @returns the integer RGB value
 */
private fun convertHSLtoRGB(hslFloats: FloatArray, alpha: Float): Int {
  var h = hslFloats[0]
  var s = hslFloats[1]
  var l = hslFloats[2]
  if (s < 0.0f || s > 100.0f) {
    val message = "Color parameter outside of expected range - Saturation"
    throw IllegalArgumentException(message)
  }
  if (l < 0.0f || l > 100.0f) {
    val message = "Color parameter outside of expected range - Luminance"
    throw IllegalArgumentException(message)
  }
  if (alpha < 0.0f || alpha > 1.0f) {
    val message = "Color parameter outside of expected range - Alpha"
    throw IllegalArgumentException(message)
  }
  //  Formula needs all values between 0 - 1.
  h %= 360.0f
  h /= 360f
  s /= 100f
  l /= 100f
  var q = 0f
  q = if (l < 0.5) l * (1 + s) else l + s - s * l
  val p = 2 * l - q
  var r = max(0f, hueToRGB(p, q, h + 1.0f / 3.0f))
  var g = max(0f, hueToRGB(p, q, h))
  var b = max(0f, hueToRGB(p, q, h - 1.0f / 3.0f))
  r = min(r, 1.0f)
  g = min(g, 1.0f)
  b = min(b, 1.0f)
  return ((alpha * 255).toInt() shl 24) + ((r * 255).toInt() shl 16) + ((g * 255).toInt() shl 8) + (b * 255).toInt()
}

private fun hueToRGB(p: Float, q: Float, h: Float): Float {
  var h = h
  if (h < 0) h += 1f
  if (h > 1) h -= 1f
  if (6 * h < 1) {
    return p + (q - p) * 6 * h
  }
  if (2 * h < 1) {
    return q
  }
  return if (3 * h < 2) {
    p + (q - p) * 6 * (2.0f / 3.0f - h)
  }
  else p
}
