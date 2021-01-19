package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.extensions.ExtensionPointName
import javax.swing.JPanel

interface NotebookOutputComponentWrapper {
  /**
   * Allows to add new components around notebook cell output components.
   *
   * The [component] is the container that holds cell output's scroll pane. It must have `BorderLayout` as a layout.
   * The center of the layout is already occupied by the scroll pane. All other placeholders are vacant and can be filled
   * with additional components.
   */
  fun wrap(component: JPanel)

  /**
   * Notifies about new output component created and added to [outerComponent]
   *
   * If [wrap] method is called only on the [outerComponent] creation, this method is called on every new output added to
   * the component. It allows to use information from cell's output and show it on [outerComponent].
   * [outputDataKey] represents corresponding data key for newly added output
   */
  fun newOutputComponentCreated(outerComponent: JPanel, editor: EditorImpl, outputDataKey: NotebookOutputDataKey)

  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<NotebookOutputComponentWrapper> =
      ExtensionPointName.create("org.jetbrains.plugins.notebooks.editor.outputs.notebookOutputComponentWrapper")
  }
}