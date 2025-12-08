// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics

import com.intellij.r.psi.run.graphics.RGraphicsUtils
import org.jetbrains.r.rinterop.RInteropImpl
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path

object RGraphicsUtils {
  fun createGraphicsDevice(rInterop: RInteropImpl, screenDimension: Dimension?, resolution: Int?): RGraphicsDevice {
    val tmpDirectory = createTempDeviceDirectory()
    val parameters = RGraphicsUtils.createParameters(screenDimension, resolution)
    return RGraphicsDevice(rInterop, tmpDirectory, parameters, true)
  }

  private fun createTempDeviceDirectory(): Path {
    // Note: 'FileUtil.createTempFile()' will break unit-tests
    return Files.createTempDirectory("rplugin-graphics").apply {
      toFile().deleteOnExit()
    }
  }
}