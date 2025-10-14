/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.util.use
import junit.framework.TestCase
import kotlinx.coroutines.time.withTimeout
import com.intellij.r.psi.rinterop.RReference
import org.jetbrains.r.run.visualize.RDataFrameException
import org.jetbrains.r.run.visualize.RDataFrameViewer
import org.jetbrains.r.run.visualize.RFilterParser
import javax.swing.RowSorter
import javax.swing.SortOrder
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class RDataFrameViewerTest : RProcessHandlerBaseTestCase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testColumns() {
    createViewer("""dplyr::tibble(i = 5L, d = 3.3, ss = "aba")""").use { viewer ->
      TestCase.assertEquals(4, viewer.nColumns)
      TestCase.assertEquals("", viewer.getColumnName(0))
      TestCase.assertEquals("i", viewer.getColumnName(1))
      TestCase.assertEquals("d", viewer.getColumnName(2))
      TestCase.assertEquals("ss", viewer.getColumnName(3))
      TestCase.assertEquals(Integer::class, viewer.getColumnType(0))
      TestCase.assertEquals(Integer::class, viewer.getColumnType(1))
      TestCase.assertEquals(Double::class, viewer.getColumnType(2))
      TestCase.assertEquals(String::class, viewer.getColumnType(3))
      TestCase.assertTrue(viewer.isColumnSortable(0))
      TestCase.assertTrue(viewer.isColumnSortable(1))
      TestCase.assertTrue(viewer.isColumnSortable(2))
      TestCase.assertTrue(viewer.isColumnSortable(3))
    }
  }

  fun testData() {
    createViewer("""dplyr::tibble(x = 0L:999L, y = x * 2L)""").use { viewer ->
      for (i in 0 until 1000) {
        TestCase.assertEquals(i + 1, viewer.getValueAt(i, 0))
        TestCase.assertEquals(i, viewer.getValueAt(i, 1))
        TestCase.assertEquals(i * 2, viewer.getValueAt(i, 2))
      }
    }
  }

  fun testSort() {
    data class MyRow(val i: Int, val x: Int, val y: Int, val z: String)
    val data = List(50) { MyRow(it + 1, it / 7, it % 7, "a$it") }

    val viewer = createViewer("""dplyr::tibble(
      x = c(${data.joinToString(", ") { "${it.x}L" }}),
      y = c(${data.joinToString(", ") { "${it.y}L" }}),
      z = c(${data.joinToString(", ") { "\"${it.z}\"" }})
    )""".trimIndent()).use { viewer ->
      val sorted = viewer.sortBy(listOf(RowSorter.SortKey(2, SortOrder.ASCENDING), RowSorter.SortKey(1, SortOrder.DESCENDING)))
      TestCase.assertEquals(4, sorted.nColumns)
      TestCase.assertEquals("", sorted.getColumnName(0))
      TestCase.assertEquals("x", sorted.getColumnName(1))
      TestCase.assertEquals("y", sorted.getColumnName(2))
      TestCase.assertEquals("z", sorted.getColumnName(3))
      TestCase.assertEquals(Integer::class, sorted.getColumnType(0))
      TestCase.assertEquals(Integer::class, sorted.getColumnType(1))
      TestCase.assertEquals(Integer::class, sorted.getColumnType(2))
      TestCase.assertEquals(String::class, sorted.getColumnType(3))

      data
        .sortedByDescending { it.x }
        .sortedBy { it.y }
        .forEachIndexed { i, row ->
          TestCase.assertEquals(row.i, sorted.getValueAt(i, 0))
          TestCase.assertEquals(row.x, sorted.getValueAt(i, 1))
          TestCase.assertEquals(row.y, sorted.getValueAt(i, 2))
          TestCase.assertEquals(row.z, sorted.getValueAt(i, 3))
        }
    }
  }

  fun testFilterNumeric() {
    createViewer("""dplyr::tibble(
      | i = 0:7,
      | a = c(30, 20, 10, 3, 2, 1, -6, -3)
      |)""".trimMargin()).use { viewer ->
      val parser = RFilterParser(2)
      fun checkFilter(s: String, expected: List<Int>) {
        val filtered = viewer.filter(parser.parseText(s).proto)
        TestCase.assertEquals(expected, (0 until filtered.nRows).map { filtered.getValueAt(it, 1) })
      }
      checkFilter("= 20", listOf(1))
      checkFilter("! 20", listOf(0, 2, 3, 4, 5, 6, 7))
      checkFilter("< 20", listOf(2, 3, 4, 5, 6, 7))
      checkFilter("> 20", listOf(0))
      checkFilter("<= 20", listOf(1, 2, 3, 4, 5, 6, 7))
      checkFilter(">= 20", listOf(0, 1))
      checkFilter("<= -2", listOf(6, 7))
    }
  }

  fun testFilterString() {
    createViewer("""dplyr::tibble(
      | i = 0:3,
      | a = c("aacd", "abcd", "ddd", "xyz")
      |)""".trimMargin()).use { viewer ->
      val parser = RFilterParser(2)
      fun checkFilter(s: String, expected: List<Int>) {
        val filtered = viewer.filter(parser.parseText(s).proto)
        TestCase.assertEquals(expected, (0 until filtered.nRows).map { filtered.getValueAt(it, 1) })
      }
      checkFilter("= abcd", listOf(1))
      checkFilter("! abcd", listOf(0, 2, 3))
      checkFilter("< abcd", listOf(0))
      checkFilter("> abcd", listOf(2, 3))
      checkFilter("<= abcd", listOf(0, 1))
      checkFilter(">= abcd", listOf(1, 2, 3))

      checkFilter("d", listOf(0, 1, 2))
      checkFilter("a?c", listOf(0, 1))
      checkFilter("a*d", listOf(0, 1))

      checkFilter("~~ d+", listOf(2))
      checkFilter("~~ [a-d][b-d].*", listOf(1, 2))
      checkFilter("~ d*", listOf(2))
      checkFilter("~ ???", listOf(2, 3))
      checkFilter("!~ d*", listOf(0, 1, 3))
    }
  }

  fun testFilterNa() {
    createViewer("""dplyr::tibble(
      | i = 0:3,
      | a = c(1, 2, NA, NA)
      |)""".trimMargin()).use { viewer ->
      val parser = RFilterParser(2)
      fun checkFilter(s: String, expected: List<Int>) {
        val filtered = viewer.filter(parser.parseText(s).proto)
        TestCase.assertEquals(expected, (0 until filtered.nRows).map { filtered.getValueAt(it, 1) })
      }

      checkFilter(">= 1", listOf(0, 1))
      checkFilter("<= 2", listOf(0, 1))
      checkFilter("_", listOf(2, 3))
      checkFilter("!_", listOf(0, 1))
    }
  }

  fun testColumnNames() {
    createViewer("""{
      |  a = data.frame(xx = 1:3, yy = 3:5)
      |  names(a) <- NULL
      |  a
      |}
    """.trimMargin()).use { viewer ->
      TestCase.assertEquals("", viewer.getColumnName(0))
      TestCase.assertEquals("Column 1", viewer.getColumnName(1))
      TestCase.assertEquals("Column 2", viewer.getColumnName(2))
    }
  }

  fun testError() {
    rInterop.dataFrameGetViewer(RReference.expressionRef("NULL", rInterop)).onSuccess {
      TestCase.fail()
    }.onError {
      TestCase.assertTrue(it is RDataFrameException)
    }
  }

  fun testMatrix() {
    createViewer("array(as.integer((1:20) ^ 2), c(4, 5))").use { viewer ->
      TestCase.assertEquals(4, viewer.nRows)
      TestCase.assertEquals(6, viewer.nColumns)
      for (i in 1..5) {
        TestCase.assertTrue(i.toString() in viewer.getColumnName(i))
        TestCase.assertEquals(Integer::class, viewer.getColumnType(i))
      }
      for (i in 0 until 20) {
        TestCase.assertEquals((i + 1) * (i + 1), viewer.getValueAt(i % 4, i / 4 + 1))
      }
    }
  }

  fun testFactor() {
    createViewer("data.frame(as.factor(c('Xaa', 'cc', 'cc', 'Yaa', 'Xaa', 'zz')))").use { viewer ->
      TestCase.assertEquals(listOf("Xaa", "cc", "cc", "Yaa", "Xaa", "zz"), List(viewer.nRows) { viewer.getValueAt(it, 1) })

      val sorted = viewer.sortBy(listOf(RowSorter.SortKey(1, SortOrder.ASCENDING)))
      TestCase.assertEquals(setOf("Xaa", "Yaa", "cc", "zz"), List(sorted.nRows) { sorted.getValueAt(it, 1) }.toSet())

      val parser = RFilterParser(1)
      val filtered = viewer.filter(parser.parseText("aa").proto)
      TestCase.assertEquals(listOf("Xaa", "Yaa", "Xaa"), List(filtered.nRows) { filtered.getValueAt(it, 1) })
    }
  }

  fun testRefresh() {
    rInterop.executeCode("aaa <- dplyr::tibble(xx = (1:5)^2.0)")
    createViewer("aaa").use { viewer ->
      TestCase.assertTrue(viewer.canRefresh)

      TestCase.assertFalse(viewer.awaitRefresh())
      rInterop.executeCode("aaa <- dplyr::tibble(xx = (1:4)^3.0, yy = c('AA', 'BB', 'CC', 'DD'))")

      TestCase.assertTrue(viewer.canRefresh)
      TestCase.assertEquals(2, viewer.nColumns)
      TestCase.assertEquals(5, viewer.nRows)
      TestCase.assertEquals("xx", viewer.getColumnName(1))
      TestCase.assertEquals(listOf(1.0, 4.0, 9.0, 16.0, 25.0), List(5) { viewer.getValueAt(it, 1) })

      TestCase.assertTrue(viewer.awaitRefresh())

      TestCase.assertTrue(viewer.canRefresh)
      TestCase.assertEquals(3, viewer.nColumns)
      TestCase.assertEquals(4, viewer.nRows)
      TestCase.assertEquals("xx", viewer.getColumnName(1))
      TestCase.assertEquals("yy", viewer.getColumnName(2))
      TestCase.assertEquals(listOf(1.0, 8.0, 27.0, 64.0), List(4) { viewer.getValueAt(it, 1) })
      TestCase.assertEquals(listOf("AA", "BB", "CC", "DD"), List(4) { viewer.getValueAt(it, 2) })
    }
  }

  fun testRefreshDataTable() {
    rInterop.executeCode("aaa <- data.table::data.table(a = c(10.0, 20.0, 40.0), b = c(2.0, 4.0, 6.0))")
    createViewer("aaa").use { viewer ->
      TestCase.assertFalse(viewer.awaitRefresh())

      rInterop.executeCode("aaa[, z := a + b]")
      TestCase.assertTrue(viewer.awaitRefresh())
      TestCase.assertEquals(4, viewer.nColumns)
      TestCase.assertEquals(3, viewer.nRows)
      TestCase.assertEquals("a", viewer.getColumnName(1))
      TestCase.assertEquals("b", viewer.getColumnName(2))
      TestCase.assertEquals("z", viewer.getColumnName(3))
      TestCase.assertEquals(listOf(12.0, 24.0, 46.0), List(3) { viewer.getValueAt(it, 3) })
      TestCase.assertFalse(viewer.awaitRefresh())
    }
  }

  private fun RDataFrameViewer.awaitRefresh(): Boolean {
    val viewer = this
    return runBlockingCancellable {
      withTimeout(DEFAULT_TIMEOUT.milliseconds.toJavaDuration()) {
        viewer.refresh()
      }
    }
  }


  private fun createViewer(expr: String): RDataFrameViewer {
    return rInterop.dataFrameGetViewer(RReference.expressionRef(expr, rInterop)).blockingGet(DEFAULT_TIMEOUT)!!
      .also { it.registerDisposable(rInterop, null) }
  }
}