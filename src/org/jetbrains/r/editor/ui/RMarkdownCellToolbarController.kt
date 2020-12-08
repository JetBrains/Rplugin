package org.jetbrains.r.editor.ui

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorEmbeddedComponentManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import org.jetbrains.plugins.notebooks.editor.*
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
  inlayOffset: Int,
) : NotebookCellInlayController, EditorCustomElementRenderer {

  private val panel = RMarkdownCellToolbarPanel(editor)

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
        inlayOffset
      )
    )!!

  init {
    performForCommittedPsi(editor, inlay) { psiElement ->
      panel.addToolbar(psiElement)
    }
  }

  override fun paintGutter(editor: EditorImpl, g: Graphics, r: Rectangle, intervalIterator: ListIterator<NotebookCellLines.Interval>) = Unit

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
          val offset = offset(editor.document, interval.lines)

          currentControllers.asSequence()
            .filterIsInstance<RMarkdownCellToolbarController>()
            .firstOrNull {
              it.inlay.isRelatedToPrecedingText == isRelatedToPrecedingText
              && it.inlay.offset == offset
            }
          ?: RMarkdownCellToolbarController(editor, this, offset)
         }
        NotebookCellLines.CellType.MARKDOWN,
        NotebookCellLines.CellType.RAW -> null
      }
    }
  }

  companion object {
    private const val isRelatedToPrecedingText: Boolean = true

    private fun offset(document: Document, codeLines: IntRange): Int =
      Integer.min(document.getLineStartOffset(codeLines.first) + 1, document.textLength)
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


private class RMarkdownCellToolbarPanel(val editor: EditorImpl) : JPanel() {
  private var hasToolbar = false

  fun addToolbar(psiElement: PsiElement) {
    require(!hasToolbar)
    hasToolbar = true
    val toolbar = ActionManager.getInstance().createActionToolbar("InlineToolbar", createToolbarActionGroup(editor, psiElement), true)
    add(toolbar.component)
    toolbar.component.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  }

  init {
    isOpaque = false
    background = editor.notebookAppearance.getCodeCellBackground(editor.colorsScheme)
    layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
  }

  private fun createToolbarActionGroup(editor: Editor, psiElement: PsiElement): ActionGroup {
    val psiElementPointer = SmartPointerManager.createPointer(psiElement)
    val actions = RunChunkNavigator.createChunkToolbarActionsList(psiElementPointer, editor)
    return DefaultActionGroup(actions)
  }
}
