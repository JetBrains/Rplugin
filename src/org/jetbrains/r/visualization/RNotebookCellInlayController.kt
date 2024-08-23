package org.jetbrains.r.visualization

import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLines
import org.jetbrains.r.editor.ui.RMarkdownCellToolbarControllerStable
import org.jetbrains.r.editor.ui.RMarkdownOutputInlayControllerStable
import java.awt.Graphics
import java.awt.Rectangle

interface RNotebookCellInlayController {
  interface Factory {
    /**
     * There must be at most one controller (and one inlay) of some factory attached to some cell.
     *
     * This methods consumes all controllers attached to some cell. Upon the method call,
     * there could be more than one controller attached to the cell.
     * For instance, it happens after cell deletion.
     *
     * The method should either choose one of the attached controllers, update and return it,
     * or should create a new controller, or return null if there should be no controller for the cell.
     * Inlays from all remaining controllers will be disposed automatically.
     *
     * The method may traverse iterator without returning to the initial position, the iterator is disposable.
     */
    fun compute(
      editor: EditorImpl,
      currentControllers: Collection<RNotebookCellInlayController>,
      interval: NotebookCellLines.Interval,
    ): RNotebookCellInlayController?

    companion object {
      val factories = listOf<Factory>(
        RMarkdownCellToolbarControllerStable.Factory(),
        RMarkdownOutputInlayControllerStable.Factory(),
      )
    }
  }

  val inlay: Inlay<*>

  val factory: Factory

  fun onViewportChange() {}

  /**
   * The method may traverse iterator without returning to the initial position, the iterator is disposable.
   */
  fun paintGutter(
    editor: EditorImpl,
    g: Graphics,
    r: Rectangle,
    interval: NotebookCellLines.Interval,
  )


  fun createGutterRendererLineMarker(editor: EditorEx, interval: NotebookCellLines.Interval) {}
}