package org.jetbrains.r.visualization

import com.intellij.lang.Language
import com.intellij.notebooks.visualization.NotebookCellLines
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
import com.intellij.util.EventDispatcher
import com.intellij.util.keyFMap.KeyFMap
import java.util.*

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
    CODE, MARKDOWN, RAW
  }

  enum class MarkersAtLines(val hasTopLine: Boolean, val hasBottomLine: Boolean) {
    NO(false, false),
    TOP(true, false),
    BOTTOM(false, true),
    TOP_AND_BOTTOM(true, true)
  }

  data class Interval(
    val ordinal: Int,
    val type: CellType,
    val lines: IntRange,
    val markers: MarkersAtLines,
    val data: KeyFMap, // different notebook implementations could store their own values in this map
  ) : Comparable<Interval> {
    val language: Language = data.get(INTERVAL_LANGUAGE_KEY)!!

    operator fun <V> get(key: Key<V>): V? = data.get(key)

    override fun compareTo(other: Interval): Int = lines.first - other.lines.first
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

  fun intervalsIterator(startLine: Int = 0): ListIterator<Interval>

  val intervals: List<Interval>

  val intervalListeners: EventDispatcher<IntervalListener>

  val modificationStamp: Long

  companion object {
    // TODO val INTERVAL_LANGUAGE_KEY = Key.create<Language>("org.jetbrains.r.visualization.RNotebookCellLines.Interval.language")
    val INTERVAL_LANGUAGE_KEY = NotebookCellLines.INTERVAL_LANGUAGE_KEY

    /**
     * TODO return RNotebookCellLines instead if NotebookCellLines
     */
    fun get(document: Document): NotebookCellLines =
      NotebookCellLines.get(document)
  }
}
