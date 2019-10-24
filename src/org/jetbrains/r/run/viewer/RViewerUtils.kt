/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.viewer

import java.io.File

object RViewerUtils {
  fun getQualifiedUrl(url: String): String {
    // If path begins with 'file:', it will be considered a relative one by File(path).
    // We should treat this case specially otherwise requests like 'file:///tmp/...' will fail.
    return if (!url.startsWith("file:")) {
      val file = File(url)
      file.toURI().toURL().toExternalForm()  // Will transform to 'file:<trimmed>'
    } else {
      url
    }
  }

  fun createViewerState(): RViewerState {
    // Note: 'FileUtil.createTempFile()' will break unit-tests
    val tmpFile = File.createTempFile("rplugin-viewer", ".txt")
    tmpFile.deleteOnExit()
    return RBasicViewerState(tmpFile)
  }
}