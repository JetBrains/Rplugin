package org.jetbrains.plugins.notebooks.editor.outputs

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.extensions.ExtensionPointName
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.JComponent

interface NotebookOutputComponentFactory {
  /** Result type of [match]. Not intended to be used elsewhere. */
  enum class Match { NONE, COMPATIBLE, SAME }

  /** Instructs how the component should be stretched horizontally. */
  enum class WidthStretching {
    /** The component gets the width of the visible area regardless of its preferred width. */
    STRETCH_AND_SQUEEZE,

    /** The component is expanded to the width of the visible area if its preferred width is less. */
    STRETCH,

    /** The component is shrinked to the width of the visible area if its preferred width is more. */
    SQUEEZE,

    /** The component has its preferred width. */
    NOTHING,
  }

  interface GutterPainter {
    fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle)
  }

  data class CreatedComponent(
    val component: JComponent,
    val widthStretching: WidthStretching,
    val gutterPainter: GutterPainter?,

    /** Experimental. The meaning can be changed, the type can be changed, the field can be removed. */
    val hasUnlimitedHeight: Boolean = false,
    val forceHeightLimitForComplexOutputs: Boolean = false
  )

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
  fun createComponent(editor: EditorImpl, output: NotebookOutputDataKey, disposable: Disposable): CreatedComponent?

  companion object {
    val EP_NAME: ExtensionPointName<NotebookOutputComponentFactory> =
      ExtensionPointName.create("org.jetbrains.plugins.notebooks.editor.outputs.notebookOutputComponentFactory")

    var JComponent.gutterPainter: GutterPainter?
      get() =
        getClientProperty(GutterPainter::class.java) as GutterPainter?
      internal set(value) =
        putClientProperty(GutterPainter::class.java, value)
  }
}
