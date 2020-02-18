/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.graphics.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

abstract class RDumbAwareActionAdapter : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    // Nothing to do here
  }
}

class RPreviousGraphicsAction : RDumbAwareActionAdapter()

class RNextGraphicsAction : RDumbAwareActionAdapter()

class RExportGraphicsAction : RDumbAwareActionAdapter()

class RCopyGraphicsAction : RDumbAwareActionAdapter()

class RZoomGraphicsAction : RDumbAwareActionAdapter()

class RClearGraphicsAction : RDumbAwareActionAdapter()

class RClearAllGraphicsAction : RDumbAwareActionAdapter()

class RTuneGraphicsDeviceAction : RDumbAwareActionAdapter()
