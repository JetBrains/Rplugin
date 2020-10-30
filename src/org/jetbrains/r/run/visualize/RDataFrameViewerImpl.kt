/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.concurrency.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.rinterop.*
import org.jetbrains.r.rinterop.DataFrameInfoResponse.ColumnType.*
import javax.swing.RowSorter
import kotlin.math.min
import kotlin.reflect.KClass

class RDataFrameViewerImpl(private val ref: RPersistentRef) : RDataFrameViewer {
  private val rInterop: RInterop = ref.rInterop
  override val nColumns: Int get() = columns.size
  override var nRows: Int = 0
  override val project get() = rInterop.project
  private lateinit var columns: Array<ColumnInfo>
  private lateinit var chunks: Array<Array<Array<Any?>>?>
  private lateinit var promises: Array<Promise<Unit>?>
  private var disposableParent: Disposable? = null
  private var virtualFile: RTableVirtualFile? = null
  override var canRefresh: Boolean = false

  private data class ColumnInfo(val name: String, val type: KClass<*>, val sortable: Boolean = true,
                                val isRowNames: Boolean = false,
                                val parseValue: (DataFrameGetDataResponse.Value) -> Any?)

  private var currentProxyDisposable: Disposable? = null

  init {
    Disposer.register(this, ref)
    val project = rInterop.project
    ensureDplyrInstalled(project)
    initInfo(rInterop.dataFrameGetInfo(ref).getWithCheckCanceled())
  }

  private fun initInfo(dataFrameInfo: DataFrameInfoResponse) {
    nRows = dataFrameInfo.nRows
    columns = dataFrameInfo.columnsList.map { col ->
      when (col.type) {
        INTEGER -> ColumnInfo(col.name, Int::class, col.sortable, col.isRowNames) { if (it.hasNa()) null else it.intValue }
        DOUBLE -> ColumnInfo(col.name, Double::class, col.sortable, col.isRowNames) { if (it.hasNa()) null else it.doubleValue }
        BOOLEAN -> ColumnInfo(col.name, Boolean::class, col.sortable, col.isRowNames) { if (it.hasNa()) null else it.booleanValue }
        else -> ColumnInfo(col.name, String::class, col.sortable, col.isRowNames) { if (it.hasNa()) null else it.stringValue }
      }
    }.toTypedArray()
    chunks = Array((nRows + CHUNK_SIZE - 1) / CHUNK_SIZE) { null }
    promises = Array(chunks.size) { null }
    canRefresh = dataFrameInfo.canRefresh
  }

  override fun getColumnName(index: Int) = columns[index].name

  override fun getColumnType(index: Int): KClass<*> {
    return columns[index].type
  }

  override fun isColumnSortable(index: Int) = columns[index].sortable

  override fun isRowNames(index: Int) = columns[index].isRowNames

  override fun getValueAt(row: Int, col: Int): Any? {
    ensureLoaded(row, col).blockingGet(Int.MAX_VALUE)
    val chunkIndex = row / CHUNK_SIZE
    return chunks.getOrNull(chunkIndex)?.getOrNull(col)?.getOrNull(row % CHUNK_SIZE)
  }

  override fun ensureLoaded(row: Int, col: Int, onLoadCallback: (() -> Unit)?): Promise<Unit> {
    val chunkIndex = row / CHUNK_SIZE
    if (chunks[chunkIndex] != null) return resolvedPromise()
    promises[chunkIndex]?.let {
      when (it.state) {
        Promise.State.PENDING -> return it
        Promise.State.SUCCEEDED -> return resolvedPromise()
        Promise.State.REJECTED -> Unit
      }
    }
    if (!rInterop.isAlive) {
      return rejectedPromise("RInterop is not alive")
    }
    val start = chunkIndex * CHUNK_SIZE
    val end = min((chunkIndex + 1) * CHUNK_SIZE, nRows)
    val promise: Promise<Unit> = rInterop.dataFrameGetData(ref, start, end)
      .also { Disposer.register(this, Disposable { it.cancel() }) }
      .then { response ->
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
    try {
      return RDataFrameViewerImpl(rInterop.dataFrameSort(ref, sortKeys)).also { newDataFrame ->
        disposableParent?.let { newDataFrame.registerDisposable(it, virtualFile) }
      }
    } catch (e: RInteropTerminated) {
      throw RDataFrameException(RBundle.message("rinterop.terminated"))
    }
  }

  override fun filter(f: DataFrameFilterRequest.Filter): RDataFrameViewer {
    try {
      return RDataFrameViewerImpl(rInterop.dataFrameFilter(ref, f)).also { newDataFrame ->
        disposableParent?.let { newDataFrame.registerDisposable(it, virtualFile) }
      }
    } catch (e: RInteropTerminated) {
      throw RDataFrameException(RBundle.message("rinterop.terminated"))
    }
  }

  override fun refresh(): Promise<Boolean> {
    if (!canRefresh) return resolvedPromise(false)
    return rInterop.dataFrameRefresh(ref).thenAsync {
      if (!it) return@thenAsync resolvedPromise(false)
      rInterop.dataFrameGetInfo(ref).thenAsync { info ->
        AsyncPromise<Boolean>().also { promise ->
          invokeLater {
            promise.compute {
              initInfo(info)
              true
            }
          }
        }
      }
    }
  }

  override fun registerDisposable(parent: Disposable, virtualFile: RTableVirtualFile?) {
    disposableParent = parent
    this.virtualFile = virtualFile
    currentProxyDisposable = object : Disposable {
      override fun dispose() {
        if (this == currentProxyDisposable &&
            virtualFile?.getUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN) != java.lang.Boolean.TRUE) {
          Disposer.dispose(this)
        }
      }
    }.also {
      Disposer.register(parent, it)
    }
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