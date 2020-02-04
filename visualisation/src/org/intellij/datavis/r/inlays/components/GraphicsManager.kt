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
  var globalResolution: Int
  var isDarkModeEnabled: Boolean
  fun getImageResolution(imagePath: String): Int?

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
