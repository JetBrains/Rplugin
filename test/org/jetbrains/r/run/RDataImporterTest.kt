/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run

import junit.framework.TestCase
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.blockingGetAndDispatchEvents
import org.jetbrains.r.interpreter.LocalOrRemotePath
import org.jetbrains.r.packages.RequiredPackage
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.rinterop.RReference
import org.jetbrains.r.run.visualize.*
import org.jetbrains.r.run.visualize.RImportDataDialog.Companion.toRBoolean
import java.nio.file.Paths
import kotlin.math.sin

class RDataImporterTest : RProcessHandlerBaseTestCase() {
  private lateinit var importer: RDataImporter

  override fun setUp() {
    super.setUp()
    addLibraries()
    importer = RDataImporter(rInterop)
  }

  override fun alwaysCreateNewInterop() = true

  fun testBase() {
    val expected = prepareExpectedIntDataFrame()
    val options = RImportBaseDataDialog.collectOptions()
    checkPreviewAndImport(CSV_FILE_NAME, options, expected)
  }

  fun testReadr() {
    checkPackage("readr")
    val expected = prepareExpectedDoubleDataFrame()
    val options = RImportCsvDataDialog.collectOptions()
    (options.additional as MutableMap)["showColumnTypes"] = false.toRBoolean()
    checkPreviewAndImport(CSV_FILE_NAME, options, expected)
  }

  fun testExcel() {
    checkPackage("readxl")
    val expected = prepareExpectedDoubleDataFrame()
    val options = RImportExcelDataDialog.collectOptions()
    checkPreviewAndImport(EXCEL_FILE_NAME, options, expected)
  }

  private fun checkPackage(name: String) {
    val requirements = listOf(RequiredPackage(name))
    val missing = RequiredPackageInstaller.getInstance(project).getMissingPackages(requirements)
    TestCase.assertTrue("Cannot run test without package '$name'", missing.isEmpty())
  }

  private fun checkPreviewAndImport(fileName: String, options: RImportOptions, expected: DataFrame) {
    val path = Paths.get(testDataPath, "datasets", fileName).toString()
    checkPreview(path, options, expected.head(PREVIEW_ROW_COUNT))
    checkImport(path, options, expected)
  }

  private fun checkPreview(path: String, options: RImportOptions, expected: DataFrame) {
    val (ref, errorCount) = importer.previewDataAsync(LocalOrRemotePath(path, false), PREVIEW_ROW_COUNT, options).wait()
    TestCase.assertEquals(0, errorCount)
    checkEqual(ref, expected)
  }

  private fun checkImport(path: String, options: RImportOptions, expected: DataFrame) {
    val ref = importer.importData("dataset", LocalOrRemotePath(path, false), options)
    checkEqual(ref, expected)
  }

  private fun checkEqual(ref: RReference, expected: DataFrame) {
    val viewer = prepareViewer(ref)
    checkEqual(viewer, expected)
  }

  private fun checkEqual(viewer: RDataFrameViewer, expected: DataFrame) {
    val rowCount = expected.columns[0].values.size
    val columnCount = expected.columns.size
    TestCase.assertEquals(columnCount, viewer.nColumns - 1)
    TestCase.assertEquals(rowCount, viewer.nRows)
    for (i in 0 until columnCount) {
      TestCase.assertEquals(expected.columns[i].name, viewer.getColumnName(i + 1))
    }
    for (row in 0 until rowCount) {
      for (column in 0 until columnCount) {
        checkEqual(expected.columns[column].values[row], viewer.getValueAt(row, column + 1))
      }
    }
  }

  private fun checkEqual(expected: Any?, actual: Any?) {
    if (expected is Double && actual is Double) {
      TestCase.assertEquals(expected, actual, DELTA)
    } else {
      TestCase.assertEquals(expected, actual)
    }
  }

  private fun prepareViewer(ref: RReference): RDataFrameViewer {
    return rInterop.dataFrameGetViewer(ref).wait()
  }

  private data class Column(val name: String, val values: List<Any>)

  private data class DataFrame(val columns: List<Column>) {
    fun head(number: Int): DataFrame {
      val columnHeads = columns.map { it.copy(values = it.values.take(number)) }
      return DataFrame(columnHeads)
    }
  }

  companion object {
    private const val DELTA = 1e-6
    private const val TIMEOUT = 5000
    private const val PREVIEW_ROW_COUNT = 50
    private const val CSV_FILE_NAME = "sins.csv"
    private const val EXCEL_FILE_NAME = "sins.xlsx"

    private fun prepareExpectedIntDataFrame(): DataFrame {
      return prepareExpectedDataFrame { it }
    }

    private fun prepareExpectedDoubleDataFrame(): DataFrame {
      return prepareExpectedDataFrame { it.toDouble() }
    }

    private fun <R : Any>prepareExpectedDataFrame(xMapper: (Int) -> R): DataFrame {
      val xs = Column("xs", (1..100).map(xMapper))
      val ys = Column("ys", (1..100).map { sin(it / 10.0) })
      return DataFrame(listOf(xs, ys))
    }

    private fun <R> Promise<R>.wait(): R {
      val result = blockingGetAndDispatchEvents(TIMEOUT)
      TestCase.assertNotNull(result)
      return result!!
    }
  }
}
