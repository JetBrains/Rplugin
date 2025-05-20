package org.jetbrains.r.visualization.inlays

import javax.swing.Icon

data class InlayOutputData(
  val data: String,
  val type: String,
  val preview: Icon?,
)