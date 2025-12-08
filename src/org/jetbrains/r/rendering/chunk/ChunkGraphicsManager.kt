/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.r.psi.rendering.chunk.GraphicsManager
import org.jetbrains.r.rendering.editor.chunkExecutionState
import org.jetbrains.r.run.graphics.RGraphicsRepository
import com.intellij.r.psi.run.graphics.RGraphicsUtils
import com.intellij.r.psi.run.graphics.RSnapshot
import com.intellij.r.psi.settings.RGraphicsSettings
import org.jetbrains.r.settings.RMarkdownGraphicsSettings
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

class ChunkGraphicsManager(private val project: Project) : GraphicsManager {
  private val repository: RGraphicsRepository
    get() = RGraphicsRepository.getInstance(project)

  private val settings: RMarkdownGraphicsSettings
    get() = RMarkdownGraphicsSettings.getInstance(project)

  val isBusy: Boolean
    get() = project.chunkExecutionState != null

  var imageNumber: Int
    get() = RGraphicsSettings.getImageNumber(project)
    set(number) {
      RGraphicsSettings.setImageNumber(project, number)
    }

  var globalResolution: Int
    get() = settings.globalResolution
    set(newResolution) {
      settings.globalResolution = newResolution
    }

  var isStandalone: Boolean
    get() = RGraphicsSettings.isStandalone(project)
    set(newStandalone) {
      RGraphicsSettings.setStandalone(project, newStandalone)
    }

  var outputDirectory: String?
    get() = RGraphicsSettings.getOutputDirectory(project)
    set(directory) {
      RGraphicsSettings.setOutputDirectory(project, directory)
    }

  override var isDarkModeEnabled: Boolean
    get() = RGraphicsSettings.isDarkModeEnabled(project)
    set(isEnabled) {
      RGraphicsSettings.setDarkMode(project, isEnabled)
    }

  override fun isInvertible(image: Path): Boolean {
    return RSnapshot.from(image) != null
  }

  fun getImageResolution(imagePath: Path): Int? {
    return imagePath.toSnapshot()?.resolution
  }

  fun suggestImageName(imageNumber: Int? = null): String {
    val number = imageNumber ?: (this.imageNumber + 1)
    return "Rplot%02d".format(number)
  }

  fun createImageGroup(imagePath: Path): Pair<Path, Disposable>? {
    return imagePath.toSnapshot()?.let { snapshot ->
      val directory = createLocalGroupDirectory(snapshot)
      copyFileTo(snapshot.recordedFile, directory)
      val copy = copyFileTo(snapshot.file, directory)
      val groupPromise = repository.createDeviceGroupAsync(directory)
      val disposable = Disposable {
        directory.toFile().deleteRecursively()
        groupPromise.onSuccess {
          it.dispose()
        }
      }
      Pair(copy, disposable)
    }
  }

  fun addGlobalResolutionListener(parent: Disposable, listener: (Int) -> Unit) {
    settings.addGlobalResolutionListener(parent, listener)
  }

  fun addStandaloneListener(parent: Disposable, listener: (Boolean) -> Unit) {
    RGraphicsSettings.addStandaloneListener(project, parent, listener)
  }

  fun rescaleImage(imagePath: Path, newSize: Dimension, newResolution: Int? = null, onResize: (Path) -> Unit) {
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

    private fun Path.toSnapshot() = RSnapshot.from(this)

    private fun createLocalGroupDirectory(snapshot: RSnapshot): Path {
      val parentPath = snapshot.file.parent
      return Files.createTempDirectory(parentPath, "group").also { temp ->
        temp.toFile().deleteOnExit()
      }
    }

    private fun copyFileTo(file: Path, directory: Path): Path {
      val copyPath = directory.toAbsolutePath().resolve(file.name)
      Files.copy(file, copyPath)
      return copyPath
    }
  }
}
