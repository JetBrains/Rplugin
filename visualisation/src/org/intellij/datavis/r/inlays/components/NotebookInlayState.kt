/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components

import javax.swing.JLayeredPane

/** Base class for all NotebookInlay states. Inlay could be Data(Table/Chart) or Output(text/html) */
abstract class NotebookInlayState : JLayeredPane() {

  /**
   * Inlay component can can adjust itself to fit the Output.
   * We need callback because text output can return height immediately,
   * but Html output can return height only delayed, from it's Platform.runLater.
   */
  var onHeightCalculated: ((height: Int) -> Unit)? = null

  var clearAction: () -> Unit = {}

  abstract fun clear()

  /** Short description of inlay content. */
  abstract fun getCollapsedDescription(): String

  open fun updateProgressStatus(progressStatus: InlayProgressStatus) {}

  open fun onViewportChange(isInViewport: Boolean) {
    // Do nothing by default
  }
}
