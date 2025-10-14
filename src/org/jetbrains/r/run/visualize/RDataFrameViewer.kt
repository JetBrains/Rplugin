/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.r.psi.rinterop.DataFrameFilterRequest
import org.jetbrains.concurrency.Promise
import javax.swing.RowSorter
import kotlin.reflect.KClass

internal interface RDataFrameViewer : Disposable {
  val nColumns: Int

  val nRows: Int
  
  val project: Project

  val canRefresh: Boolean

  fun getColumnName(index: Int): String

  fun getColumnType(index: Int): KClass<*>

  fun isColumnSortable(index: Int): Boolean

  fun isRowNames(index: Int): Boolean

  fun getValueAt(row: Int, col: Int): Any?

  fun ensureLoaded(row: Int, col: Int, onLoadCallback: (() -> Unit)? = null): Promise<Unit>

  fun sortBy(sortKeys: List<RowSorter.SortKey>): RDataFrameViewer

  fun filter(f: DataFrameFilterRequest.Filter): RDataFrameViewer

  suspend fun refresh(): Boolean

  override fun dispose() {
  }

  fun registerDisposable(parent: Disposable, virtualFile: RTableVirtualFile?) {
  }
}
