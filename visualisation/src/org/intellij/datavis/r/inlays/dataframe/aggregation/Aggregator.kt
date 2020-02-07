/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.dataframe.aggregation

import org.intellij.datavis.r.inlays.dataframe.DataFrame
import org.intellij.datavis.r.inlays.dataframe.columns.Column

// List of aggregation functions from pandas
// count()	Total number of items
// first(), last()	First and last item
// mean(), median()	Mean and median
// min(), max()	Minimum and maximum
// std(), var()	Standard deviation and variance
// mad()	Mean absolute deviation
// prod()	Product of all items
// sum()	Sum of all items

abstract class Aggregator(val columnName: String) {
  abstract fun process(dataFrame: DataFrame): Column<*>
}