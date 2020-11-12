package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.extensions.ExtensionPointName
import javax.swing.JComponent

interface NotebookOutputComponentFactory {
  /** Result type of [match]. Not intended to be used elsewhere. */
  enum class Match { NONE, COMPATIBLE, SAME }

  /**
   * Check if the [component] can update it's content with the [outputDataKey].
   *
   * @returns
   *  [Match.NONE] if the [component] can't represent the [outputDataKey].
   *  [Match.COMPATIBLE] if the [component] can represent the [outputDataKey] by calling [updateComponent].
   *  [Match.SAME] if the [component] already represents the [outputDataKey], and call of [updateComponent] would change nothing.
   */
  fun match(component: JComponent, outputDataKey: NotebookOutputDataKey): Match

  /**
   * Updates the data representing by the component. Can never be called if [match] with the same arguments returned [Match.NONE].
   */
  fun updateComponent(editor: EditorImpl, component: JComponent, outputDataKey: NotebookOutputDataKey)

  /**
   * May return `null` if the factory can't create any component for specific subtype of [NotebookOutputDataKey].
   */
  fun createComponent(editor: EditorImpl, output: NotebookOutputDataKey): JComponent?

  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<NotebookOutputComponentFactory> =
      ExtensionPointName.create("org.jetbrains.plugins.notebooks.editor.outputs.notebookOutputComponentFactory")
  }
}