package org.jetbrains.plugins.notebooks.editor

import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.tree.IElementType
import com.intellij.util.EventDispatcher
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines.CellType
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines.Interval
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines.IntervalListener
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines.Marker
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min


class JupyterNotebookCellLines private constructor(private val document: Document,
                                                   private val cellTypeAwareLexerProvider: LexerProvider): NotebookCellLines {
  private val markerCache = mutableListOf<Marker>()
  private val intervalCache = mutableListOf<Interval>()

  override val intervalListeners = EventDispatcher.create(IntervalListener::class.java)

  override var modificationStamp: Long = 0
    private set

  override fun markersIterator(startOffset: Int): ListIterator<Marker> {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    return markerCache.listIterator(getMarkerUpperBound(startOffset))
  }

  override fun intervalsIterator(startLine: Int): ListIterator<Interval> {
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

  override fun getIterator(interval: Interval): ListIterator<Interval> {
    check(interval == intervalCache[interval.ordinal])
    return intervalCache.listIterator(interval.ordinal)
  }

  private fun initialize(it: AsyncPromise<NotebookCellLines>) {
    runReadAction {
      document.addDocumentListener(documentListener)

      initializeEmptyLists()
    }
    it.setResult(this)
  }

  private fun initializeEmptyLists() {
    wrapErrors(null) {
      markerCache.addAll(markerSequence(document.immutableCharSequence, 0, 0))
      intervalCache.addAll(adjustedMarkers(0, markerCache).asSequence().zipWithNext(::markersToInterval))
    }
    checkIntegrity(null)
  }

  private fun markersToInterval(a: Marker, b: Marker) =
    Interval(a.ordinal, a.type, document.getLineNumber(a.offset)..document.getLineNumber(max(0, b.offset - 1)))

  private fun shiftOffsetToMarkerStart(text: CharSequence, initialOffset: Int): Int {
    val interestingTextAbsoluteOffset = max(0, initialOffset - cellTypeAwareLexerProvider.longestTokenLength + 1)
    val interestingText = text.subSequence(
      interestingTextAbsoluteOffset,
      min(text.length, initialOffset + cellTypeAwareLexerProvider.longestTokenLength - 1)
    )
    return markerSequence(interestingText, 0, interestingTextAbsoluteOffset).firstOrNull()?.offset?.takeIf { it < initialOffset }
           ?: initialOffset
  }

  private val documentListener = object : DocumentListener {
    private var shiftedStartOffsetBefore: Int = -1

    override fun beforeDocumentChange(event: DocumentEvent) {
      if (!cellTypeAwareLexerProvider.shouldParseWholeFile()) {
        shiftedStartOffsetBefore = shiftOffsetToMarkerStart(document.immutableCharSequence, event.offset)
      }
    }

    override fun documentChanged(event: DocumentEvent) {
      ApplicationManager.getApplication().assertWriteAccessAllowed()

      if (cellTypeAwareLexerProvider.shouldParseWholeFile()) {
        markerCache.clear()
        val oldIntervals = intervalCache.toList()
        intervalCache.clear()
        initializeEmptyLists()
        notifyChangedIntervals(oldIntervals, intervalCache.toList())
      }
      else {
        wrapErrors(event) {
          updateIntervals(
            updateMarkers(
              shiftedStartOffsetBefore = shiftedStartOffsetBefore,
              startOffset = event.offset,
              oldLength = event.oldLength,
              newLength = event.newLength,
            )
          )
        }
      }

      checkIntegrity(event)
    }

    override fun bulkUpdateFinished(document: Document) {
      ApplicationManager.getApplication().assertWriteAccessAllowed()
      markerCache.clear()
      intervalCache.clear()
      initializeEmptyLists()
    }
  }

  /**
   * @param firstIntervalOrdinal The start index of [intervalCache] where intervals should change.
   * @param markerSizeDiff Difference between new adjusted markers count and old adjusted markers.
   * @param adjustedNewMarkers New markers that also can contain additional bogus markers, not existing in [markerCache] but required
   *  for constricting intervals.
   */
  private class UpdateMarkersResult(
    val firstIntervalOrdinal: Int,
    val markerSizeDiff: Int,
    val adjustedNewMarkers: List<Marker>,
  )

  private fun updateMarkers(
    shiftedStartOffsetBefore: Int,
    startOffset: Int,
    oldLength: Int,
    newLength: Int,
  ): UpdateMarkersResult {
    // The document change may cut half of a marker at the start. In such case, rewind a bit to rescan from the marker start.
    val startDocumentOffset = min(
      shiftedStartOffsetBefore,
      shiftOffsetToMarkerStart(document.immutableCharSequence, startOffset)
    )

    val markerCacheCutStart = getMarkerUpperBound(startDocumentOffset)

    // Also, the document change may cut half of a marker at the end. Detect that and widen the text slice if needed.
    val endOffsetIncrement =
      markerCache
        .getOrNull(getMarkerUpperBound(startOffset + oldLength))
        ?.takeIf {
          // -1 because the document is changed until previousEndOffset excluding the end.
          (startOffset + oldLength - it.offset - 1) in 0 until it.length
        }
        ?.let { startOffset + oldLength - it.offset + it.length }
      ?: 0

    val previousEndDocumentOffset = startOffset + oldLength + endOffsetIncrement
    val endDocumentOffset = startOffset + newLength + endOffsetIncrement

    val markerCacheCutEndExclusive =
      markerCache
        .subList(markerCacheCutStart, markerCache.size)
        .withIndex()
        .takeWhile { it.value.offset < previousEndDocumentOffset }
        .lastOrNull()
        ?.let { markerCacheCutStart + it.index + 1 }
      ?: markerCacheCutStart

    val adjustedOldMarkersSize =
      (
        // markerCache contains real markers, as seen by the lexer. Intervals are constructed by combining two adjacent markers.
        // A bogus interval at the start should be created in order to generate the interval above the first real marker.
        // However, the bogus marker isn't needed if there's a marker right at the document start.
        (if (markerCacheCutStart != 0 || markerCache.getOrNull(0)?.offset != 0) 1 else 0) +

        markerCacheCutEndExclusive - markerCacheCutStart

        // Intervals are constructed by combining two adjacent markers. A bogus interval at the end should be created in order to
        // generate the interval below the last marker.
        + 1
      )

    val newMarkers = markerSequence(
      chars = document.immutableCharSequence.subSequence(startDocumentOffset, endDocumentOffset),
      ordinalIncrement = markerCacheCutStart,
      offsetIncrement = startDocumentOffset,
    ).toList()

    val diff = newLength - oldLength
    if (newMarkers != markerCache.subList(markerCacheCutStart, markerCacheCutEndExclusive)) {
      ++modificationStamp

      markerCache.substitute(markerCacheCutStart, markerCacheCutEndExclusive, newMarkers)
      for (idx in markerCacheCutStart + newMarkers.size until markerCache.size) {
        markerCache[idx] = markerCache[idx].let { it.copy(offset = it.offset + diff, ordinal = idx) }
      }
    }
    else if (diff != 0 && markerCacheCutStart < markerCache.size) {
      // No markers changed, but some markers might have shifted. Shifting all markers after the document event offset
      // (not after the adjusted offset).
      val start = markerCacheCutStart + if (markerCache[markerCacheCutStart].offset < startOffset) 1 else 0
      for (index in start until markerCache.size) {
        markerCache[index] = markerCache[index].let { it.copy(offset = it.offset + diff) }
      }
    }

    val adjustedNewMarkers = adjustedMarkers(markerCacheCutStart, newMarkers)
    return UpdateMarkersResult(
      firstIntervalOrdinal = max(0, markerCacheCutStart - if (markerCacheCutStart == 0 && markerCache.getOrNull(0)?.offset != 0) 0 else 1),
      markerSizeDiff = adjustedNewMarkers.size - adjustedOldMarkersSize,
      adjustedNewMarkers = adjustedNewMarkers,
    )
  }

  private fun adjustedMarkers(startOrdinal: Int, sublist: List<Marker>): List<Marker> = mutableListOf<Marker>().also { result ->
    // markerCache contains real markers, as seen by the lexer. Intervals are constructed by combining two adjacent markers.
    // A bogus interval at the start should be created in order to generate the interval above the first real marker.
    // However, the bogus marker isn't needed if there's a marker right at the document start.
    val ephemeralStart =
      startOrdinal == 0 &&
      markerCache.getOrNull(0)?.offset != 0 &&
      sublist.firstOrNull().let { it?.ordinal != 0 || it.offset != 0 }
    result.addIfNotNull(
      when {
        ephemeralStart -> Marker(0, CellType.RAW, 0, 0)
        sublist.firstOrNull()?.ordinal == 0 -> null
        else -> markerCache.getOrNull(startOrdinal - 1)
      })

    val ordinalShift = result.firstOrNull()?.ordinal?.let { it + 1 } ?: 0
    for ((index, marker) in sublist.withIndex()) {
      result.add(marker.copy(ordinal = index + ordinalShift))
    }

    val lastOrdinal = (result.lastOrNull()?.ordinal ?: startOrdinal) + (if (ephemeralStart) 0 else 1)
    result.add(
      markerCache.getOrNull(lastOrdinal)
      // Intervals are constructed by combining two adjacent markers. A bogus interval at the end should be created in order to
      // generate the interval below the last marker.
      // +1 -- later it'll be decreased back while constructing the last interval.
      ?: Marker(lastOrdinal, CellType.RAW, document.textLength + 1, 0)
    )
  }

  private fun updateIntervals(updateMarkersResult: UpdateMarkersResult) {
    val firstIntervalOrdinal = updateMarkersResult.firstIntervalOrdinal
    val markerSizeDiff = updateMarkersResult.markerSizeDiff
    val newMarkers = updateMarkersResult.adjustedNewMarkers

    val oldIntervals = intervalCache.subList(
      firstIntervalOrdinal,
      // Interval is an entity between two adjacent markers. Amount of intervals is always less than amount of *adjusted* markers by one.
      firstIntervalOrdinal + newMarkers.size - markerSizeDiff - 1,
    ).toList()
    val newIntervals = newMarkers.zipWithNext(::markersToInterval)
    if (oldIntervals != newIntervals) {
      val lineDiff =
        (newIntervals.takeIf { it.isNotEmpty() }?.run { last().lines.last - first().lines.first + 1 } ?: 0) -
        (oldIntervals.takeIf { it.isNotEmpty() }?.run { last().lines.last - first().lines.first + 1 } ?: 0)

      intervalCache.substitute(firstIntervalOrdinal, firstIntervalOrdinal + oldIntervals.size, newIntervals)
      for (idx in firstIntervalOrdinal + newIntervals.size until intervalCache.size) {
        intervalCache[idx] = intervalCache[idx].let {
          it.copy(
            ordinal = idx,
            lines = (it.lines.first + lineDiff)..(it.lines.last + lineDiff),
          )
        }
      }

      // If one marker is just converted to another one, lists will be trimmed at left by one.
      notifyChangedIntervals(oldIntervals, newIntervals)
    }
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

  private fun notifyChangedIntervals(oldIntervals: List<Interval>, newIntervals: List<Interval>) {
    val trimLeft = oldIntervals.asSequence()
      .zip(newIntervals.asSequence())
      .takeWhile { (o, n) -> o == n }
      .count()

    val trimRight = oldIntervals.reversed().asSequence()
      .zip(newIntervals.reversed().asSequence())
      .takeWhile { (o, n) ->
        o.type == n.type &&
        o.lines.last - o.lines.first == n.lines.last - n.lines.first
      }
      .count()
      .coerceAtMost(min(oldIntervals.size, newIntervals.size) - trimLeft)

    if (oldIntervals.size != newIntervals.size || trimLeft + trimRight != oldIntervals.size) {
      intervalListeners.multicaster.segmentChanged(
        oldIntervals.run { subList(trimLeft, size - trimRight) },
        newIntervals.run { subList(trimLeft, size - trimRight) },
      )
    }
  }

  private fun markerSequence(chars: CharSequence, ordinalIncrement: Int, offsetIncrement: Int): Sequence<Marker> = sequence {
    val lexer = cellTypeAwareLexerProvider.createNotebookCellTypeAwareLexer()
    lexer.start(chars, 0, chars.length)
    var ordinal = 0
    while (true) {
      val tokenType = lexer.tokenType ?: break
      val cellType = cellTypeAwareLexerProvider.getCellType(tokenType)
      if (cellType != null) {
        yield(Marker(
          ordinal = ordinal++ + ordinalIncrement,
          type = cellType,
          offset = lexer.currentPosition.offset + offsetIncrement,
          length = lexer.tokenText.length,
        ))
      }
      lexer.advance()
    }
  }

  private fun checkIntegrity(event: DocumentEvent?) {  // TODO It's expensive. Should be deleted later, or covered by a flag.
    val problems = mutableListOf<String>()
    for ((idx, marker) in markerCache.withIndex()) {
      if (marker.ordinal != idx)
        problems += "$marker: expected ordinal $idx"

      if (idx < markerCache.size - 1 && marker.offset + marker.length > markerCache[idx + 1].offset)
        problems += "$marker overlaps with ${markerCache[idx + 1]}"

      if (marker.offset < 0 || marker.offset + marker.length > document.textLength)
        problems += "$marker is out of document contents"

      if (marker.length <= 0)
        problems += "$marker length is not positive"
    }

    for ((idx, interval) in intervalCache.withIndex()) {
      if (interval.ordinal != idx)
        problems += "$interval: expected ordinal $idx"

      if (idx < intervalCache.size - 1 && interval.lines.last + 1 != intervalCache[idx + 1].lines.first)
        problems += "No junction between $interval and ${intervalCache[idx + 1]}"

      if (idx == 0 && interval.lines.first != 0)
        problems += "The first $interval is expected to start with line 0"

      if (idx == intervalCache.size - 1 && interval.lines.last + 1 != document.lineCount.coerceAtLeast(1))
        problems += "The last $interval is expected to end with line ${document.lineCount}"
    }

    if (problems.isNotEmpty()) {
      LOG.error(
        "Integrity failure: ${problems[0]}",
        Attachment("all_errors.txt", problems.joinToString("\n")),
        *commonErrorAttachments(event),
      )
    }
  }

  private fun commonErrorAttachments(event: DocumentEvent?): Array<Attachment> = listOfNotNull(
    Attachment("all_markers.txt", markerCache.withIndex().joinToString("\n") { (idx, marker) -> "$idx: $marker" })
      .apply { isIncluded = true },
    Attachment("all_intervals.txt", intervalCache.withIndex().joinToString("\n") { (idx, interval) -> "$idx: $interval" })
      .apply { isIncluded = true },
    Attachment("document.txt", document.text)
      .apply { isIncluded = false },
    event?.run {
      Attachment("event_old_fragment.txt", oldFragment.toString())
        .apply { isIncluded = false }
    },
    event?.run {
      Attachment("event_new_fragment.txt", oldFragment.toString())
        .apply { isIncluded = false }
    },
    event?.run {
      Attachment("event.txt", """
        Start offset: $offset
        Old length: $oldLength
        New length: $newLength
        """.trimIndent())
        .apply { isIncluded = true }
    },
  ).toTypedArray()

  private inline fun <T> wrapErrors(event: DocumentEvent?, handler: () -> T): T =
    try {
      handler()
    }
    catch (err: Throwable) {
      err.addSuppressed(RuntimeExceptionWithAttachments("", *commonErrorAttachments(event)))
      throw err
    }

  companion object {
    private val LOG = logger<JupyterNotebookCellLines>()
    private val map = ContainerUtil.createConcurrentWeakMap<Document, Promise<NotebookCellLines>>()

    // TODO Maybe get rid of the linear or binary search? It looks like an over-optimization.
    private val BINARY_SEARCH_THRESHOLD: Int
      get() =
        NotebookCellLines.overriddenBinarySearchThreshold
        ?: Registry.intValue("pycharm.ds.notebook.editor.ui.binary.search.threshold")


    fun get(document: Document, language: Language): NotebookCellLines =
      get(document, NotebookCellTypeAwareLexerProvider.forLanguage(language))

    fun get(document: Document, lexerProvider: LexerProvider): NotebookCellLines {
      val promise = AsyncPromise<NotebookCellLines>()
      val actualPromise = map.putIfAbsent(document, promise)
                          ?: promise.also { JupyterNotebookCellLines(document, lexerProvider).initialize(it) }
      return actualPromise.blockingGet(1, TimeUnit.MILLISECONDS)!!
    }

    fun get(editor: Editor): NotebookCellLines {
      val psiDocumentManager = PsiDocumentManager.getInstance(editor.project!!)
      val document = editor.document
      val psiFile = psiDocumentManager.getPsiFile(document) ?: error("document ${document} doesn't have PSI file")
      return get(document, psiFile.language)
    }
  }

  interface LexerProvider {
    val longestTokenLength: Int

    fun createNotebookCellTypeAwareLexer(): Lexer

    fun getCellType(tokenType: IElementType): CellType?

    fun shouldParseWholeFile(): Boolean = false
  }
}

private fun <T> MutableList<T>.substitute(start: Int, end: Int, pattern: List<T>) {
  val sizeDiff = pattern.size + start - end
  when {
    sizeDiff == 0 -> {
      for ((idx, value) in pattern.withIndex()) {
        this[start + idx] = value
      }
    }
    sizeDiff > 0 -> {
      addAll(start, pattern.subList(0, sizeDiff))
      for (idx in sizeDiff until pattern.size) {
        this[start + idx] = pattern[idx]
      }
    }
    else -> {
      subList(start, start - sizeDiff).clear()
      for ((idx, value) in pattern.withIndex()) {
        this[start + idx] = value
      }
    }
  }
}