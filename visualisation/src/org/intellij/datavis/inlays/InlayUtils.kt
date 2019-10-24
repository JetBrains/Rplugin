/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays

import com.intellij.openapi.editor.Editor
import java.awt.Point

/** Helper functions to update inlay components sizes. */
class InlayUtils {

  companion object {

    /** Updates bounds to all inlays components in editor. */
    fun updateInlaysInEditor(editor: Editor) {

      val end = editor.xyToLogicalPosition(Point(0, Int.MAX_VALUE))
      val offsetEnd = editor.logicalPositionToOffset(end)

      val inlays = editor.inlayModel.getBlockElementsInRange(0, offsetEnd)

      inlays.forEach { inlay ->
        if (inlay.renderer is InlayComponent) {
          (inlay.renderer as InlayComponent).updateComponentBounds(inlay)
        }
      }
    }
  }
}