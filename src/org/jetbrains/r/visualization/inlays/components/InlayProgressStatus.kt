package org.jetbrains.r.visualization.inlays.components

import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.progress.ProgressStatus

data class InlayProgressStatus(val progress: ProgressStatus, @Nls val statusText: String = "")