/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import java.awt.Dimension
import java.io.File

interface GraphicsManager {
  val isBusy: Boolean
  var imageNumber: Int
  var globalResolution: Int
  var outputDirectory: String?
  var isDarkModeEnabled: Boolean
  fun canRescale(imagePath: String): Boolean
  fun getImageResolution(imagePath: String): Int?

  /**
   * Suggest a name for image which can be used to save it (without an extension).
   * **Example:** R Plugin uses "Rplot%02d" pattern for this purpose
   */
  fun suggestImageName(imageNumber: Int? = null): String

  /**
   * Create the group of isolated transformations of reference image.
   * @return pair of copied image file and an instance of [Disposable]
   * which should be disposed in order to delete group,
   * `null` if creation is not possible
   */
  fun createImageGroup(imagePath: String): Pair<File, Disposable>?

  /**
   * @return an instance of [Disposable] that should be disposed in order to remove the listener.
   * This is pretty similar to `Observable` API from RxJava
   */
  fun addGlobalResolutionListener(listener: (Int) -> Unit): Disposable

  /**
   * @param newResolution if `null`, current resolution won't be changed
   */
  fun rescaleImage(imagePath: String, newSize: Dimension, newResolution: Int? = null, onResize: (File) -> Unit)

  companion object {
    fun getInstance(project: Project): GraphicsManager? {
      return GraphicsManagerProvider.getInstance()?.getManager(project)
    }
  }
}
