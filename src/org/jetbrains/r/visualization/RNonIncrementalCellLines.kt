package org.jetbrains.r.visualization

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.TextRange
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.ThreadingAssertions

/**
 * inspired by [org.jetbrains.plugins.notebooks.editor.NotebookCellLinesImpl],
 * calculates all markers and intervals from scratch for each document update
 */
class RNonIncrementalCellLines private constructor(
  private val document: Document,
  private val intervalsGenerator: RIntervalsGenerator,
) : RNotebookCellLines {

  override var intervals: List<RNotebookCellLines.Interval> = intervalsGenerator.makeIntervals(document)
    private set

  private val documentListener = createDocumentListener()
  override val intervalListeners = EventDispatcher.create(RNotebookCellLines.IntervalListener::class.java)

  override var modificationStamp: Long = 0
    private set

  init {
    document.addDocumentListener(documentListener)
  }

  override fun intervalsIterator(startLine: Int): ListIterator<RNotebookCellLines.Interval> {
    ThreadingAssertions.assertReadAccess()
    val ordinal = intervals.find { startLine <= it.lines.last }?.ordinal ?: intervals.size
    return intervals.listIterator(ordinal)
  }

  private fun notifyChanged(
    oldCells: List<RNotebookCellLines.Interval>,
    oldAffectedCells: List<RNotebookCellLines.Interval>,
    newCells: List<RNotebookCellLines.Interval>,
    newAffectedCells: List<RNotebookCellLines.Interval>,
    documentEvent: DocumentEvent,
  ) {
    val (trimmedOldCells, trimmedNewCells) =
      if (oldCells == newCells) {
        Pair(emptyList(), emptyList())
      }
      else {
        ++modificationStamp

        val trimAtBegin = oldCells.asSequence().zip(newCells.asSequence()).takeWhile { (oldCell, newCell) ->
          oldCell == newCell &&
          oldCell != oldAffectedCells.firstOrNull() && newCell != newAffectedCells.firstOrNull()
        }.count()

        val trimAtEnd = oldCells.asReversed().asSequence().zip(newCells.asReversed().asSequence()).takeWhile { (oldCell, newCell) ->
          oldCell.type == newCell.type &&
          oldCell.language == newCell.language &&
          oldCell.size == newCell.size &&
          oldCell != oldAffectedCells.lastOrNull() && newCell != newAffectedCells.lastOrNull()
        }.count()

        Pair(trimmed(oldCells, trimAtBegin, trimAtEnd), trimmed(newCells, trimAtBegin, trimAtEnd))
      }

    val event = RNotebookCellLinesEvent(
      documentEvent = documentEvent,
      oldIntervals = trimmedOldCells,
      oldAffectedIntervals = oldAffectedCells,
      newIntervals = trimmedNewCells,
      newAffectedIntervals = newAffectedCells,
      modificationStamp = modificationStamp,
    )

    catchThrowableAndLog {
      intervalListeners.multicaster.documentChanged(event)
    }
  }

  private fun createDocumentListener() = object : DocumentListener {
    private var oldAffectedCells: List<RNotebookCellLines.Interval> = emptyList()

    override fun beforeDocumentChange(event: DocumentEvent) {
      oldAffectedCells = getAffectedCells(intervals, document, TextRange(event.offset, event.offset + event.oldLength))

      catchThrowableAndLog {
        intervalListeners.multicaster.beforeDocumentChange(
          RNotebookCellLinesEventBeforeChange(
            documentEvent = event,
            oldAffectedIntervals = oldAffectedCells,
            modificationStamp = modificationStamp
          )
        )
      }
    }

    override fun documentChanged(event: DocumentEvent) {
      ThreadingAssertions.assertWriteAccess()
      val oldIntervals = intervals
      intervals = intervalsGenerator.makeIntervals(document)

      val newAffectedCells = getAffectedCells(intervals, document, TextRange(event.offset, event.offset + event.newLength))
      notifyChanged(oldIntervals, oldAffectedCells, intervals, newAffectedCells, event)
    }
  }

  private inline fun catchThrowableAndLog(func: () -> Unit) {
    try {
      func()
    }
    catch (t: Throwable) {
      thisLogger().error("NotebookCellLines.IntervalListener shouldn't throw exceptions", t)
      // consume exception, otherwise this will prevent document updating. See DS-4305
    }
  }

  companion object {
    internal fun create(document: Document, intervalsGenerator: RIntervalsGenerator): RNotebookCellLines =
      RNonIncrementalCellLines(document, intervalsGenerator)
  }
}

private fun <T> trimmed(list: List<T>, trimAtBegin: Int, trimAtEnd: Int) =
  list.subList(trimAtBegin, list.size - trimAtEnd)

private val RNotebookCellLines.Interval.size: Int
  get() = lines.last + 1 - lines.first

private fun getAffectedCells(
  intervals: List<RNotebookCellLines.Interval>,
  document: Document,
  textRange: TextRange,
): List<RNotebookCellLines.Interval> {
  val firstLine = document.getLineNumber(textRange.startOffset).let { line ->
    if (document.getLineEndOffset(line) == textRange.startOffset) line + 1 else line
  }

  val endLine = run {
    val line = document.getLineNumber(textRange.endOffset)
    val isAtStartOfLine = document.getLineStartOffset(line) == textRange.endOffset
    // for example: "CELL2" => "cell1\nCELL2"
    // CELL2 wasn't modified, but textRange.endOffset = 6 and getLineNumber(6) == 1.
    // so line number should be decreased by 1
    val isAtTheDocumentEnd = document.textLength == textRange.endOffset
    // RMarkdown may contain empty md cell after last \n symbol.
    // for example, "```{r}\ncode\n```\n" has code cell at lines 0..2 and empty markdown cell at line 3
    // empty cell has text length==0 and should be marked as affected cell - it begins and ends at textRange.endOffset
    if (isAtStartOfLine && !isAtTheDocumentEnd) line - 1 else line
  }

  return intervals.asSequence().dropWhile {
    it.lines.last < firstLine
  }.takeWhile {
    it.lines.first <= endLine
  }.toList()
}