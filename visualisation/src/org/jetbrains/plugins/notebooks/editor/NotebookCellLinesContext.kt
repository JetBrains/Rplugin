package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.util.castSafelyTo
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * Get the position of a current [NotebookCellLines.Interval]. It is calculated according to the focused component and the caret position.
 */
val DataContext.notebookCellLinesInterval: NotebookCellLines.Interval?
  get() =
    getData(NOTEBOOK_CELL_LINES_INTERVAL_DATA_KEY)
    ?: getOffsetInEditorWithComponents(this) { it.notebookCellLinesProvider != null }
      ?.let { (editor, offset) ->
        NotebookCellLines.get(editor).intervalsIterator(editor.document.getLineNumber(offset)).takeIf { it.hasNext() }?.next()
      }

inline fun getOffsetInEditorWithComponents(
  dataContext: DataContext,
  crossinline editorFilter: (Editor) -> Boolean,
): Pair<Editor, Int>? =
  dataContext
    .getData(PlatformDataKeys.CONTEXT_COMPONENT)
    ?.castSafelyTo<EditorComponentImpl>()
    ?.editor
    ?.takeIf(editorFilter)
    ?.let { notebookEditor ->
      notebookEditor to notebookEditor.caretModel.offset.coerceAtMost(notebookEditor.document.textLength - 1).coerceAtLeast(0)
    }
  ?: getOffsetInEditorByComponentHierarchy(dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT), editorFilter)
  ?: getOffsetInEditorByComponentHierarchy(dataContext.getData(PlatformDataKeys.EDITOR)?.contentComponent, editorFilter)

/** Private API. */
@PublishedApi
internal inline fun getOffsetInEditorByComponentHierarchy(
  component: Component?,
  crossinline editorFilter: (Editor) -> Boolean,
): Pair<Editor, Int>? =
  generateSequence(component, Component::getParent)
    .zipWithNext()
    .mapNotNull { (child, parent) ->
      if (parent is EditorComponentImpl && editorFilter(parent.editor)) child to parent.editor
      else null
    }
    .firstOrNull()
    ?.let { (child, editor) ->
      val point = SwingUtilities.convertPoint(child, 0, 0, editor.contentComponent)
      editor to editor.logicalPositionToOffset(editor.xyToLogicalPosition(point))
    }