/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
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
import java.nio.file.Files
import java.nio.file.Paths

class ChunkGraphicsManager(private val project: Project) : GraphicsManager {
  private val repository: RGraphicsRepository
    get() = RGraphicsRepository.getInstance(project)

  private val settings: RMarkdownGraphicsSettings
    get() = RMarkdownGraphicsSettings.getInstance(project)

  override val isBusy: Boolean
    get() = project.chunkExecutionState != null

  override var imageNumber: Int
    get() = RGraphicsSettings.getImageNumber(project)
    set(number) {
      RGraphicsSettings.setImageNumber(project, number)
    }

  override var globalResolution: Int
    get() = settings.globalResolution
    set(newResolution) {
      settings.globalResolution = newResolution
    }

  override var outputDirectory: String?
    get() = RGraphicsSettings.getOutputDirectory(project)
    set(directory) {
      RGraphicsSettings.setOutputDirectory(project, directory)
    }

  override var isDarkModeEnabled: Boolean
    get() = RGraphicsSettings.isDarkModeEnabled(project)
    set(isEnabled) {
      RGraphicsSettings.setDarkMode(project, isEnabled)
    }

  override fun canRescale(imagePath: String): Boolean {
    return imagePath.toSnapshot()?.recordedFile?.takeIf { it.exists() } != null
  }

  override fun getImageResolution(imagePath: String): Int? {
    return imagePath.toSnapshot()?.resolution
  }

  override fun suggestImageName(imageNumber: Int?): String {
    val number = imageNumber ?: (this.imageNumber + 1)
    return "Rplot%02d".format(number)
  }

  override fun createImageGroup(imagePath: String): Pair<File, Disposable>? {
    return imagePath.toSnapshot()?.let { snapshot ->
      val directory = createLocalGroupDirectory(snapshot)
      copyFileTo(snapshot.recordedFile, directory)
      val copy = copyFileTo(snapshot.file, directory)
      val groupPromise = repository.createDeviceGroupAsync(directory)
      val disposable = Disposable {
        directory.deleteRecursively()
        groupPromise.onSuccess {
          it.dispose()
        }
      }
      Pair(copy, disposable)
    }
  }

  override fun addGlobalResolutionListener(listener: (Int) -> Unit): Disposable {
    return settings.addGlobalResolutionListener(listener)
  }

  override fun rescaleImage(imagePath: String, newSize: Dimension, newResolution: Int?, onResize: (File) -> Unit) {
    imagePath.toSnapshot()?.let { snapshot ->
      val resolution = newResolution ?: snapshot.resolutionOrDefault
      val newParameters = RGraphicsUtils.ScreenParameters(newSize, resolution)
      repository.rescaleStoredAsync(snapshot, newParameters).onSuccess { rescaled ->
        if (rescaled != null) {
          onResize(rescaled.file)
        }
      }
    }
  }

  companion object {
    private val defaultResolution: Int?
      get() = if (!ApplicationManager.getApplication().isUnitTestMode) RGraphicsUtils.getDefaultResolution(false) else null

    private val RSnapshot.resolutionOrDefault: Int?
      get() = resolution ?: defaultResolution  // NOT `settings.globalResolution`!

    private fun String.toSnapshot() = RSnapshot.from(File(this))

    private fun createLocalGroupDirectory(snapshot: RSnapshot): File {
      val parentPath = snapshot.file.parentFile.toPath()
      return Files.createTempDirectory(parentPath, "group").toFile().also { temp ->
        temp.deleteOnExit()
      }
    }

    private fun copyFileTo(file: File, directory: File): File {
      val copyPath = Paths.get(directory.absolutePath, file.name)
      Files.copy(file.toPath(), copyPath)
      return copyPath.toFile()
    }
  }
}
