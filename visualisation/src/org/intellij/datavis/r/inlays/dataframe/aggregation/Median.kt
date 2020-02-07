/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.dataframe.aggregation

import org.intellij.datavis.r.inlays.dataframe.DataFrame
import org.intellij.datavis.r.inlays.dataframe.columns.Column
import org.intellij.datavis.r.inlays.dataframe.columns.DoubleArrayType
import org.intellij.datavis.r.inlays.dataframe.columns.DoubleColumn
import org.intellij.datavis.r.inlays.dataframe.columns.IntArrayType

// https://en.wikipedia.org/wiki/Median
class Median(columnName: String) : Aggregator(columnName) {

  override fun process(dataFrame: DataFrame): Column<*> {
    val column = dataFrame[columnName]

    @Suppress("UNCHECKED_CAST")
    return when (column.type) {

      is IntArrayType -> {

        val result = ArrayList<Double>(column.size)

        for (i in 0 until column.size) {

          val arr = column[i] as ArrayList<Int>
          arr.sort()
          val median: Double = if (arr.size % 2 == 0)
            (arr[arr.size / 2] + arr[arr.size / 2 - 1]) / 2.0
          else
            arr[arr.size / 2].toDouble()

          result += median
        }

        DoubleColumn(columnName, result)
      }

      is DoubleArrayType -> {
        val result = ArrayList<Double>(column.size)


        for (i in 0 until column.size) {
          val arr = column[i] as ArrayList<Double>
          arr.sort()
          val median: Double = if (arr.size % 2 == 0)
            (arr[arr.size / 2] + arr[arr.size / 2 - 1]) / 2.0
          else
            arr[arr.size / 2]

          result += median
        }

        DoubleColumn(columnName, result)
      }

      else -> column
    }
  }
}
