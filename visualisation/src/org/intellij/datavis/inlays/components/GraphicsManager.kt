/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.components

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import java.awt.Dimension
import java.io.File

interface GraphicsManager {
  fun isBusy(project: Project): Boolean
  fun resizeImage(project: Project, imagePath: String, newSize: Dimension, onResize: (File) -> Unit)

  companion object {
    private val EP = ExtensionPointName.create<GraphicsManager>("org.intellij.datavis.inlays.components.graphicsManager")

    fun getInstance(): GraphicsManager? {
      return EP.extensionList.firstOrNull()
    }
  }
}
