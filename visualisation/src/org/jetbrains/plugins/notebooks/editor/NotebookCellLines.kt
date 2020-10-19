package org.jetbrains.plugins.notebooks.editor

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.EventDispatcher
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

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
class NotebookCellLines private constructor(private val document: Document,
                                            private val cellTypeAwareLexerProvider: NotebookCellTypeAwareLexerProvider) {
  private val markerCache = mutableListOf<Marker>()
  private val intervalCache = mutableListOf<Interval>()

  enum class CellType {
    CODE, MARKDOWN, RAW
  }

  data class Marker(
    val ordinal: Int,
    val type: CellType,
    val offset: Int,
    val length: Int
  ) : Comparable<Marker> {
    override fun compareTo(other: Marker): Int = offset - other.offset
  }

  data class Interval(
    val ordinal: Int,
    val type: CellType,
    val lines: IntRange
  ) : Comparable<Interval> {
    override fun compareTo(other: Interval): Int = lines.first - other.lines.first
  }

  interface IntervalListener : EventListener {
    /**
     * Called when amount of intervals is changed, or types of some intervals are changed.
     * It is NOT called if lines of some intervals are changed.
     *
     * Intervals in the lists are valid only until the document changes. Check their validity
     * when postponing handling of intervals.
     *
     * It is guaranteed that:
     * * At least one of [oldIntervals] and [newIntervals] is not empty.
     * * Ordinals from every list defines an arithmetical progression where
     *   every next element has ordinal of the previous element incremented by one.
     * * If both lists are not empty, the first elements of both lists has the same ordinal.
     * * Both lists don't contain any interval that has been only shifted, shrank or enlarged.
     *
     * See `NotebookCellLinesTest` for examples of calls for various changes.
     */
    fun segmentChanged(oldIntervals: List<Interval>, newIntervals: List<Interval>)
  }

  val intervalListeners = EventDispatcher.create(IntervalListener::class.java)

  var modificationStamp: Long = 0
    private set

  fun markersIterator(startOffset: Int = 0): ListIterator<Marker> {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    return markerCache.listIterator(getMarkerUpperBound(startOffset))
  }

  fun intervalsIterator(startLine: Int = 0): ListIterator<Interval> {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    var fromIndex =
      if (intervalCache.size < BINARY_SEARCH_THRESHOLD) 0
      else Collections
        .binarySearch(intervalCache, Interval(-1, CellType.RAW, startLine..startLine))
        .let { if (it < 0) -it - 2 else it - 1 }
        .coerceAtLeast(0)
    while (fromIndex < intervalCache.size && intervalCache[fromIndex].lines.last < startLine) {
      ++fromIndex
    }
    return intervalCache.listIterator(fromIndex)
  }

  fun getIterator(interval: Interval): ListIterator<Interval> {
    check(interval == intervalCache[interval.ordinal])
    return intervalCache.listIterator(interval.ordinal)
  }

  private fun initialize(it: AsyncPromise<NotebookCellLines>) {
    runReadAction {
      document.addDocumentListener(documentListener)
      markerSequence(document.immutableCharSequence).toCollection(markerCache)
      val interestingMarkerSequence: Sequence<Pair<CellType, Int>> = markerCache
        .asSequence()
        .map { it.type to document.getLineNumber(it.offset) }
      updateIntervalCache(interestingMarkerSequence, document, 0, 0)
    }
    it.setResult(this)
  }

  private data class DocumentUpdate(
    val document: Document,
    val startLine: Int,
    val startOffset: Int,
    val endLine: Int,
    val markerCacheCutStart: Int,
    val markerCacheCutEndExclusive: Int
  ) {
    val endOffset = min(document.getLineEndOffset(endLine) + 1, document.textLength)
  }

  private val documentListener = object : DocumentListener {
    private var previousEndLine: Int = -1
    private var previousEndOffset: Int = -1

    override fun beforeDocumentChange(event: DocumentEvent) {
      val document = event.document
      previousEndLine = document.getLineNumber(event.offset + event.oldLength)
      previousEndOffset = document.getLineEndOffset(previousEndLine)
    }

    private fun getDocumentUpdate(event: DocumentEvent): DocumentUpdate {
      val document = event.document
      if (cellTypeAwareLexerProvider.shouldParseWholeFile()) {
        val startLine = 0
        val startOffset = 0
        val endLine = document.lineCount - 1
        val markerCacheCutStart = 0
        val markerCacheCutEndExclusive = markerCache.size
        return DocumentUpdate(document, startLine, startOffset, endLine, markerCacheCutStart, markerCacheCutEndExclusive)
      } else {
        val startLine = document.getLineNumber(event.offset)
        val startOffset = document.getLineStartOffset(startLine)
        val endLine = document.getLineNumber(event.offset + event.newLength)
        val markerCacheCutStart = getMarkerUpperBound(startOffset)
        val markerCacheCutEndExclusive = getMarkerCacheCutEndExclusive(markerCacheCutStart)
        return DocumentUpdate(document, startLine, startOffset, endLine, markerCacheCutStart, markerCacheCutEndExclusive)
      }
    }

    override fun documentChanged(event: DocumentEvent) {
      ApplicationManager.getApplication().assertWriteAccessAllowed()
      val upd = getDocumentUpdate(event)

      val oldMarkers = markerCache.subList(upd.markerCacheCutStart, upd.markerCacheCutEndExclusive).toList()
      val newMarkers = markerSequence(document.immutableCharSequence.subSequence(upd.startOffset, upd.endOffset)).toList()

      val oldIntervals =
        intervalsIterator(upd.startLine).asSequence().takeWhile { it.lines.first <= previousEndLine }.toList()

      val markersChanged = newMarkers != oldMarkers
      val diff = event.newLength - event.oldLength
      if (markersChanged || diff != 0) {
        if (markersChanged) {
          ++modificationStamp

          val rest = markerCache.subList(upd.markerCacheCutEndExclusive, markerCache.size).toList()
          markerCache.subList(upd.markerCacheCutStart, markerCache.size).clear()
          for (marker in newMarkers) {
            markerCache.add(marker.copy(offset = marker.offset + upd.startOffset, ordinal = markerCache.size))
          }
          for (marker in rest) {
            markerCache.add(marker.copy(offset = marker.offset + diff, ordinal = markerCache.size))
          }
        }
        else if (diff != 0 && upd.markerCacheCutStart < markerCache.size) {
          val start =
            upd.markerCacheCutStart + if (markerCache[upd.markerCacheCutStart].offset < event.offset) 1 else 0
          for (index in start until markerCache.size) {
            markerCache[index] = markerCache[index].let { it.copy(offset = it.offset + diff) }
          }
        }

        if (markersChanged || event.newFragment.count { it == '\n' } != event.oldFragment.count { it == '\n' }) {
          val intervalStartingOrdinal = max(0, upd.markerCacheCutStart - 1)
          val interestingMarkerSequence: Sequence<Pair<CellType, Int>> = markerCache
            .subList(intervalStartingOrdinal, markerCache.size)
            .asSequence()
            .map { it.type to document.getLineNumber(it.offset) }
          updateIntervalCache(interestingMarkerSequence, document, upd.markerCacheCutStart, intervalStartingOrdinal)
        }

        notifyIntervalListenersIfNeeded(oldIntervals, upd.endLine)
      }
    }

    override fun bulkUpdateFinished(document: Document) {
      ApplicationManager.getApplication().assertWriteAccessAllowed()
      markerCache.clear()
      markerSequence(document.immutableCharSequence).toCollection(markerCache)
    }

    private fun notifyIntervalListenersIfNeeded(oldIntervals: List<Interval>, endLine: Int) {
      val newIntervals =
        (oldIntervals.firstOrNull()?.ordinal ?: 0)
          .takeIf { it < intervalCache.size }
          ?.let(intervalCache::listIterator)
          ?.asSequence()
          ?.takeWhile { it.lines.first <= endLine }
          ?.toList()
        ?: emptyList()

      // Sometimes (for instance, when deleting cells) there could appear non-changed, just shifted intervals at starts and ends of lists.
      fun findTrim(oldSeq: Sequence<Interval>, newSeq: Sequence<Interval>) = oldSeq
        .zip(newSeq)
        .takeWhile { (old, new) ->
          old.type == new.type
          && old.lines.run { last - first } == new.lines.run { last - first }
        }
        .count()

      val trimRight = findTrim(
        oldIntervals.asReversed().asSequence(),
        newIntervals.asReversed().asSequence()
      )
      val trimLeft = findTrim(
        oldIntervals.run { subList(0, size - trimRight) }.asSequence(),
        newIntervals.run { subList(0, size - trimRight) }.asSequence()
      )

      if (trimRight != oldIntervals.size || trimRight != newIntervals.size) {
        val trimmedOldIntervals = oldIntervals.run { subList(trimLeft, size - trimRight) }
        val trimmedNewIntervals = newIntervals.run { subList(trimLeft, size - trimRight) }
        if (trimmedOldIntervals.map { it.type } != trimmedNewIntervals.map { it.type }) {
          intervalListeners.multicaster.segmentChanged(trimmedOldIntervals, trimmedNewIntervals)
        }
      }
    }

    private fun getMarkerCacheCutEndExclusive(markerCacheCutStart: Int): Int =
      markerCache
        .subList(markerCacheCutStart, markerCache.size)
        .withIndex()
        .takeWhile { it.value.offset < previousEndOffset }
        .lastOrNull()
        ?.let { markerCacheCutStart + it.index + 1 }
      ?: markerCacheCutStart
  }

  private fun updateIntervalCache(interestingMarkerSequence: Sequence<Pair<CellType, Int>>,
                                  document: Document,
                                  markerCacheCutStart: Int,
                                  intervalStartingOrdinal: Int) {
    val startBoundary =
      if (markerCache.isEmpty() || markerCacheCutStart == 0 && 0 < document.getLineNumber(markerCache[0].offset))
        sequenceOf(CellType.RAW to 0)
      else
        emptySequence()
    val endBoundary = sequenceOf(CellType.RAW to document.lineCount.coerceAtLeast(1))
    intervalCache.subList(intervalStartingOrdinal, intervalCache.size).clear()
    intervalCache.addAll(
      (startBoundary + interestingMarkerSequence + endBoundary)
        .withIndex()
        .zipWithNext { (index, current), (_, next) ->
          Interval(intervalStartingOrdinal + index, current.first, current.second until next.second)
        })
  }

  private fun getMarkerUpperBound(offset: Int): Int {
    val startIndex =
      if (markerCache.size < BINARY_SEARCH_THRESHOLD) 0
      else Collections.binarySearch(markerCache, Marker(-1, CellType.RAW, offset, 0)).let { if (it < 0) -it - 1 else it }
    return markerCache
             .subList(startIndex, markerCache.size)
             .withIndex()
             .dropWhile { it.value.offset < offset }
             .firstOrNull()
             ?.let { it.index + startIndex }
           ?: markerCache.size
  }

  private fun markerSequence(chars: CharSequence): Sequence<Marker> = sequence {
    val lexer = cellTypeAwareLexerProvider.createNotebookCellTypeAwareLexer()
    lexer.start(chars, 0, chars.length)
    var ordinal = 0
    while (true) {
      val tokenType = lexer.tokenType ?: break
      val cellType = cellTypeAwareLexerProvider.getCellType(tokenType)
      if (cellType != null) {
        yield(Marker(ordinal = ordinal++, type = cellType, offset = lexer.currentPosition.offset, length = lexer.tokenText.length))
      }
      lexer.advance()
    }
  }

  companion object {
    private val map = ContainerUtil.createConcurrentWeakMap<Document, Promise<NotebookCellLines>>()

    /** It's uneasy to change a registry value inside tests. */
    @TestOnly
    var overriddenBinarySearchThreshold: Int? = null

    // TODO Maybe get rid of the linear or binary search? It looks like an over-optimization.
    private val BINARY_SEARCH_THRESHOLD: Int
      get() =
        overriddenBinarySearchThreshold
        ?: Registry.intValue("pycharm.ds.notebook.editor.ui.binary.search.threshold")


    fun get(document: Document, language: Language): NotebookCellLines {
      val lexerProvider = NotebookCellTypeAwareLexerProvider.forLanguage(language)
      val promise = AsyncPromise<NotebookCellLines>()
      val actualPromise = map.putIfAbsent(document, promise)
                          ?: promise.also { NotebookCellLines(document, lexerProvider).initialize(it) }
      return actualPromise.blockingGet(1, TimeUnit.MILLISECONDS)!!
    }

    fun get(editor: Editor): NotebookCellLines {
      val psiDocumentManager = PsiDocumentManager.getInstance(editor.project!!)
      val document = editor.document
      val psiFile = psiDocumentManager.getPsiFile(document) ?: error("document ${document} doesn't have PSI file")
      return get(document, psiFile.language)
    }
  }
}
