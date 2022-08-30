package org.jetbrains.r.editor.ui

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorEmbeddedComponentManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import org.jetbrains.plugins.notebooks.visualization.*
import org.jetbrains.plugins.notebooks.visualization.ui.SteadyUIPanel
import org.jetbrains.r.rendering.chunk.RunChunkNavigator
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.plaf.PanelUI
import kotlin.math.max


internal class RMarkdownCellToolbarController private constructor(
  val editor: EditorImpl,
  override val factory: Factory,
  private val intervalPointer: NotebookIntervalPointer,
  inlayOffset: Int,
) : NotebookCellInlayController, EditorCustomElementRenderer {

  private val panel = RMarkdownCellToolbarPanel(editor, intervalPointer)

  init {
    panel.setUI(RMarkdownCellToolbarPanelUI(editor))
  }

  override fun calcWidthInPixels(inlay: Inlay<*>): Int = inlay.editor.scrollingModel.visibleArea.width

  override fun calcHeightInPixels(inlay: Inlay<*>): Int = editor.notebookAppearance.run {
    INNER_CELL_TOOLBAR_HEIGHT + SPACE_BELOW_CELL_TOOLBAR
  }

  override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes): Unit = Unit

  override val inlay: Inlay<*> =
    EditorEmbeddedComponentManager.getInstance().addComponent(
      editor,
      panel,
      EditorEmbeddedComponentManager.Properties(
        EditorEmbeddedComponentManager.ResizePolicy.none(),
        null,
        isRelatedToPrecedingText,
        true,
        editor.notebookAppearance.JUPYTER_CELL_SPACERS_INLAY_PRIORITY,
        inlayOffset
      )
    )!!

  override fun paintGutter(editor: EditorImpl,
                           g: Graphics,
                           r: Rectangle,
                           interval: NotebookCellLines.Interval) {
    val inlayBounds = inlay.bounds ?: return
    paintNotebookCellBackgroundGutter(editor, g, r, interval, inlayBounds.y, inlayBounds.height)
  }

  class Factory : NotebookCellInlayController.Factory {
    override fun compute(editor: EditorImpl,
                         currentControllers: Collection<NotebookCellInlayController>,
                         intervalIterator: ListIterator<NotebookCellLines.Interval>
    ): NotebookCellInlayController? {
      if (!isRMarkdown(editor))
        return null

      val interval: NotebookCellLines.Interval = intervalIterator.next()
      return when (interval.type) {
        NotebookCellLines.CellType.CODE -> {
          val intervalPointer = NotebookIntervalPointerFactory.get(editor).create(interval)
          currentControllers.asSequence()
            .filterIsInstance<RMarkdownCellToolbarController>()
            .firstOrNull {
              it.intervalPointer.get() == intervalPointer.get()
            }
          ?: createController(editor, intervalPointer)
         }
        NotebookCellLines.CellType.MARKDOWN,
        NotebookCellLines.CellType.RAW -> null
      }
    }

    private fun createController(editor: EditorImpl, intervalPointer: NotebookIntervalPointer): RMarkdownCellToolbarController? {
      val offset = editor.document.getLineStartOffset(intervalPointer.get()!!.lines.first) + 1
      if (offset > editor.document.textLength) return null
      return RMarkdownCellToolbarController(editor, this, intervalPointer, offset)
    }
  }

  companion object {
    private const val isRelatedToPrecedingText: Boolean = true
  }
}


private class RMarkdownCellToolbarPanelUI(private val editor: EditorImpl) : PanelUI() {
  override fun getPreferredSize(c: JComponent): Dimension {
    val preferredSize = super.getPreferredSize(c)
    with(editor.notebookAppearance) {
      val height = max(preferredSize?.height ?: 0, INNER_CELL_TOOLBAR_HEIGHT + SPACE_BELOW_CELL_TOOLBAR)
      val width = max(preferredSize?.width ?: 0, editor.scrollingModel.visibleArea.width)
      return Dimension(width, height)
    }
  }

  override fun paint(g: Graphics, c: JComponent) {
    @Suppress("NAME_SHADOWING") g.create().use { g ->
      g.color = editor.notebookAppearance.getCodeCellBackground(editor.colorsScheme)
      g.fillRect(0, 0, editor.scrollPane.let { it.viewport.width - it.verticalScrollBar.width }, c.height)
    }
    super.paint(g, c)
  }
}


private class RMarkdownCellToolbarPanel(editor: EditorImpl, pointer: NotebookIntervalPointer) : SteadyUIPanel(RMarkdownCellToolbarPanelUI(editor)) {
  init {
    isOpaque = false
    background = editor.notebookAppearance.getCodeCellBackground(editor.colorsScheme)
    layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

    val toolbar = ActionManager.getInstance().createActionToolbar("InlineToolbar", createToolbarActionGroup(editor, pointer), true)
    toolbar.setTargetComponent(this)
    add(toolbar.component)
    toolbar.component.isOpaque = false
    toolbar.component.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  }

  private fun createToolbarActionGroup(editor: Editor, pointer: NotebookIntervalPointer): ActionGroup {
    val actions = RunChunkNavigator.createChunkToolbarActionsList(pointer, editor)
    return DefaultActionGroup(actions)
  }
}
