/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import com.intellij.openapi.project.Project
import org.intellij.datavis.r.inlays.components.GraphicsManagerProvider

class ChunkGraphicsManagerProvider : GraphicsManagerProvider {
  override fun getManager(project: Project) = ChunkGraphicsManager(project)
}
