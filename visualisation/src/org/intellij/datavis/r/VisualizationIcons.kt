/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r

import com.intellij.openapi.util.IconLoader

object VisualizationIcons {

  // Chart.
  val BAR_CHART = getIcon("/icons/chart/chartBar.svg")
  val LINE_CHART = getIcon("/icons/chart/chartLine.svg")
  val SCATTER_CHART = getIcon("/icons/chart/chartScatter.svg")
  val PIE_CHART = getIcon("/icons/chart/chartPie.svg")
  val AREA_CHART = getIcon("/icons/chart/chartAreaRange.svg")
  val STOCK_CHART = getIcon("/icons/chart/chartStock.svg")
  val BUBBLE_CHART = getIcon("/icons/chart/chartBubble.svg")

  // Table.
  val TABLE_PAGINATION = getIcon("/icons/table/pagination.svg")

  // Graphics.
  val CONSTRAIN_IMAGE_PROPORTIONS = getIcon("/icons/graphics/constraintProportions.svg")

  private fun getIcon(path: String) = IconLoader.getIcon(path, VisualizationIcons::class.java)
}
