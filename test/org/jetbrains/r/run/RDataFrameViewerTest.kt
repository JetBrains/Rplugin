/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import junit.framework.TestCase
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.run.visualize.RDataFrameViewer
import org.jetbrains.r.run.visualize.RFilterParser
import javax.swing.RowSorter
import javax.swing.SortOrder

class RDataFrameViewerTest : RProcessHandlerBaseTestCase() {
  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testColumns() {
    val viewer = createViewer("""dplyr::tibble(i = 5L, d = 3.3, ss = "aba")""")
    TestCase.assertEquals(3, viewer.nColumns)
    TestCase.assertEquals("i", viewer.getColumnName(0))
    TestCase.assertEquals("d", viewer.getColumnName(1))
    TestCase.assertEquals("ss", viewer.getColumnName(2))
    TestCase.assertEquals(Integer::class, viewer.getColumnType(0))
    TestCase.assertEquals(Double::class, viewer.getColumnType(1))
    TestCase.assertEquals(String::class, viewer.getColumnType(2))
    TestCase.assertTrue(viewer.isColumnSortable(0))
    TestCase.assertTrue(viewer.isColumnSortable(1))
    TestCase.assertTrue(viewer.isColumnSortable(2))
  }

  fun testData() {
    val viewer = createViewer("""dplyr::tibble(x = 1L:1000L, y = x * 2L)""")
    for (i in 0 until 1000) {
      TestCase.assertEquals(i + 1, viewer.getValueAt(i, 0))
      TestCase.assertEquals((i + 1) * 2, viewer.getValueAt(i, 1))
    }
  }

  fun testSort() {
    data class MyRow(val x: Int, val y: Int, val z: String)
    val data = List(50) { MyRow(it / 7, it % 7, "a$it") }

    val viewer = createViewer("""dplyr::tibble(
      x = c(${data.joinToString(", ") { "${it.x}L" }}),
      y = c(${data.joinToString(", ") { "${it.y}L" }}),
      z = c(${data.joinToString(", ") { "\"${it.z}\"" }})
    )""".trimIndent())
    val sorted = viewer.sortBy(listOf(RowSorter.SortKey(1, SortOrder.ASCENDING), RowSorter.SortKey(0, SortOrder.DESCENDING)))
    TestCase.assertEquals(3, sorted.nColumns)
    TestCase.assertEquals("x", sorted.getColumnName(0))
    TestCase.assertEquals("y", sorted.getColumnName(1))
    TestCase.assertEquals("z", sorted.getColumnName(2))
    TestCase.assertEquals(Integer::class, sorted.getColumnType(0))
    TestCase.assertEquals(Integer::class, sorted.getColumnType(1))
    TestCase.assertEquals(String::class, sorted.getColumnType(2))

    data
      .sortedByDescending { it.x }
      .sortedBy { it.y }
      .forEachIndexed { i, row ->
        TestCase.assertEquals(row.x, sorted.getValueAt(i, 0))
        TestCase.assertEquals(row.y, sorted.getValueAt(i, 1))
        TestCase.assertEquals(row.z, sorted.getValueAt(i, 2))
      }
  }

  fun testFilterNumeric() {
    val viewer = createViewer("""dplyr::tibble(
      | i = 0:7,
      | a = c(30, 20, 10, 3, 2, 1, -6, -3)
      |)""".trimMargin())
    val parser = RFilterParser(1)
    fun checkFilter(s: String, expected: List<Int>) {
      val filtered = viewer.filter(parser.parseText(s).proto)
      TestCase.assertEquals(expected, (0 until filtered.nRows).map { filtered.getValueAt(it, 0) })
    }
    checkFilter("= 20", listOf(1))
    checkFilter("! 20", listOf(0, 2, 3, 4, 5, 6, 7))
    checkFilter("< 20", listOf(2, 3, 4, 5, 6, 7))
    checkFilter("> 20", listOf(0))
    checkFilter("<= 20", listOf(1, 2, 3, 4, 5, 6, 7))
    checkFilter(">= 20", listOf(0, 1))
    checkFilter("<= -2", listOf(6, 7))
  }

  fun testFilterString() {
    val viewer = createViewer("""dplyr::tibble(
      | i = 0:3,
      | a = c("aacd", "abcd", "ddd", "xyz")
      |)""".trimMargin())
    val parser = RFilterParser(1)
    fun checkFilter(s: String, expected: List<Int>) {
      val filtered = viewer.filter(parser.parseText(s).proto)
      TestCase.assertEquals(expected, (0 until filtered.nRows).map { filtered.getValueAt(it, 0) })
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

  fun testFilterNa() {
    val viewer = createViewer("""dplyr::tibble(
      | i = 0:3,
      | a = c(1, 2, NA, NA)
      |)""".trimMargin())
    val parser = RFilterParser(1)
    fun checkFilter(s: String, expected: List<Int>) {
      val filtered = viewer.filter(parser.parseText(s).proto)
      TestCase.assertEquals(expected, (0 until filtered.nRows).map { filtered.getValueAt(it, 0) })
    }

    checkFilter(">= 1", listOf(0, 1))
    checkFilter("<= 2", listOf(0, 1))
    checkFilter("_", listOf(2, 3))
    checkFilter("!_", listOf(0, 1))
  }

  private fun createViewer(expr: String): RDataFrameViewer {
    return rInterop.dataFrameGetViewer(RRef.expressionRef(expr, rInterop))
  }
}