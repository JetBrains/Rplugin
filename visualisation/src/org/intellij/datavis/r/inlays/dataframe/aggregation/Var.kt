/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.dataframe.aggregation

import org.intellij.datavis.r.inlays.dataframe.DataFrame
import org.intellij.datavis.r.inlays.dataframe.columns.*

// https://en.wikipedia.org/wiki/Variance
// Se should not forget about problems with calculation of variance and std on large numbers.
// https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm
class Var(columnName: String) : Aggregator(columnName) {

  companion object {

    fun getVarianceDouble(array: ArrayList<Double>): Double {
      var average = 0.0
      for (p in array) {
        average += p
      }
      average /= array.size

      var variance = 0.0
      for (p in array) {
        variance += (p - average) * (p - average)
      }
      return variance / array.size
    }

    fun getVarianceInt(array: ArrayList<Int>): Double {
      var average = 0.0
      for (p in array) {
        average += p
      }
      average /= array.size

      var variance = 0.0
      for (p in array) {
        variance += (p - average) * (p - average)
      }
      return variance / array.size
    }
  }

  override fun process(dataFrame: DataFrame): Column<*> {
    val column = dataFrame[columnName]

    @Suppress("UNCHECKED_CAST")
    return when (column.type) {
      is StringArrayType -> throw Exception("Cannot apply var to string-typed column $columnName")

      is IntArrayType -> {

        val result = ArrayList<Double>(column.size)

        for (i in 0 until column.size) {
          result += Var.getVarianceInt(column[i] as ArrayList<Int>)
        }

        DoubleColumn(columnName, result)
      }

      is DoubleArrayType -> {
        val result = ArrayList<Double>(column.size)

        for (i in 0 until column.size) {
          result += Var.getVarianceDouble(column[i] as ArrayList<Double>)
        }

        DoubleColumn(columnName, result)
      }
      else -> column
    }
  }
}