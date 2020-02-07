/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.dataframe.aggregation

import org.intellij.datavis.r.inlays.dataframe.DataFrame
import org.intellij.datavis.r.inlays.dataframe.columns.Column
import org.intellij.datavis.r.inlays.dataframe.columns.IntColumn

class Count(columnName: String) : Aggregator(columnName) {
  override fun process(dataFrame: DataFrame): Column<*> {
    val column = dataFrame[columnName]

    val result = ArrayList<Int>(column.size)

    if (column.type.isArray()) {

      for (i in 0 until column.size) {
        result += (column[i] as ArrayList<out Any>).size
      }

    }
    else {
      for (i in 0 until column.size) {
        result += 1
      }
    }

    return IntColumn(columnName, result)
  }
}
