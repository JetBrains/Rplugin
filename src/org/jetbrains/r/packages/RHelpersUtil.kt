/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import com.intellij.openapi.application.PathManager
import java.io.File
import java.nio.file.Paths

object RHelpersUtil {
  fun findFileInRHelpers(relativePath: String): File =
    Paths.get(helpersPath, relativePath).toFile()

  val helpersPath = Paths.get(PathManager.getPluginsPath(), "rplugin").toString()
}
