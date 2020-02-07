/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.dataframe.aggregation

import org.intellij.datavis.r.inlays.dataframe.DataFrame
import org.intellij.datavis.r.inlays.dataframe.columns.*
import java.lang.Math.abs

// https://en.wikipedia.org/wiki/Average_absolute_deviation#Median_absolute_deviation_around_the_mean
class Mad(columnName: String) : Aggregator(columnName) {

  private fun aggregateIntArray(column: Column<*>): DoubleColumn {

    val result = ArrayList<Double>(column.size)

    for (i in 0 until column.size) {
      @Suppress("UNCHECKED_CAST")
      val array = column[i] as ArrayList<Int>
      val mean = array.sum() / array.size.toDouble()

      var absoluteDeviationSum = 0.0
      for (value in array) {
        absoluteDeviationSum += abs(value - mean)
      }

      result += absoluteDeviationSum / array.size.toDouble()
    }

    return DoubleColumn(columnName, result)
  }

  private fun aggregateDoubleArray(column: Column<*>): DoubleColumn {
    val result = ArrayList<Double>(column.size)

    for (i in 0 until column.size) {
      @Suppress("UNCHECKED_CAST")
      val array = column[i] as ArrayList<Double>
      val mean = array.sum() / array.size.toDouble()

      var absoluteDeviationSum = 0.0
      for (value in array) {
        absoluteDeviationSum += abs(value - mean)
      }

      result += absoluteDeviationSum / array.size.toDouble()
    }

    return DoubleColumn(columnName, result)
  }

  @Suppress("UNCHECKED_CAST")
  override fun process(dataFrame: DataFrame): Column<*> {
    val column = dataFrame[columnName]

    return when (column.type) {
      is StringArrayType -> throw Exception("Cannot apply mean to string-typed column $columnName")
      is IntArrayType -> aggregateIntArray(column)
      is DoubleArrayType -> aggregateDoubleArray(column)
      else -> column
    }
  }
}