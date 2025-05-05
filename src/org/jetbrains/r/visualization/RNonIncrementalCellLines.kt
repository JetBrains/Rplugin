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

  @Volatile
  override var snapshot = RNotebookCellLines.Snapshot(0, intervalsGenerator.makeIntervals(document))
    private set

  private val documentListener = createDocumentListener()
  override val intervalListeners = EventDispatcher.create(RNotebookCellLines.IntervalListener::class.java)

  init {
    document.addDocumentListener(documentListener)
  }

  private fun makeNewSnapshotAndEvent(
    oldSnapshot: RNotebookCellLines.Snapshot,
    oldAffectedCells: List<RNotebookCellLines.Interval>,
    newCells: List<RNotebookCellLines.Interval>,
    newAffectedCells: List<RNotebookCellLines.Interval>,
    documentEvent: DocumentEvent,
  ): Pair<RNotebookCellLines.Snapshot, RNotebookCellLinesEvent> {
    val oldCells = oldSnapshot.intervals

    if (oldCells == newCells) {
      val newSnapshot = RNotebookCellLines.Snapshot(modificationStamp = oldSnapshot.modificationStamp, intervals = newCells)
      // intervals are the same, don't update modificationStamp

      val event = RNotebookCellLinesEvent(
        documentEvent = documentEvent,
        oldIntervals = emptyList(),
        oldAffectedIntervals = oldAffectedCells,
        newIntervals = emptyList(),
        newAffectedIntervals = newAffectedCells,
        modificationStamp = newSnapshot.modificationStamp,
      )

      return Pair(newSnapshot, event)
    }

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

    val newSnapshot = RNotebookCellLines.Snapshot(oldSnapshot.modificationStamp + 1, newCells)

    val trimmedOldCells = trimmed(oldCells, trimAtBegin, trimAtEnd)
    val trimmedNewCells = trimmed(newCells, trimAtBegin, trimAtEnd)

    val event = RNotebookCellLinesEvent(
      documentEvent = documentEvent,
      oldIntervals = trimmedOldCells,
      oldAffectedIntervals = oldAffectedCells,
      newIntervals = trimmedNewCells,
      newAffectedIntervals = newAffectedCells,
      modificationStamp = newSnapshot.modificationStamp,
    )

    return Pair(newSnapshot, event)
  }

  private fun createDocumentListener() = object : DocumentListener {
    private var oldAffectedCells: List<RNotebookCellLines.Interval> = emptyList()

    override fun beforeDocumentChange(event: DocumentEvent) {
      val currentSnapshot = snapshot
      oldAffectedCells = getAffectedCells(currentSnapshot.intervals, document, TextRange(event.offset, event.offset + event.oldLength))

      catchThrowableAndLog {
        intervalListeners.multicaster.beforeDocumentChange(
          RNotebookCellLinesEventBeforeChange(
            documentEvent = event,
            oldAffectedIntervals = oldAffectedCells,
            modificationStamp = currentSnapshot.modificationStamp
          )
        )
      }
    }

    override fun documentChanged(event: DocumentEvent) {
      ThreadingAssertions.assertWriteAccess()
      val newIntervals = intervalsGenerator.makeIntervals(document)
      val newAffectedCells = getAffectedCells(newIntervals, document, TextRange(event.offset, event.offset + event.newLength))

      val (newSnapshot, event) = makeNewSnapshotAndEvent(snapshot, oldAffectedCells, newIntervals, newAffectedCells, event)
      snapshot = newSnapshot

      catchThrowableAndLog {
        intervalListeners.multicaster.documentChanged(event)
      }
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