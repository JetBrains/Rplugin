/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.dataframe.aggregation

import org.intellij.datavis.inlays.dataframe.DataFrame
import org.intellij.datavis.inlays.dataframe.columns.*

/**
 * Product aggregator can be applied only to numeric columns.
  */
class Prod(columnName: String) : Aggregator(columnName) {

  /**
   * @return IntColumn with products for INT_ARRAY type column
   * @return DoubleColumn with products for DOUBLE_ARRAY type column
   * @return original column for any other type of columns
   */
  override fun process(dataFrame: DataFrame): Column<*> {
    val column = dataFrame[columnName]

    @Suppress("UNCHECKED_CAST")
    return when (column.type) {

      is IntArrayType -> {

        val result = ArrayList<Int>(column.size)

        for (i in 0 until column.size) {

          var res = 1
          for (value in (column[i] as ArrayList<Int>)) {
            res *= value
          }

          result += res
        }

        IntColumn(columnName, result)
      }

      is DoubleArrayType -> {
        val result = ArrayList<Double>(column.size)


        for (i in 0 until column.size) {

          var res = 1.0
          for (value in (column[i] as ArrayList<Double>)) {
            res *= value
          }

          result += res
        }

        DoubleColumn(columnName, result)
      }

      else -> column
    }
  }
}
