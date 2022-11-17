/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import org.jetbrains.r.RBundle
import org.jetbrains.r.rinterop.DataFrameFilterRequest
import org.jetbrains.r.rinterop.getWithCheckCanceled
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import javax.swing.JTable
import javax.swing.RowSorter
import javax.swing.SortOrder

class RDataFrameRowSorter(private var model: RDataFrameTableModel, private val jTable: JTable) : RowSorter<RDataFrameTableModel>() {
  private var sortKeys: List<SortKey> = emptyList()
  private val initialViewer = model.viewer
  private var currentViewer = model.viewer
  var updatesSuspended = false
  var shownRange: Pair<Int, Int>? = null
    set(newRange) {
      if (field == newRange) return
      if (newRange == null) {
        field = null
        fireRowSorterChanged(null)
        return
      }
      val (start, end) = newRange
      if (start < 0 || end > modelRowCount || start > end) return
      field = newRange
      fireRowSorterChanged(null)
    }

  var rowFilter: DataFrameFilterRequest.Filter? = null
    set(value) {
      field = value
      update()
    }
  private var currentUpdateTask: Future<Unit>? = null
  private var errorWasReported = false

  override fun getModel() = model

  override fun getSortKeys() = sortKeys

  override fun getViewRowCount() = shownRange?.let { it.second - it.first } ?: model.rowCount

  override fun getModelRowCount() = model.rowCount

  override fun toggleSortOrder(columnIndex: Int) {
    if (!initialViewer.isColumnSortable(columnIndex)) return

    val newSortKeys = sortKeys.toMutableList()
    val sortIndex = newSortKeys.indexOfFirst { it.column == columnIndex }
    if (sortIndex == 0) {
      newSortKeys[0] = newSortKeys[0].toggle()
    } else {
      if (sortIndex != -1) {
        newSortKeys.removeAt(sortIndex)
      }
      newSortKeys.add(0, SortKey(columnIndex, SortOrder.ASCENDING))
    }
    while (newSortKeys.size > MAX_SORT_KEYS) {
      newSortKeys.removeAt(newSortKeys.size - 1)
    }
    setSortKeys(newSortKeys)
  }

  override fun convertRowIndexToModel(index: Int) = shownRange?.let { index + it.first } ?: index

  override fun convertRowIndexToView(index: Int) =
    shownRange?.let { if (index in it.first until it.second) index - it.first else -1 } ?: index

  override fun setSortKeys(newSortKeys: List<SortKey>?) {
    if (newSortKeys != sortKeys) {
      sortKeys = newSortKeys.orEmpty()
      fireSortOrderChanged()
      update()
    }
  }

  fun update() {
    if (updatesSuspended) return
    currentUpdateTask?.let {
      if (!it.isDone) it.cancel(true)
    }
    val toWait = currentUpdateTask
    val currentFilter = rowFilter
    val currentSortKeys = sortKeys
    currentUpdateTask = ApplicationManager.getApplication().executeOnPooledThread<Unit> {
      try {
        toWait?.getWithCheckCanceled()
        if (currentViewer != initialViewer) Disposer.dispose(currentViewer)
        currentViewer = initialViewer
        currentFilter?.let {
          currentViewer = currentViewer.filter(it)
        }
        if (currentSortKeys.isNotEmpty()) {
          val newViewer = currentViewer.sortBy(currentSortKeys)
          if (currentViewer != initialViewer) Disposer.dispose(currentViewer)
          currentViewer = newViewer
        }
        model.viewer = currentViewer
        jTable.clearSelection()
        model.fireTableDataChanged()
        fireRowSorterChanged(null)
      } catch (e: RDataFrameException) {
        if (!errorWasReported) {
          errorWasReported = true
          Notification("RDataFrameViewer", RBundle.message("data.frame.viewer.error.title"), e.message.orEmpty(), NotificationType.ERROR)
            .notify(initialViewer.project)
        }
      } catch (e: CancellationException) {
      } catch (e: InterruptedException) {
      }
    }
  }

  fun restore() {
    currentUpdateTask?.let {
      if (!it.isDone) it.cancel(true)
      currentUpdateTask = null
    }
    currentViewer = initialViewer
    model.viewer = initialViewer
    jTable.clearSelection()
    fireRowSorterChanged(null)
  }

  override fun rowsInserted(p0: Int, p1: Int) {}
  override fun rowsDeleted(p0: Int, p1: Int) {}
  override fun rowsUpdated(p0: Int, p1: Int) {}
  override fun rowsUpdated(p0: Int, p1: Int, p2: Int) {}
  override fun allRowsChanged() {}
  override fun modelStructureChanged() {}

  companion object {
    private const val MAX_SORT_KEYS = 3
  }
}

private fun RowSorter.SortKey.toggle(): RowSorter.SortKey {
  return if (this.sortOrder == SortOrder.ASCENDING) {
    RowSorter.SortKey(column, SortOrder.DESCENDING)
  } else {
    RowSorter.SortKey(column, SortOrder.ASCENDING)
  }
}
