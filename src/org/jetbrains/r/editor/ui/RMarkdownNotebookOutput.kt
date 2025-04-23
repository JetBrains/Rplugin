package org.jetbrains.r.editor.ui

import com.intellij.openapi.util.Key
import org.jetbrains.r.visualization.RNotebookIntervalPointer
import org.jetbrains.r.visualization.inlays.components.InlayProgressStatus

/**
 * calls to clearOutputs, addText and updateOutputs are runned in edt, order of calls is preserved
 * dispose() and updates of RMarkdownNotebook done at call time
 */
interface RMarkdownNotebookOutput {
  val intervalPointer: RNotebookIntervalPointer

  /** clear outputs and text */
  fun clearOutputs(removeFiles: Boolean)

  /** add text as output */
  fun addText(text: String, outputType: Key<*>)

  /** do clearOutputs(), load outputs from filesystem */
  fun updateOutputs()

  fun updateProgressStatus(progressStatus: InlayProgressStatus)

  fun dispose()
}