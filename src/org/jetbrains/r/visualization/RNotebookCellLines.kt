package org.jetbrains.r.visualization

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.util.EventDispatcher
import java.util.EventListener

/**
 * Incrementally iterates over Notebook document, calculates line ranges of cells using lexer.
 * Fast enough for running in EDT, but could be used in any other thread.
 *
 * Note: there's a difference between this model and the PSI model.
 * If a document starts not with a cell marker, this class treat the text before the first cell marker as a raw cell.
 * PSI model treats such cell as a special "stem" cell which is not a Jupyter cell at all.
 * We haven't decided which model is correct and which should be fixed. So, for now avoid using stem cells in tests,
 * while UI of PyCharm DS doesn't allow to create a stem cell at all.
 */
interface RNotebookCellLines {
  enum class CellType {
    CODE,
    MARKDOWN,
  }

  enum class MarkersAtLines(val hasTopLine: Boolean, val hasBottomLine: Boolean) {
    NO(false, false),
    TOP(true, false),
    BOTTOM(false, true),
    TOP_AND_BOTTOM(true, true),
  }

  data class Interval(
    val ordinal: Int,
    val type: CellType,
    val lines: IntRange,
    val markers: MarkersAtLines,
    val language: Language,
  ) : Comparable<Interval> {
    override fun compareTo(other: Interval): Int = lines.first - other.lines.first
  }

  data class Snapshot(
    val modificationStamp: Long,
    val intervals: List<Interval>,
  ) {
    fun intervalByLine(line: Int): Interval =
      intervals.find { line <= it.lines.last }!!

    fun intervalsIteratorByLine(startLine: Int = 0): ListIterator<Interval> {
      val ordinal = intervals.find { startLine <= it.lines.last }?.ordinal ?: intervals.size
      return intervals.listIterator(ordinal)
    }
  }

  interface IntervalListener : EventListener {
    /**
     * Called each time when document is changed, even if intervals are the same.
     * Contains DocumentEvent and additional information about intervals.
     * Components which work with intervals can simply listen for NotebookCellLinesEvent and don't subscribe for DocumentEvent.
     * Listener shouldn't throw exceptions
     */
    fun documentChanged(event: RNotebookCellLinesEvent)

    /**
     * Called each time before document is changed.
     * Listener shouldn't throw exceptions
     */
    fun beforeDocumentChange(event: RNotebookCellLinesEventBeforeChange) {}
  }

  val snapshot: Snapshot

  val intervalListeners: EventDispatcher<IntervalListener>

  companion object {
    fun get(document: Document): RNotebookCellLines =
      RNotebookCellLinesProvider.get(document)

    fun getSnapshot(document: Document): Snapshot =
      get(document).snapshot
  }
}
