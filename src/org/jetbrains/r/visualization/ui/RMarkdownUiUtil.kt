package org.jetbrains.r.visualization.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.TextRange
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import java.awt.Graphics
import kotlin.math.max

internal inline fun <T, G : Graphics> G.use(handler: (g: G) -> T): T =
  try {
    handler(this)
  }
  finally {
    dispose()
  }

/**
 * Creates a document listener that will be automatically unregistered when the editor is disposed.
 */
internal fun Editor.addEditorDocumentListener(listener: DocumentListener) {
  require(this is EditorImpl)
  if (!isDisposed) {
    document.addDocumentListener(listener, disposable)
  }
}

internal fun Document.getText(interval: Interval): String =
  getText(TextRange(
    getLineStartOffset(interval.lines.first),
    getLineEndOffset(interval.lines.last)
  ))

internal fun MutableList<IntRange>.mergeAndJoinIntersections(other: List<IntRange>) {
  val merged = ContainerUtil.mergeSortedLists(this, other, Comparator { o1, o2 -> o1.first - o2.first }, false)
  clear()
  for (current in merged) {
    val previous = removeLastOrNull()
    when {
      previous == null -> add(current)
      previous.last + 1 >= current.first -> add(previous.first..max(previous.last, current.last))
      else -> {
        add(previous)
        add(current)
      }
    }
  }
}

/**
 * Changes the color scheme of consoleEditor to the color scheme of the main editor, if required.
 * [editor] is a main notebook editor, [consoleEditor] editor of particular console output.
 */
internal fun updateOutputTextConsoleUI(consoleEditor: EditorEx, editor: Editor) {
  if (consoleEditor.colorsScheme != editor.colorsScheme) {
    consoleEditor.colorsScheme = editor.colorsScheme
  }
}
