/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays.table

import org.jetbrains.r.visualization.inlays.dataframe.DataFrame
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.TableColumn

class DataFrameColumnModel(dataFrame: DataFrame) : DefaultTableColumnModel() {

    init {
        for (i in 0 until dataFrame.dim.width) {
            addColumn(i, dataFrame[i].name)
        }
    }

    private fun addColumn(i: Int, name: String) {
        val column = TableColumn(i)
        column.headerValue = name
        addColumn(column)
    }
}