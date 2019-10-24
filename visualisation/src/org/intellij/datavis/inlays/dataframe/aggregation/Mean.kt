/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.dataframe.aggregation

import org.intellij.datavis.inlays.dataframe.DataFrame
import org.intellij.datavis.inlays.dataframe.columns.*

class Mean(columnName: String) : Aggregator(columnName) {

  override fun process(dataFrame: DataFrame): Column<*> {
    val column = dataFrame[columnName]

    @Suppress("UNCHECKED_CAST")
    return when (column.type) {
      is StringArrayType -> throw Exception("Cannot apply mean to string-typed column $columnName")

      is IntArrayType -> {

        val result = ArrayList<Double>(column.size)

        for (i in 0 until column.size) {
          result += (column[i] as ArrayList<Int>).sum() / (column[i] as ArrayList<Int>).size.toDouble()
        }

        DoubleColumn(columnName, result)
      }

      is DoubleArrayType -> {
        val result = ArrayList<Double>(column.size)

        for (i in 0 until column.size) {
          result += (column[i] as ArrayList<Double>).sum() / (column[i] as ArrayList<Double>).size
        }

        DoubleColumn(columnName, result)
      }
      else -> column
    }
  }
}