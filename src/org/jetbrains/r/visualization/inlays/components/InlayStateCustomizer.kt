/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays.components

import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.NotebookInlayState

/** This extension point allows to perform additional customizations to inlay outputs before adding
 * them to a notebook.
 *
 * @see org.jetbrains.plugins.notebooks.visualization.r.inlays.components.NotebookInlayState
 * */
interface InlayStateCustomizer {
  /** Applies customizations and return the inlay output. */
  fun customize(state: NotebookInlayState): NotebookInlayState

  companion object {
    fun customize(state: NotebookInlayState) {
      InlayStateScrollPaneCustomizer.customize(state)
    }
  }
}