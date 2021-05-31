package org.jetbrains.plugins.notebooks.editor.outputs.impl

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.editor.impl.ScrollingModelImpl
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.notebooks.editor.SwingClientProperty
import org.jetbrains.plugins.notebooks.editor.cellSelectionModel
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputInlayController
import java.awt.BorderLayout
import javax.swing.JComponent

internal var EditorGutterComponentEx.hoveredCollapsingComponentRect: CollapsingComponent? by SwingClientProperty()

// TODO It severely breaks encapsulation. At least, we should cover it with tests.
internal val NotebookOutputInlayController.collapsingComponents: List<CollapsingComponent>
  get() = inlay
    .renderer
    .let { (it as JComponent).getComponent(0)!! }
    .let { it as SurroundingComponent }
    .let { (it.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER) }
    .let { it as InnerComponent }
    .components
    .map { it as CollapsingComponent }

val NotebookOutputInlayController.outputComponents: List<JComponent>
  @TestOnly get() = collapsingComponents.map { it.mainComponent }

fun scrollToSelectedCell(editor: Editor) {
  if (!editor.caretModel.isUpToDate) {
    return
  }

  val cell = editor.cellSelectionModel?.primarySelectedCell
  if (cell != null) {
    val lineNumber = cell.lines.first
    val offset = editor.document.getLineEndOffset(lineNumber)
    (editor.scrollingModel as ScrollingModelImpl).run {
      val wasAnimationEnabled = isAnimationEnabled
      disableAnimation()
      try {
        editor.scrollingModel.scrollTo(editor.offsetToLogicalPosition(offset), ScrollType.MAKE_VISIBLE)
      }
      finally {
        if (wasAnimationEnabled) enableAnimation()
      }
    }
  }
}