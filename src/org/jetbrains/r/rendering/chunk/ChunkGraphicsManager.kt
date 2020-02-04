/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.intellij.datavis.r.inlays.components.GraphicsManager
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.run.graphics.RGraphicsRepository
import org.jetbrains.r.run.graphics.RGraphicsUtils
import org.jetbrains.r.run.graphics.RSnapshot
import org.jetbrains.r.settings.RGraphicsSettings
import org.jetbrains.r.settings.RMarkdownGraphicsSettings
import java.awt.Dimension
import java.io.File

class ChunkGraphicsManager(private val project: Project) : GraphicsManager {
  private val settings: RMarkdownGraphicsSettings
    get() = RMarkdownGraphicsSettings.getInstance(project)

  override val isBusy: Boolean
    get() = project.chunkExecutionState != null

  override var globalResolution: Int
    get() = settings.globalResolution
    set(newResolution) {
      settings.globalResolution = newResolution
    }

  override var isDarkModeEnabled: Boolean
    get() = RGraphicsSettings.isDarkModeEnabled(project)
    set(isEnabled) {
      RGraphicsSettings.setDarkMode(project, isEnabled)
    }

  override fun getImageResolution(imagePath: String): Int? {
    return imagePath.toSnapshot()?.resolution
  }

  override fun addGlobalResolutionListener(listener: (Int) -> Unit): Disposable {
    return settings.addGlobalResolutionListener(listener)
  }

  override fun rescaleImage(imagePath: String, newSize: Dimension, newResolution: Int?, onResize: (File) -> Unit) {
    imagePath.toSnapshot()?.let { snapshot ->
      val resolution = newResolution ?: snapshot.resolutionOrDefault
      val newParameters = RGraphicsUtils.ScreenParameters(newSize, resolution)
      RGraphicsRepository.getInstance(project).rescale(snapshot, newParameters, onResize)
    }
  }

  companion object {
    private val RSnapshot.resolutionOrDefault: Int
      get() = resolution ?: RGraphicsUtils.getDefaultResolution(false)  // NOT `settings.globalResolution`!

    private fun String.toSnapshot() = RSnapshot.from(File(this))
  }
}
