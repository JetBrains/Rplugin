/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.ui

import java.awt.Dimension
import java.awt.Toolkit

enum class RDimensionPreference {
  VERY_NARROW,
  NARROW,
  MODERATE,
  WIDE
}

private fun calculateRatioByPreference(dimensionPreference: RDimensionPreference): Double {
  return when (dimensionPreference) {
    RDimensionPreference.VERY_NARROW -> 0.15
    RDimensionPreference.NARROW -> 0.25
    RDimensionPreference.MODERATE -> 0.5
    RDimensionPreference.WIDE -> 0.75
  }
}

fun calculateDialogPreferredSize(sizePreference: RDimensionPreference): Dimension {
  return calculateDialogPreferredSize(sizePreference, sizePreference)
}

fun calculateDialogPreferredSize(widthPreference: RDimensionPreference, heightPreference: RDimensionPreference): Dimension {
  val widthRatio = calculateRatioByPreference(widthPreference)
  val heightRatio = calculateRatioByPreference(heightPreference)
  return calculateDialogPreferredSize(widthRatio, heightRatio)
}

fun calculateDialogPreferredSize(widthRatio: Double, heightRatio: Double): Dimension {
  fun checkRatio(ratio: Double, name: String) {
    if (ratio <= 0.0 || ratio > 1.0) {
      throw IllegalArgumentException("$name ratio must fall within the range (0.0; 1.0]. Actual value was $ratio")
    }
  }

  checkRatio(widthRatio, "Width")
  checkRatio(heightRatio, "Height")
  val screenSize = Toolkit.getDefaultToolkit().screenSize
  val width = (screenSize.width * widthRatio).toInt()
  val height = (screenSize.height * heightRatio).toInt()
  return Dimension(width, height)
}
