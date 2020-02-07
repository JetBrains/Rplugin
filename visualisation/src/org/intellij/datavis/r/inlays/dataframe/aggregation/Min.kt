/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.dataframe.aggregation

import org.intellij.datavis.r.inlays.dataframe.DataFrame
import org.intellij.datavis.r.inlays.dataframe.columns.*

class Min(columnName: String) : Aggregator(columnName) {
  override fun process(dataFrame: DataFrame): Column<*> {
    val column = dataFrame[columnName]

    @Suppress("UNCHECKED_CAST")
    return when (column.type) {
      is StringArrayType -> {
        val result = ArrayList<String?>(column.size)

        for (i in 0 until column.size) {
          result += (column[i] as ArrayList<String?>).min()
        }

        StringColumn(columnName, result)
      }

      is IntArrayType -> {

        val result = ArrayList<Int>(column.size)

        for (i in 0 until column.size) {
          result += (column[i] as ArrayList<Int>).min()!!
        }

        IntColumn(columnName, result)
      }

      is DoubleArrayType -> {
        val result = ArrayList<Double>(column.size)

        for (i in 0 until column.size) {
          result += (column[i] as ArrayList<Double>).min()!!
        }

        DoubleColumn(columnName, result)
      }
      else -> column
    }
  }
}