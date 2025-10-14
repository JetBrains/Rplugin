/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.r.psi.interpreter.LocalOrRemotePath
import com.intellij.r.psi.interpreter.uploadFileToHost
import com.intellij.r.psi.rinterop.RReference
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.rinterop.RInteropImpl
import java.io.File

data class RImportOptions(val mode: String, val additional: Map<String, String>)

class RDataImporter(private val interop: RInteropImpl) {
  @Volatile
  private var lastLocalAndHostPath: Pair<String, String>? = null
  private val OUTPUT_NUMBER = "[1]"

  fun importData(name: String, path: LocalOrRemotePath, options: RImportOptions): RReference {
    val pathOnHost = getPathOnHost(path)
    interop.commitDataImport(name, pathOnHost, options.mode, options.additional)
    return refOf(name)
  }

  fun previewDataAsync(path: LocalOrRemotePath, rowCount: Int, options: RImportOptions): Promise<Pair<RReference, Int>> {
    return runAsync {
      val pathOnHost = getPathOnHost(path)
      val result = interop.previewDataImport(pathOnHost, options.mode, rowCount, options.additional)
      val errorCount = parseErrorCount(result.stdout, result.stderr)
      val ref = refOf(PREVIEW_DATA_VARIABLE_NAME)
      Pair(ref, errorCount)
    }
  }

  private fun getPathOnHost(path: LocalOrRemotePath): String {
    if (path.isRemote) return path.path
    lastLocalAndHostPath?.let {
      if (it.first == path.path) return it.second
    }
    return interop.interpreter.uploadFileToHost(File(path.path)).also {
      lastLocalAndHostPath = path.path to it
    }
  }

  private fun parseErrorCount(output: String, error: String): Int {
    if (output.isBlank()) {
      throw RuntimeException("Cannot get any output from interop\nStderr was: '$error'")
    }
    // Note: expected format
    //  - for failure: "NULL"
    //  - for success: "[1] errorCount\n"
    val errorCounterIndex = output.indexOf(OUTPUT_NUMBER)
    if (errorCounterIndex < 0 || output.length < errorCounterIndex + 4) {
      throw RuntimeException("Failed to preview data import.\nStdout was: '$output'\nStderr was: '$error'")
    }
    val errorCountMessage = output.substring(errorCounterIndex + OUTPUT_NUMBER.length)
    return errorCountMessage.trim().toInt()
  }

  private fun refOf(name: String): RReference {
    return RReference.expressionRef(name, interop)
  }

  companion object {
    const val PREVIEW_VARIABLE_NAME = ".jetbrains\$previewDataImportResult"
    private const val PREVIEW_DATA_VARIABLE_NAME = "$PREVIEW_VARIABLE_NAME\$data"
  }
}
