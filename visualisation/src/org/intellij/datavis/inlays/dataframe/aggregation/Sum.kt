/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.dataframe.aggregation

import org.intellij.datavis.inlays.dataframe.DataFrame
import org.intellij.datavis.inlays.dataframe.columns.*

class Sum(columnName: String) : Aggregator(columnName) {

  companion object {
    val builder = StringBuilder()
  }

  override fun process(dataFrame: DataFrame): Column<*> {
    val column = dataFrame[columnName]

    @Suppress("UNCHECKED_CAST")
    return when (column.type) {
      is StringArrayType -> {
        val result = ArrayList<String?>(column.size)

        for (i in 0 until column.size) {

          val arr = column[i] as ArrayList<String>
          builder.setLength(0)
          for (str in arr) {
            builder.append(str).append(',')
          }

          result += builder.toString()
        }

        StringColumn(columnName, result)
      }

      is IntArrayType -> {

        val result = ArrayList<Int>(column.size)

        for (i in 0 until column.size) {
          result += (column[i] as ArrayList<Int>).sum()
        }

        IntColumn(columnName, result)
      }

      is DoubleArrayType -> {
        val result = ArrayList<Double>(column.size)

        for (i in 0 until column.size) {
          result += (column[i] as ArrayList<Double>).sum()
        }

        DoubleColumn(columnName, result)
      }
      else -> column
    }
  }
}