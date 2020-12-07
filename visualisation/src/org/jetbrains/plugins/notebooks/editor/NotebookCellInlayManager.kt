package org.jetbrains.plugins.notebooks.editor

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.Processor
import com.intellij.util.SmartList
import com.intellij.util.castSafelyTo
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.jetbrains.annotations.TestOnly
import java.awt.Graphics
import java.util.*
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min

class NotebookCellInlayManager private constructor(val editor: EditorImpl) {
  private val inlays: MutableMap<Inlay<*>, NotebookCellInlayController> = HashMap()
  private val notebookCellLines = NotebookCellLines.get(editor)
  private val viewportQueue = MergingUpdateQueue(VIEWPORT_TASK, VIEWPORT_TIME_SPAN, true, null, editor.disposable, null, true)
  private var initialized = false

  fun inlaysForInterval(interval: NotebookCellLines.Interval): Iterable<NotebookCellInlayController> =
    getMatchingInlaysForLines(interval.lines)

  fun update(interval: NotebookCellLines.Interval) {
    val actualInterval = notebookCellLines.getIterator(interval).takeIf { it.hasNext() }?.next()
    if (interval != actualInterval) {
      LOG.error(
        "Tried to call update() with outdated interval $interval while the actual interval for the same ordinal is $actualInterval.")
    }
    if (initialized) {
      updateInlays(interval.lines, listOf(interval))
    }
  }

  fun updateAll() {
    if (initialized) {
      val intervals = notebookCellLines.intervalsIterator().asSequence().toList()
      if (intervals.isNotEmpty()) {
        updateInlays(intervals.first().lines.first..intervals.last().lines.last, intervals)
      }
    }
  }



  private fun addViewportChangeListener() {
    editor.scrollPane.viewport.addChangeListener {
      viewportQueue.queue(object : Update(VIEWPORT_TASK) {
        override fun run() {
          if (Disposer.isDisposed(editor.disposable)) return
          for ((inlay, controller) in inlays) {
            controller.onViewportChange()

            // Many UI instances has overridden getPreferredSize relying on editor dimensions.
            inlay.renderer?.castSafelyTo<JComponent>()?.updateUI()
          }
        }
      })
    }
  }

  private fun initialize() {
    // TODO It would be a cool approach to add inlays lazily while scrolling.

    editor.putUserData(key, this)

    handleRefreshedDocument()

    addDocumentListener()

    val appMessageBus = ApplicationManager.getApplication().messageBus.connect(editor.disposable)

    appMessageBus.subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
      refreshHighlightersLookAndFeel()
    })
    appMessageBus.subscribe(LafManagerListener.TOPIC, LafManagerListener {
      refreshHighlightersLookAndFeel()
    })

    addViewportChangeListener()

    initialized = true
  }

  private fun refreshHighlightersLookAndFeel() {
    for (highlighter in editor.markupModel.allHighlighters) {
      if (highlighter.customRenderer === NotebookCellHighlighterRenderer) {
        (highlighter as? RangeHighlighterEx)?.setTextAttributes(textAttributesForHighlighter())
      }
    }
  }

  data class NotebookUpdateInterval(val matchingCells: List<NotebookCellLines.Interval>, val logicalLines: IntRange)

  fun getUpdateInterval(startOffset: Int, length: Int): NotebookUpdateInterval {
    val document = editor.document
    val start = document.getLineNumber(startOffset)
    val end = document.getLineNumber(min(startOffset + length, document.textLength))
    val logicalLines = start..end
    val matchingCells = notebookCellLines.getMatchingCells(logicalLines)
    return NotebookUpdateInterval(matchingCells, logicalLines)
  }

  private fun handleRefreshedDocument() {
    val allCellLines = notebookCellLines.intervalsIterator().asSequence().toList()
    val factories = NotebookCellInlayController.Factory.EP_NAME.extensionList
    for (interval in allCellLines) {
      for (factory in factories) {
        val controller = factory.compute(editor, emptyList(), notebookCellLines.getIterator(interval))
        if (controller != null) {
          rememberController(controller)
        }
      }
    }
    addHighlighters(allCellLines)
  }

  private fun addDocumentListener() {
    val documentListener = object : DocumentListener {
      private var matchingCellsBeforeChange: List<NotebookCellLines.Interval> = emptyList()
      private var isBulkModeEnabled = false;

      private fun interestingLogicalLines(document: Document, startOffset: Int, length: Int): IntRange {
        // Adding one additional line is needed to handle deletions at the end of the document.
        val end =
          if (startOffset + length <= document.textLength) document.getLineNumber(startOffset + length)
          else document.lineCount + 1
        return document.getLineNumber(startOffset)..end
      }

      override fun bulkUpdateStarting(document: Document) {
        isBulkModeEnabled = true
        matchingCellsBeforeChange = notebookCellLines.getMatchingCells(0 until document.lineCount)
      }

      override fun beforeDocumentChange(event: DocumentEvent) {
        if (isBulkModeEnabled) return
        val document = event.document
        val logicalLines = interestingLogicalLines(document, event.offset, event.oldLength)

        matchingCellsBeforeChange = notebookCellLines.getMatchingCells(logicalLines)
      }

      override fun documentChanged(event: DocumentEvent) {
        if (isBulkModeEnabled) return
        val logicalLines = interestingLogicalLines(event.document, event.offset, event.newLength)
        ensureInlaysAndHighlightersExist(matchingCellsBeforeChange, logicalLines)
      }

      override fun bulkUpdateFinished(document: Document) {
        isBulkModeEnabled = false
        // bulk mode is over, now we could access inlays, let's update them all
        ensureInlaysAndHighlightersExist(matchingCellsBeforeChange, 0 until document.lineCount)
      }
    }

    editor.document.addDocumentListener(documentListener, editor.disposable)
  }

  fun ensureInlaysAndHighlightersExist(matchingCellsBeforeChange: List<NotebookCellLines.Interval>, logicalLines: IntRange) {
    val interestingRange =
      matchingCellsBeforeChange
        .map { it.lines }
        .takeIf { it.isNotEmpty() }
        ?.let { min(logicalLines.first, it.first().first)..max(it.last().last, logicalLines.last) }
      ?: logicalLines
    val matchingIntervals = notebookCellLines.getMatchingCells(interestingRange)
    val fullInterestingRange =
      if (matchingIntervals.isNotEmpty()) matchingIntervals.first().lines.first..matchingIntervals.last().lines.last
      else interestingRange

    updateInlays(fullInterestingRange, matchingIntervals)
  }

  private fun updateInlays(fullInterestingRange: IntRange, matchingIntervals: List<NotebookCellLines.Interval>) {
    val existingHighlighters = getMatchingHighlightersForLines(fullInterestingRange)
    val intervalsToAddHighlightersFor = matchingIntervals.associateByTo(HashMap()) { it.lines }
    for (highlighter in existingHighlighters) {
      val lines = editor.document.run { getLineNumber(highlighter.startOffset)..getLineNumber(highlighter.endOffset) }
      if (intervalsToAddHighlightersFor.remove(lines) == null) {
        editor.markupModel.removeHighlighter(highlighter)
      }
    }
    addHighlighters(intervalsToAddHighlightersFor.values)

    val allMatchingInlays: MutableList<Pair<Int, NotebookCellInlayController>> =
      getMatchingInlaysForLines(fullInterestingRange)
        .mapTo(mutableListOf()) {
          editor.document.getLineNumber(it.inlay.offset) to it
        }
    val allFactories = NotebookCellInlayController.Factory.EP_NAME.extensionList

    for (interval in matchingIntervals) {
      val seenControllersByFactory: Map<NotebookCellInlayController.Factory, MutableList<NotebookCellInlayController>> =
        allFactories.associateWith { SmartList<NotebookCellInlayController>() }
      allMatchingInlays.removeIf { (inlayLine, controller) ->
        if (inlayLine in interval.lines) {
          seenControllersByFactory[controller.factory]?.add(controller)
          true
        }
        else false
      }
      for ((factory, controllers) in seenControllersByFactory) {
        val actualController = if (!Disposer.isDisposed(editor.disposable)) {
          factory.compute(editor, controllers, notebookCellLines.getIterator(interval))
        }
        else {
          null
        }
        if (actualController != null) {
          rememberController(actualController)
        }
        for (oldController in controllers) {
          if (oldController != actualController) {
            Disposer.dispose(oldController.inlay, false)
          }
        }
      }
    }

    for ((_, controller) in allMatchingInlays) {
      Disposer.dispose(controller.inlay, false)
    }
  }

  private fun rememberController(controller: NotebookCellInlayController) {
    inlays[controller.inlay] = controller
    Disposer.register(controller.inlay, Disposable {
      inlays.remove(controller.inlay)
    })
  }

  private fun getMatchingHighlightersForLines(lines: IntRange): List<RangeHighlighterEx> =
    mutableListOf<RangeHighlighterEx>()
      .also { list ->
        val startOffset = editor.document.getLineStartOffset(saturateLine(lines.first))
        val endOffset = editor.document.getLineEndOffset(saturateLine(lines.last))
        editor.markupModel.processRangeHighlightersOverlappingWith(startOffset, endOffset, Processor {
          if (it.customRenderer === NotebookCellHighlighterRenderer) {
            list.add(it)
          }
          true
        })
      }

  private fun getMatchingInlaysForLines(lines: IntRange): List<NotebookCellInlayController> =
    getMatchingInlaysForOffsets(
      editor.document.getLineStartOffset(saturateLine(lines.first)),
      editor.document.getLineEndOffset(saturateLine(lines.last)))

  private fun saturateLine(line: Int): Int =
    line.coerceAtMost(editor.document.lineCount - 1).coerceAtLeast(0)

  private fun getMatchingInlaysForOffsets(startOffset: Int, endOffset: Int): List<NotebookCellInlayController> =
    editor.inlayModel
      .getBlockElementsInRange(startOffset, endOffset)
      .mapNotNull(inlays::get)

  private fun addHighlighters(intervals: Collection<NotebookCellLines.Interval>) {
    val document = editor.document
    for (interval in intervals) {
      if (interval.type == NotebookCellLines.CellType.CODE) {
        val highlighter = editor.markupModel.addRangeHighlighter(
          document.getLineStartOffset(interval.lines.first),
          document.getLineEndOffset(interval.lines.last),
          // Code cell background should be seen behind any syntax highlighting, selection or any other effect.
          HighlighterLayer.FIRST - 100,
          textAttributesForHighlighter(),
          HighlighterTargetArea.LINES_IN_RANGE
        )
        highlighter.customRenderer = NotebookCellHighlighterRenderer
      }
    }
  }

  private fun textAttributesForHighlighter() = TextAttributes().apply {
    backgroundColor = editor.notebookAppearance.getCodeCellBackground(editor.colorsScheme)
  }

  private fun NotebookCellLines.getMatchingCells(logicalLines: IntRange): List<NotebookCellLines.Interval> =
    mutableListOf<NotebookCellLines.Interval>().also { result ->
      // Since inlay appearance may depend from neighbour cells, adding one more cell at the start and at the end.
      val iterator = intervalsIterator(logicalLines.first)
      if (iterator.hasPrevious()) iterator.previous()
      for (interval in iterator) {
        result.add(interval)
        if (interval.lines.first > logicalLines.last) break
      }
    }

  @TestOnly
  fun getInlays(): MutableMap<Inlay<*>, NotebookCellInlayController> = inlays

  @TestOnly
  fun updateControllers(matchingCells: List<NotebookCellLines.Interval>, logicalLines: IntRange) {
    ensureInlaysAndHighlightersExist(matchingCells, logicalLines)
  }

  companion object {
    private val LOG = logger<NotebookCellInlayManager>()

    @JvmStatic
    fun install(editor: EditorImpl) {
      NotebookCellInlayManager(editor).initialize()
    }

    @JvmStatic
    fun get(editor: Editor): NotebookCellInlayManager? = key.get(editor)

    private val key = Key.create<NotebookCellInlayManager>(NotebookCellInlayManager::class.java.name)
    private const val VIEWPORT_TASK = "Viewport Update"
    private const val VIEWPORT_TIME_SPAN = 100
  }
}

/**
 * Renders rectangle in the right part of editor to make filled code cells look like rectangles with margins.
 * But mostly it's used as a token to filter notebook cell highlighters.
 */
private object NotebookCellHighlighterRenderer : CustomHighlighterRenderer {
  override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
    editor as EditorImpl
    @Suppress("NAME_SHADOWING") g.create().use { g ->
      val scrollbarWidth = editor.scrollPane.verticalScrollBar.width
      val oldBounds = g.clipBounds
      val visibleArea = editor.scrollingModel.visibleArea
      g.setClip(
        visibleArea.x + visibleArea.width - scrollbarWidth,
        oldBounds.y,
        scrollbarWidth,
        oldBounds.height
      )

      g.color = editor.colorsScheme.defaultBackground
      g.clipBounds.run {
        g.fillRect(x, y, width, height)
      }
    }
  }
}