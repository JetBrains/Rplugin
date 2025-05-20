package org.jetbrains.r.visualization.inlays

import java.nio.file.Path
import javax.swing.Icon

sealed interface InlayOutputData {
  val preview: Icon?

  data class Image(val path: Path, override val preview: Icon?) : InlayOutputData

  data class HtmlUrl(val url: String, override val preview: Icon?) : InlayOutputData

  data class CsvTable(val text: String, override val preview: Icon?) : InlayOutputData

  data class TextOutput(val path: Path, override val preview: Icon?) : InlayOutputData
}