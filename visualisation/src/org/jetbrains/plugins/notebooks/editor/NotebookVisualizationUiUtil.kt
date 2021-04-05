package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.TextRange
import com.intellij.util.containers.ContainerUtil
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KProperty

infix fun IntRange.hasIntersectionWith(other: IntRange): Boolean =
  !(first > other.last || last < other.first)

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

fun Editor.getCell(line: Int): NotebookCellLines.Interval =
  NotebookCellLines.get(this).intervalsIterator(line).next()

fun Editor.getCells(lines: IntRange): List<NotebookCellLines.Interval> =
  NotebookCellLines.get(this).getCells(lines).toList()

fun Editor.getPrimarySelectedCell(): NotebookCellLines.Interval =
  getCell(caretModel.primaryCaret.logicalPosition.line)

fun Editor.getSelectedCells(): List<NotebookCellLines.Interval> {
  val notebookCellLines = NotebookCellLines.get(this)
  return caretModel.allCarets.flatMap { caret ->
    notebookCellLines.getCells(document.getSelectionLines(caret))
  }.distinct()
}

fun Editor.isSelectedCell(cell: NotebookCellLines.Interval): Boolean =
  caretModel.allCarets.any { caret ->
    document.getSelectionLines(caret).hasIntersectionWith(cell.lines)
  }

fun Editor.deselectCell(cell: NotebookCellLines.Interval) {
  for (caret in caretModel.allCarets) {
    if (caret.logicalPosition.line in cell.lines) {
      caretModel.removeCaret(caret)
    }
  }
}

private fun NotebookCellLines.getCells(lines: IntRange): Sequence<NotebookCellLines.Interval> =
  intervalsIterator(lines.first).asSequence().takeWhile { it.lines.first <= lines.last }

private fun Document.getSelectionLines(caret: Caret): IntRange =
  IntRange(getLineNumber(caret.selectionStart), getLineNumber(caret.selectionEnd))

/** Both lists should be sorted by the [IntRange.first]. */
fun MutableList<IntRange>.mergeAndJoinIntersections(other: List<IntRange>) {
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

class SwingClientProperty<T, R: T?> {
  operator fun getValue(thisRef: JComponent, property: KProperty<*>): R =
    @Suppress("UNCHECKED_CAST") (thisRef.getClientProperty(property) as R)

  operator fun setValue(thisRef: JComponent, property: KProperty<*>, value: R) {
    thisRef.putClientProperty(property, value)
  }
}