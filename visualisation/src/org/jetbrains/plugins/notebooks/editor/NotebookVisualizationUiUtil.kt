package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.TextRange
import java.awt.Color
import java.awt.Graphics
import kotlin.math.min
import java.awt.Rectangle

infix fun IntRange.hasIntersectionWith(other: IntRange): Boolean =
  if (first < other.first) other.first in this || other.last in this
  else first in other || last in other

inline fun <T, G : Graphics> G.use(handler: (g: G) -> T): T =
  try {
    handler(this)
  }
  finally {
    dispose()
  }

inline fun <T> trimLists(left: List<T>, right: List<T>, comparator: (T, T) -> Boolean): Pair<List<T>, List<T>> {
  val minSize = min(left.size, right.size)

  var trimLeft = 0
  while (trimLeft < minSize && comparator(left[trimLeft], right[trimLeft])) {
    ++trimLeft
  }

  var trimRight = 0
  while (trimRight < minSize - trimLeft && comparator(left[left.size - trimRight - 1], right[right.size - trimRight - 1])) {
    ++trimRight
  }

  return left.run { subList(trimLeft, size - trimRight) } to right.run { subList(trimLeft, size - trimRight) }
}

inline fun paintNotebookCellBackgroundGutter(
  editor: Editor,
  g: Graphics,
  r: Rectangle,
  stripe: Color?,
  top: Int,
  height: Int,
  crossinline actionBetweenBackgroundAndStripe: () -> Unit = {}
) {
  val appearance = editor.notebookAppearance
  val borderWidth = appearance.getLeftBorderWidth()
  g.color = appearance.getCodeCellBackground(editor.colorsScheme)
  g.fillRect(r.width - borderWidth, top, borderWidth, height)
  actionBetweenBackgroundAndStripe()
  if (stripe != null) {
    appearance.paintCellStripe(g, r, stripe, top, height)
  }
}

fun NotebookEditorAppearance.paintCellStripe(
  g: Graphics,
  r: Rectangle,
  stripe: Color,
  top: Int,
  height: Int,
) {
  g.color = stripe
  g.fillRect(r.width - getLeftBorderWidth(), top, getCellLeftLineWidth(), height)
}

/**
 * Creates a document listener that will be automatically unregistered when the editor is disposed.
 */
fun Editor.addEditorDocumentListener(listener: DocumentListener) {
  require(this is EditorImpl)
  if (!isDisposed) {
    document.addDocumentListener(listener, disposable)
  }
}

fun Document.getText(interval: NotebookCellLines.Interval): String =
  getText(TextRange(
    getLineStartOffset(interval.lines.first),
    getLineEndOffset(interval.lines.last)
  ))