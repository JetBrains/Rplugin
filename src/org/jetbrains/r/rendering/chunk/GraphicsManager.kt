/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.chunk

import java.io.File

interface GraphicsManager {
  /**
   * Whether a [GraphicsPanel] should invert images for a dark editor
   */
  val isDarkModeEnabled: Boolean

  /**
   * Test whether the color palette of this [image] can be inverted by a [GraphicsPanel]
   */
  fun isInvertible(image: File): Boolean
}
