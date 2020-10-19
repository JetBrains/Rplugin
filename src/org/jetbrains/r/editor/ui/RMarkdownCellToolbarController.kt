package org.jetbrains.r.editor.ui

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorEmbeddedComponentManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiDocumentManager
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownFile
import org.jetbrains.plugins.notebooks.editor.*
import org.jetbrains.r.rendering.chunk.ChunkActionByOffset
import org.jetbrains.r.rendering.chunk.RunChunkNavigator
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.plaf.PanelUI
import kotlin.math.max


internal class RMarkdownCellToolbarController private constructor(
  val editor: EditorImpl,
  override val factory: Factory,
  lines: IntRange,
  offset: Int,
) : NotebookCellInlayController, EditorCustomElementRenderer {

  private val panel = RMarkdownCellToolbarPanel(editor, offset)

  init {
    @Suppress("UsePropertyAccessSyntax")
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
        offset(editor.document, lines)
      )
    )!!

  override fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle, intervalIterator: ListIterator<NotebookCellLines.Interval>) = Unit

  class Factory : NotebookCellInlayController.Factory {
    override fun compute(editor: EditorImpl,
                         currentControllers: Collection<NotebookCellInlayController>,
                         intervalIterator: ListIterator<NotebookCellLines.Interval>
    ): NotebookCellInlayController? {
      val psiFile = editor.project?.let { PsiDocumentManager.getInstance(it) }?.getPsiFile(editor.document)
      if (psiFile !is MarkdownFile) {
        return null
      }
      val interval: NotebookCellLines.Interval = intervalIterator.next()
      return when (interval.type) {
        NotebookCellLines.CellType.CODE ->
          currentControllers.asSequence()
            .filterIsInstance<RMarkdownCellToolbarController>()
            .firstOrNull {
              it.inlay.isRelatedToPrecedingText == isRelatedToPrecedingText
              && it.inlay.offset == offset(editor.document, interval.lines)
            }
          ?: RMarkdownCellToolbarController(editor, this, interval.lines, offset(editor.document, interval.lines))
        NotebookCellLines.CellType.MARKDOWN,
        NotebookCellLines.CellType.RAW -> null
      }
    }
  }

  companion object {
    private const val isRelatedToPrecedingText: Boolean = true

    private fun offset(document: Document, codeLines: IntRange): Int =
      Integer.min(document.getLineEndOffset(codeLines.first) + 1, document.textLength)
  }
}


private class RMarkdownCellToolbarPanelUI(private val editor: EditorImpl) : PanelUI() {
  override fun getPreferredSize(c: JComponent): Dimension? {
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


private class RMarkdownCellToolbarPanel(editor: EditorImpl, offset: Int) : JPanel() {
  private val toolbar =
    ActionManager.getInstance().createActionToolbar("InlineToolbar", createToolbarActionGroup(offset), true)

  init {
    isOpaque = false
    background = editor.notebookAppearance.getCodeCellBackground(editor.colorsScheme)
    layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
    add(toolbar.component)
    toolbar.component.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  }

  private fun createToolbarActionGroup(offset: Int): ActionGroup {
    val wrapped = RunChunkNavigator.createRunChunkActionsList().map{ action -> ChunkActionByOffset(action, offset)}
    return DefaultActionGroup(wrapped)
  }
}
