package org.jetbrains.r.visualization.inlays.components

import org.jetbrains.annotations.Nls

data class InlayProgressStatus(val progress: RProgressStatus, @Nls val statusText: String = "")

enum class RProgressStatus {
  RUNNING,
  STOPPED_OK,
  STOPPED_ERROR,
}