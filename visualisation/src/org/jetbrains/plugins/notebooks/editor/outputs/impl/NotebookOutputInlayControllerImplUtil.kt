package org.jetbrains.plugins.notebooks.editor.outputs.impl

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.ScrollingModelImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.SwingClientProperty
import org.jetbrains.plugins.notebooks.editor.cellSelectionModel
import org.jetbrains.plugins.notebooks.editor.outputs.NotebookOutputInlayController
import java.awt.BorderLayout
import java.awt.Rectangle
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

fun scrollToMarkedPosition(editor: Editor) {
  if (editor.document.getUserData(DOCUMENT_BEING_UPDATED) == true) {
    // At the moment it's impossible to calculate coordinates by line number
    return
  }
  val topLine = editor.getUserData(TARGET_LINE_KEY);
  if (topLine == null) {
    return
  }

  val shift = editor.getUserData(TARGET_LINE_SHIFT_KEY);
  if (shift == null) {
    return
  }

  val newLineY: Int = editor.logicalPositionToXY(LogicalPosition(topLine, 0)).y
  val scrollValue = newLineY - shift

  (editor.scrollingModel as ScrollingModelImpl).run {
    val wasAnimationEnabled = isAnimationEnabled
    if (wasAnimationEnabled) disableAnimation()
    try {
      scrollVertically(scrollValue)
    }
    finally {
      if (wasAnimationEnabled) enableAnimation()
    }
  }
}

fun markScrollingPosition(virtualFile: VirtualFile, project: Project) {
  val fileEditors = FileEditorManager.getInstance(project).getAllEditors(virtualFile)
  val editors = fileEditors.filterIsInstance<TextEditor>().map { it.editor }
  for (editor in editors) {
    markScrollingPosition(editor)
  }
}

fun markScrollingPosition(editor: Editor) {
  val visibleArea: Rectangle = editor.scrollingModel.visibleAreaOnScrollingFinished
  val selectedCell = editor.cellSelectionModel?.selectedCells?.firstOrNull()
  var targetCell: NotebookCellLines.Interval? = null
  if (selectedCell != null) {
    val cellYTop = editor.logicalPositionToXY(LogicalPosition(selectedCell.lines.first, 0)).y
    if (cellYTop >= visibleArea.y &&
        cellYTop <= visibleArea.y + visibleArea.height) {
      targetCell = selectedCell
    }
  }
  markScrollingPosition(editor, targetCell)
}

fun markScrollingPosition(editor: Editor, targetCell: NotebookCellLines.Interval?) {
  val visibleArea: Rectangle = editor.scrollingModel.visibleAreaOnScrollingFinished
  if (visibleArea.height <= 0) {
    return
  }
  var topLine: Int = editor.xyToVisualPosition(visibleArea.location).line

  if (editor is EditorImpl) {
    if (targetCell != null) {
      topLine = targetCell.lines.first
    } else {
      // Looking for the first cell that starts on the screen
      while (topLine < editor.document.lineCount - 1 && editor.logicalPositionToXY(LogicalPosition(topLine, 0)).y < visibleArea.y &&
             editor.logicalPositionToXY(LogicalPosition(topLine + 1, 0)).y <= visibleArea.y + visibleArea.height) {
        topLine++
      }
    }
  }
  val lineToKeep = topLine
  val lineY: Int = editor.logicalPositionToXY(LogicalPosition(lineToKeep, 0)).y
  val shift = lineY - visibleArea.y

  editor.putUserData(TARGET_LINE_KEY, lineToKeep)
  editor.putUserData(TARGET_LINE_SHIFT_KEY, shift)
}