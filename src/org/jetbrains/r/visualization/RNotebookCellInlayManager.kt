package org.jetbrains.r.visualization

import com.intellij.ide.DataManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.UI
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.r.psi.RPluginCoroutineScope
import com.intellij.util.Processor
import com.intellij.util.SmartList
import com.intellij.util.asSafely
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.containers.SmartHashSet
import com.intellij.util.ui.update.DebouncedUpdates
import kotlinx.coroutines.Dispatchers
import org.jetbrains.r.editor.RMarkdownEditorAppearance
import org.jetbrains.r.visualization.RNotebookCellLines.CellType
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import org.jetbrains.r.visualization.ui.mergeAndJoinIntersections
import org.jetbrains.r.visualization.ui.use
import java.awt.Graphics
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

class RNotebookCellInlayManager private constructor(val editor: EditorImpl) {
  private val disposable = Disposer.newCheckedDisposable(editor.disposable)
  private val inlays: MutableMap<Inlay<*>, RNotebookCellInlayController> = HashMap()
  private val notebookCellLines = RNotebookCellLines.get(editor.document)

  private val scope = RPluginCoroutineScope.getApplicationScope()

  private val viewportUpdateQueue = DebouncedUpdates.forScope<Unit>(scope, "RNotebookCellInlayManager Viewport Update", 100.milliseconds)
    .withContext(Dispatchers.UI)
    .runLatest { updateViewPort() }
    .cancelOnDispose(disposable)

  private sealed interface InlayUpdateEvent {
    data object UpdateAll : InlayUpdateEvent
    data class UpdatePointers(val pointers: Collection<RNotebookIntervalPointer>) : InlayUpdateEvent
  }

  /** 20 is 1000 / 50, two times faster than the eye refresh rate. Actually, the value has been chosen randomly, without experiments. */
  private val inlayUpdateQueue = DebouncedUpdates.forScope<InlayUpdateEvent>(scope, "RNotebookCellInlayManager Interval Update", 20.milliseconds)
    .withContext(Dispatchers.EDT)
    .runBatched { processInlayUpdatesBatch(it) }
    .cancelOnDispose(disposable)

  private var initialized = false

  fun inlaysForInterval(interval: Interval): Iterable<RNotebookCellInlayController> =
    getMatchingInlaysForLines(interval.lines)

  /** It's public, but think twice before using it. Called many times in a row, it can freeze UI. Consider using [update] instead. */
  internal fun updateImmediately(lines: IntRange) {
    if (initialized) {
      updateConsequentInlays(lines)
    }
  }

  /** It's public, but think seven times before using it. Called many times in a row, it can freeze UI. */
  internal fun updateAllImmediately() {
    if (initialized) {
      updateConsequentInlays(0..editor.document.lineCount)
    }
  }

  fun updateAll() {
    inlayUpdateQueue.queue(InlayUpdateEvent.UpdateAll)
  }

  fun update(pointers: Collection<RNotebookIntervalPointer>) {
    inlayUpdateQueue.queue(InlayUpdateEvent.UpdatePointers(pointers))
  }

  fun update(pointer: RNotebookIntervalPointer) {
    inlayUpdateQueue.queue(InlayUpdateEvent.UpdatePointers(listOf(pointer)))
  }

  private fun processInlayUpdatesBatch(batch: List<InlayUpdateEvent>) {
    // If ANY event is UpdateAll, do updateAll and skip rest
    if (batch.any { it is InlayUpdateEvent.UpdateAll }) {
      updateAllImmediately()
      return
    }

    val allPointers = batch.flatMapTo(SmartHashSet()) { (it as InlayUpdateEvent.UpdatePointers).pointers }

    val linesList = allPointers.mapNotNullTo(mutableListOf()) { it.get()?.lines }
    linesList.sortBy { it.first }
    linesList.mergeAndJoinIntersections(listOf())

    for (lines in linesList) {
      updateImmediately(lines)
    }
  }

  private fun addViewportChangeListener() {
    viewportUpdateQueue.queue(Unit)
  }

  private fun updateViewPort() {
    for ((inlay, controller) in inlays) {
      controller.onViewportChange()

      // Many UI instances has overridden getPreferredSize relying on editor dimensions.
      inlay.renderer.asSafely<JComponent>()?.updateUI()
    }
  }

  private fun initialize() {
    handleRefreshedDocument()

    addDocumentListener()

    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
      updateAll()
      refreshHighlightersLookAndFeel()
    })
    connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
      updateAll()
      refreshHighlightersLookAndFeel()
    })

    addViewportChangeListener()

    editor.putUserData(key, this)

    Disposer.register(disposable, Disposable {
      disposeAllInlays()
      editor.putUserData(key, null)
    })

    initialized = true
  }

  private fun refreshHighlightersLookAndFeel() {
    for (highlighter in editor.markupModel.allHighlighters) {
      if (highlighter.customRenderer === NotebookCellHighlighterRenderer) {
        (highlighter as? RangeHighlighterEx)?.setTextAttributes(textAttributesForHighlighter())
      }
    }
  }

  private fun handleRefreshedDocument() {
    ThreadingAssertions.assertReadAccess()
    val currentIntervals = notebookCellLines.snapshot.intervals

    val factories = RNotebookCellInlayController.Factory.factories
    for (interval in currentIntervals) {
      for (factory in factories) {
        val controller = failSafeCompute(factory, emptyList(), interval)
        if (controller != null) {
          rememberController(controller, interval)
        }
      }
    }
    addHighlighters(currentIntervals)
  }

  private fun addDocumentListener() {
    val documentListener = object : DocumentListener {
      private var matchingCellsBeforeChange: List<Interval> = emptyList()
      private var isBulkModeEnabled = false

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

    editor.document.addDocumentListener(documentListener, disposable)
  }

  private fun ensureInlaysAndHighlightersExist(matchingCellsBeforeChange: List<Interval>, logicalLines: IntRange) {
    val interestingRange =
      matchingCellsBeforeChange
        .map { it.lines }
        .takeIf { it.isNotEmpty() }
        ?.let { min(logicalLines.first, it.first().first)..max(it.last().last, logicalLines.last) }
      ?: logicalLines
    updateConsequentInlays(interestingRange)
  }

  private fun updateConsequentInlays(interestingRange: IntRange) {
    ThreadingAssertions.assertReadAccess()
    //editor.notebookCellEditorScrollingPositionKeeper?.saveSelectedCellPosition()
    val matchingIntervals = notebookCellLines.getMatchingCells(interestingRange)
    val fullInterestingRange =
      if (matchingIntervals.isNotEmpty()) matchingIntervals.first().lines.first..matchingIntervals.last().lines.last
      else interestingRange

    val existingHighlighters = getMatchingHighlightersForLines(fullInterestingRange)
    val intervalsToAddHighlightersFor = matchingIntervals.associateByTo(HashMap()) { it.lines }
    for (highlighter in existingHighlighters) {
      val lines = editor.document.run { getLineNumber(highlighter.startOffset)..getLineNumber(highlighter.endOffset) }
      if (intervalsToAddHighlightersFor.remove(lines)?.shouldHaveHighlighter != true) {
        editor.markupModel.removeHighlighter(highlighter)
      }
    }
    addHighlighters(intervalsToAddHighlightersFor.values)

    val allMatchingInlays: MutableList<Pair<Int, RNotebookCellInlayController>> = getMatchingInlaysForLines(fullInterestingRange)
      .mapTo(mutableListOf()) {
        editor.document.getLineNumber(it.inlay.offset) to it
      }
    val allFactories = RNotebookCellInlayController.Factory.factories

    for (interval in matchingIntervals) {
      val seenControllersByFactory: Map<RNotebookCellInlayController.Factory, MutableList<RNotebookCellInlayController>> =
        allFactories.associateWith { SmartList() }
      allMatchingInlays.removeIf { (inlayLine, controller) ->
        if (inlayLine in interval.lines) {
          seenControllersByFactory[controller.factory]?.add(controller)
          true
        }
        else false
      }
      for ((factory, controllers) in seenControllersByFactory) {
        val actualController = if (!disposable.isDisposed) {
          failSafeCompute(factory, controllers, interval)
        }
        else {
          null
        }
        if (actualController != null) {
          rememberController(actualController, interval)
        }
        for (oldController in controllers) {
          if (oldController != actualController) {
            Disposer.dispose(oldController.inlay, false)
          }
        }
      }
    }

    RNotebookGutterLineMarkerManager.updateHighlighters(editor)

    for ((_, controller) in allMatchingInlays) {
      Disposer.dispose(controller.inlay, false)
    }
  }

  private data class NotebookCellDataProvider(
    val editor: EditorImpl,
    val component: JComponent,
    val interval: Interval,
  ) : DataProvider {
    override fun getData(key: String): Any? =
      when (key) {
        R_NOTEBOOK_CELL_LINES_INTERVAL.name -> interval
        PlatformCoreDataKeys.CONTEXT_COMPONENT.name -> component
        PlatformDataKeys.EDITOR.name -> editor
        else -> null
      }
  }

  private fun rememberController(controller: RNotebookCellInlayController, interval: Interval) {
    val inlay = controller.inlay
    inlay.renderer.asSafely<JComponent>()?.let { component ->
      val oldProvider = DataManager.getDataProvider(component)
      if (oldProvider != null && oldProvider !is NotebookCellDataProvider) {
        LOG.error("Overwriting an existing CLIENT_PROPERTY_DATA_PROVIDER. Old provider: $oldProvider")
      }
      DataManager.removeDataProvider(component)
      DataManager.registerDataProvider(component, NotebookCellDataProvider(editor, component, interval))
    }
    if (inlays.put(inlay, controller) !== controller) {
      val disposableForDataProvider = Disposable {
        inlay.renderer.asSafely<JComponent>()?.let { DataManager.removeDataProvider(it) }
        inlays.remove(inlay)
      }
      if (Disposer.isDisposed(inlay)) {
        disposableForDataProvider.dispose()
      }
      else {
        Disposer.register(inlay, disposableForDataProvider)
      }
    }
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

  private fun getMatchingInlaysForLines(lines: IntRange): List<RNotebookCellInlayController> =
    getMatchingInlaysForOffsets(
      editor.document.getLineStartOffset(saturateLine(lines.first)),
      editor.document.getLineEndOffset(saturateLine(lines.last)))

  private fun saturateLine(line: Int): Int =
    line.coerceAtMost(editor.document.lineCount - 1).coerceAtLeast(0)

  private fun getMatchingInlaysForOffsets(startOffset: Int, endOffset: Int): List<RNotebookCellInlayController> =
    editor.inlayModel
      .getBlockElementsInRange(startOffset, endOffset)
      .mapNotNull(inlays::get)

  private val Interval.shouldHaveHighlighter: Boolean
    get() = type == CellType.CODE

  private fun addHighlighters(intervals: Collection<Interval>) {
    val document = editor.document
    for (interval in intervals) {
      if (interval.shouldHaveHighlighter) {
        val highlighter = editor.markupModel.addRangeHighlighter(
          document.getLineStartOffset(interval.lines.first),
          document.getLineEndOffset(interval.lines.last),
          // Code cell background should be seen behind any syntax highlighting, selection or any other effect.
          HighlighterLayer.FIRST - 100,
          textAttributesForHighlighter(),
          HighlighterTargetArea.LINES_IN_RANGE
        )
        highlighter.setCustomRenderer(NotebookCellHighlighterRenderer)
      }
    }
  }

  private fun textAttributesForHighlighter() = TextAttributes().apply {
    backgroundColor = RMarkdownEditorAppearance.getCodeCellBackgroundColor(editor.colorsScheme)
  }

  private fun RNotebookCellLines.getMatchingCells(logicalLines: IntRange): List<Interval> =
    mutableListOf<Interval>().also { result ->
      // Since inlay appearance may depend from neighbour cells, adding one more cell at the start and at the end.
      val iterator = snapshot.intervalsIteratorByLine(logicalLines.first)
      if (iterator.hasPrevious()) iterator.previous()
      for (interval in iterator) {
        result.add(interval)
        if (interval.lines.first > logicalLines.last) break
      }
    }

  private fun failSafeCompute(
    factory: RNotebookCellInlayController.Factory,
    controllers: Collection<RNotebookCellInlayController>,
    interval: Interval,
  ): RNotebookCellInlayController? {
    try {
      return factory.compute(editor, controllers, interval)
    }
    catch (t: Throwable) {
      thisLogger().error("${factory.javaClass.name} shouldn't throw exceptions at RNotebookCellInlayController.Factory.compute(...)", t)
      return null
    }
  }

  private fun disposeAllInlays() {
    for (controllers in getMatchingInlaysForLines(0 until editor.document.lineCount)) {
      Disposer.dispose(controllers.inlay, false)
    }
  }

  companion object {
    private val LOG = logger<RNotebookCellInlayManager>()

    val R_NOTEBOOK_CELL_LINES_INTERVAL: DataKey<Interval> = DataKey.create("R_NOTEBOOK_CELL_LINES_INTERVAL")

    @JvmStatic
    fun install(editor: EditorImpl) {
      require(get(editor) == null)
      RNotebookCellInlayManager(editor).initialize()
    }

    @JvmStatic
    fun get(editor: Editor): RNotebookCellInlayManager? = key.get(editor)

    private val key = Key.create<RNotebookCellInlayManager>(RNotebookCellInlayManager::class.java.name)
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
        val fillX = if (editor.editorKind == EditorKind.DIFF && editor.isMirrored) x + 20 else x
        g.fillRect(fillX, y, width, height)
      }
    }
  }
}
