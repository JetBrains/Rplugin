/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RRef

data class RImportOptions(val mode: String, val additional: Map<String, String>)

class RDataImporter(private val interop: RInterop) {
  fun importData(name: String, path: String, options: RImportOptions): RRef {
    interop.commitDataImport(name, path, options.mode, options.additional)
    return refOf(name)
  }

  fun previewDataAsync(path: String, rowCount: Int, options: RImportOptions): Promise<Pair<RRef, Int>> {
    return runAsync {
      val result = interop.previewDataImport(path, options.mode, rowCount, options.additional)
      val errorCount = parseErrorCount(result.stdout, result.stderr)
      val ref = refOf(PREVIEW_DATA_VARIABLE_NAME)
      Pair(ref, errorCount)
    }
  }

  private fun parseErrorCount(output: String, error: String): Int {
    if (output.isBlank()) {
      throw RuntimeException("Cannot get any output from interop\nStderr was: '$error'")
    }
    // Note: expected format
    //  - for failure: "NULL"
    //  - for success: "[1] errorCount\n"
    if (output == "NULL" || output.length < 6 || !output.startsWith("[1]")) {
      throw RuntimeException("Failed to preview data import.\nStdout was: '$output'\nStderr was: '$error'")
    }
    return output.substring(4, output.length - 1).toInt()
  }

  private fun refOf(name: String): RRef {
    return RRef.expressionRef(name, interop)
  }

  companion object {
    const val PREVIEW_VARIABLE_NAME = ".jetbrains\$previewDataImportResult"
    private const val PREVIEW_DATA_VARIABLE_NAME = "$PREVIEW_VARIABLE_NAME\$data"
  }
}
