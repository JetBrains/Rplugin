/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

object RImportDataUtil {
  val supportedTextFormats = arrayOf("txt", "csv", "tsv")
  val suggestedTextFormats = arrayOf("csv", "tsv")  // Suggest importing these formats via editor notifications
  val supportedExcelFormats = arrayOf("xls", "xlsx")
}
