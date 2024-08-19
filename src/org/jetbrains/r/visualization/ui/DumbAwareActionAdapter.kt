package org.jetbrains.r.visualization.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

abstract class DumbAwareActionAdapter : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    // Nothing to do here
  }
}