/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.project.Project
import org.intellij.datavis.r.inlays.components.GraphicsManager
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.run.graphics.RGraphicsRepository
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.RSnapshot
import java.awt.Dimension
import java.io.File

class ChunkGraphicsManager(private val project: Project) : GraphicsManager {
  override val isBusy: Boolean
    get() = project.chunkExecutionState != null

  override fun getImageResolution(imagePath: String): Int? {
    return imagePath.toSnapshot()?.resolution
  }

  override fun resizeImage(imagePath: String, newSize: Dimension, onResize: (File) -> Unit) {
    imagePath.toSnapshot()?.let { snapshot ->
      val resolution = RGraphicsUtils.getDefaultResolution(false)
      val newParameters = RGraphicsUtils.ScreenParameters(newSize, resolution)
      RGraphicsRepository.getInstance(project).rescale(snapshot, newParameters, onResize)
    }
  }

  companion object {
    private fun String.toSnapshot() = RSnapshot.from(File(this))
  }
}
