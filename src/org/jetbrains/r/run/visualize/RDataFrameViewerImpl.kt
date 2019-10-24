/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RPersistentRef
import org.jetbrains.r.rinterop.Service
import org.jetbrains.r.rinterop.Service.DataFrameInfoResponse.ColumnType.*
import javax.swing.RowSorter
import kotlin.math.min
import kotlin.reflect.KClass

class RDataFrameViewerImpl(private val ref: RPersistentRef) : RDataFrameViewer {
  private val rInterop: RInterop = ref.rInterop
  override val nColumns: Int get() = columns.size
  override val nRows: Int
  private val columns: Array<ColumnInfo>
  private val chunks: Array<Array<Array<Any?>>?>
  private val promises: Array<Promise<Unit>?>
  private var disposableParent: Disposable? = null

  private data class ColumnInfo(val name: String, val type: KClass<*>, val sortable: Boolean = true,
                                val parseValue: (Service.DataFrameGetDataResponse.Value) -> Any?)

  init {
    Disposer.register(this, ref)
    val project = rInterop.project
    ensureDplyrInstalled(project)
    val dataFrameInfo = rInterop.dataFrameGetInfo(ref)
    nRows = dataFrameInfo.nRows
    columns = dataFrameInfo.columnsList.map { column ->
      when (column.type) {
        INTEGER -> ColumnInfo(column.name, Int::class, column.sortable) { if (it.hasNa()) null else it.intValue }
        DOUBLE -> ColumnInfo(column.name, Double::class, column.sortable) { if (it.hasNa()) null else it.doubleValue }
        BOOLEAN -> ColumnInfo(column.name, Boolean::class, column.sortable) { if (it.hasNa()) null else it.booleanValue }
        else -> ColumnInfo(column.name, String::class, column.sortable) { if (it.hasNa()) null else it.stringValue }
      }
    }.toTypedArray()
    chunks = Array((nRows + CHUNK_SIZE - 1) / CHUNK_SIZE) { null }
    promises = Array(chunks.size) { null }
  }

  override fun getColumnName(index: Int) = columns[index].name

  override fun getColumnType(index: Int) = columns[index].type

  override fun isColumnSortable(index: Int) = columns[index].sortable

  override fun getValueAt(row: Int, col: Int): Any? {
    while (true) {
      val promise = ensureLoaded(row, col) ?: break
      promise.blockingGet(Int.MAX_VALUE)
      if (promise.isSucceeded) break
    }
    val chunkIndex = row / CHUNK_SIZE
    return chunks[chunkIndex]!![col][row % CHUNK_SIZE]
  }

  override fun ensureLoaded(row: Int, col: Int, onLoadCallback: (() -> Unit)?): Promise<Unit>? {
    val chunkIndex = row / CHUNK_SIZE
    if (chunks[chunkIndex] != null) return null
    promises[chunkIndex]?.let {
      when (it.state) {
        Promise.State.PENDING -> return it
        Promise.State.SUCCEEDED -> return null
        Promise.State.REJECTED -> Unit
      }
    }
    val start = chunkIndex * CHUNK_SIZE
    val end = min((chunkIndex + 1) * CHUNK_SIZE, nRows)
    val promise: Promise<Unit> = rInterop.dataFrameGetData(ref, start, end).then { response ->
      chunks[chunkIndex] = Array(nColumns) { col ->
        Array(end - start) { row ->
          columns[col].parseValue(response.getColumns(col).getValues(row))
        }
      }
      onLoadCallback?.invoke()
    }
    promises[chunkIndex] = promise
    return promise
  }

  override fun sortBy(sortKeys: List<RowSorter.SortKey>): RDataFrameViewer {
    return RDataFrameViewerImpl(rInterop.dataFrameSort(ref, sortKeys)).also { newDataFrame ->
      disposableParent?.let { newDataFrame.registerDisposable(it) }
    }
  }

  override fun filter(f: Service.DataFrameFilterRequest.Filter): RDataFrameViewer {
    return RDataFrameViewerImpl(rInterop.dataFrameFilter(ref, f)).also { newDataFrame ->
      disposableParent?.let { newDataFrame.registerDisposable(it) }
    }
  }

  override fun registerDisposable(parent: Disposable) {
    disposableParent = parent
    Disposer.register(parent, this)
  }

  companion object {
    private const val CHUNK_SIZE = 256

    fun ensureDplyrInstalled(project: Project) {
      val requiredPackages = listOf(RequiredPackage("dplyr"))
      if (RequiredPackageInstaller.getInstance(project).getMissingPackages(requiredPackages).isNotEmpty()) {
        throw RequiredPackageException(requiredPackages)
      }
    }
  }
}