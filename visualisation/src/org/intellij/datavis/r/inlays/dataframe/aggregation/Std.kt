/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.dataframe.aggregation

import org.intellij.datavis.r.inlays.dataframe.DataFrame
import org.intellij.datavis.r.inlays.dataframe.columns.*
import java.lang.Math.sqrt

// https://en.wikipedia.org/wiki/Standard_deviation
// Se should not forget about problems with calculation of variance and std on large numbers.
// https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm
class Std(columnName: String) : Aggregator(columnName) {

  override fun process(dataFrame: DataFrame): Column<*> {
    val column = dataFrame[columnName]

    @Suppress("UNCHECKED_CAST")
    return when(column.type) {
      is StringArrayType -> throw Exception("Cannot apply mean to string-typed column $columnName")

      is IntArrayType -> {

        val result = ArrayList<Double>(column.size)

        for (i in 0 until column.size) {
          result += sqrt(Var.getVarianceInt(column[i] as ArrayList<Int>))
        }

        DoubleColumn(columnName, result)
      }

      is DoubleArrayType -> {
        val result = ArrayList<Double>(column.size)

        for (i in 0 until column.size) {
          result += sqrt(Var.getVarianceDouble(column[i] as ArrayList<Double>))
        }

        DoubleColumn(columnName, result)
      }
      else -> column
    }
  }
}