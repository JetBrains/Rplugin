package org.jetbrains.r.visualization

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.ThreadingAssertions
import org.jetbrains.annotations.TestOnly
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import org.jetbrains.r.visualization.RNotebookIntervalPointersEvent.*

object RNotebookIntervalPointerFactoryImplProvider {
  fun create(project: Project, document: Document): RNotebookIntervalPointerFactory {
    val notebookCellLines = RNotebookCellLines.get(document)
    val factory = RNotebookIntervalPointerFactoryImpl(notebookCellLines,
                                                      DocumentReferenceManager.getInstance().create(document),
                                                      UndoManager.getInstance(project),
                                                      project)

    notebookCellLines.intervalListeners.addListener(factory)
    Disposer.register(project) {
      notebookCellLines.intervalListeners.removeListener(factory)
      RNotebookIntervalPointerFactory.key.set(document, null)
    }

    return factory
  }
}


private class RNotebookIntervalPointerImpl(@Volatile var interval: Interval?) : RNotebookIntervalPointer {
  override fun get(): Interval? = interval

  override fun toString(): String = "NotebookIntervalPointerImpl($interval)"
}


private typealias NotebookIntervalPointersEventChanges = ArrayList<Change>


private sealed interface ChangesContext

private data class DocumentChangedContext(var redoContext: RedoContext? = null) : ChangesContext
private data class UndoContext(val changes: List<Change>) : ChangesContext
private data class RedoContext(val changes: List<Change>) : ChangesContext

/**
 * One unique NotebookIntervalPointer exists for each current interval. You can use NotebookIntervalPointer as map key.
 * [NotebookIntervalPointerFactoryImpl] automatically supports undo/redo for [documentChanged] and [modifyPointers] calls.
 *
 * During undo or redo operations old intervals are restored.
 * For example, you can save pointer anywhere, remove interval, undo removal and pointer instance will contain interval again.
 * You can store interval-related data into WeakHashMap<NotebookIntervalPointer, Data> and this data will outlive undo/redo actions.
 */
class RNotebookIntervalPointerFactoryImpl(
  private val notebookCellLines: RNotebookCellLines,
  private val documentReference: DocumentReference,
  undoManager: UndoManager?,
  private val project: Project,
) : RNotebookIntervalPointerFactory, RNotebookCellLines.IntervalListener {
  private val pointers = ArrayList<RNotebookIntervalPointerImpl>()
  private var changesContext: ChangesContext? = null
  override val changeListeners: EventDispatcher<RNotebookIntervalPointerFactory.ChangeListener> =
    EventDispatcher.create(RNotebookIntervalPointerFactory.ChangeListener::class.java)

  init {
    pointers.addAll(notebookCellLines.snapshot.intervals.asSequence().map { RNotebookIntervalPointerImpl(it) })
  }

  private val validUndoManager: UndoManager? = undoManager
    get() = field?.takeIf { !project.isDisposed }

  override fun create(interval: Interval): RNotebookIntervalPointer {
    ThreadingAssertions.assertReadAccess()
    return pointers[interval.ordinal].also {
      require(it.interval == interval)
    }
  }

  override fun modifyPointers(changes: Iterable<RNotebookIntervalPointerFactory.Change>) {
    ThreadingAssertions.assertWriteAccess()

    val eventChanges = NotebookIntervalPointersEventChanges()
    applyChanges(changes, eventChanges)

    val pointerEvent = RNotebookIntervalPointersEvent(eventChanges, cellLinesEvent = null, RNotebookIntervalPointersEvent.EventSource.ACTION)

    validUndoManager?.undoableActionPerformed(object : BasicUndoableAction(documentReference) {
      override fun undo() {
        ThreadingAssertions.assertWriteAccess()
        val invertedChanges = invertChanges(eventChanges)
        updatePointersByChanges(invertedChanges)
        onUpdated(RNotebookIntervalPointersEvent(invertedChanges, cellLinesEvent = null, EventSource.UNDO_ACTION))
      }

      override fun redo() {
        ThreadingAssertions.assertWriteAccess()
        updatePointersByChanges(eventChanges)
        onUpdated(RNotebookIntervalPointersEvent(eventChanges, cellLinesEvent = null, EventSource.REDO_ACTION))
      }
    })

    onUpdated(pointerEvent)
  }

  override fun documentChanged(event: RNotebookCellLinesEvent) {
    ThreadingAssertions.assertWriteAccess()
    try {
      val pointersEvent = when (val context = changesContext) {
        is DocumentChangedContext -> documentChangedByAction(event, context)
        is UndoContext -> documentChangedByUndo(event, context)
        is RedoContext -> documentChangedByRedo(event, context)
        null -> documentChangedByAction(event, null) // changesContext is null if undo manager is unavailable
      }
      onUpdated(pointersEvent)
    }
    catch (ex: Exception) {
      thisLogger().error(ex)
      // DS-3893 consume exception and log it, actions changing document should work as usual
    }
    finally {
      changesContext = null
    }
  }

  override fun beforeDocumentChange(event: RNotebookCellLinesEventBeforeChange) {
    ThreadingAssertions.assertWriteAccess()
    val undoManager = validUndoManager
    if (undoManager == null || undoManager.isUndoOrRedoInProgress) return
    val context = DocumentChangedContext()
    try {
      undoManager.undoableActionPerformed(object : BasicUndoableAction() {
        override fun undo() {}

        override fun redo() {
          changesContext = context.redoContext
        }
      })
      changesContext = context
    }
    catch (ex: Exception) {
      thisLogger().error(ex)
      // DS-3893 consume exception, don't prevent document updating
    }
  }

  private fun documentChangedByAction(
    event: RNotebookCellLinesEvent,
    documentChangedContext: DocumentChangedContext?,
  ): RNotebookIntervalPointersEvent {
    val eventChanges = NotebookIntervalPointersEventChanges()

    updateChangedIntervals(event, eventChanges)
    updateShiftedIntervals(event)

    validUndoManager?.undoableActionPerformed(object : BasicUndoableAction(documentReference) {
      override fun undo() {
        changesContext = UndoContext(eventChanges)
      }

      override fun redo() {}
    })

    documentChangedContext?.let {
      it.redoContext = RedoContext(eventChanges)
    }

    return RNotebookIntervalPointersEvent(eventChanges, event, EventSource.ACTION)
  }

  private fun documentChangedByUndo(event: RNotebookCellLinesEvent, context: UndoContext): RNotebookIntervalPointersEvent {
    val invertedChanges = invertChanges(context.changes)
    updatePointersByChanges(invertedChanges)
    updateShiftedIntervals(event)
    return RNotebookIntervalPointersEvent(invertedChanges, event, EventSource.UNDO_ACTION)
  }

  private fun documentChangedByRedo(event: RNotebookCellLinesEvent, context: RedoContext): RNotebookIntervalPointersEvent {
    updatePointersByChanges(context.changes)
    updateShiftedIntervals(event)
    return RNotebookIntervalPointersEvent(context.changes, event, EventSource.REDO_ACTION)
  }

  private fun updatePointersByChanges(changes: List<Change>) {
    for (change in changes) {
      when (change) {
        is OnEdited -> (change.pointer as RNotebookIntervalPointerImpl).interval = change.intervalAfter
        is OnInserted -> {
          for (p in change.subsequentPointers) {
            (p.pointer as RNotebookIntervalPointerImpl).interval = p.interval
          }
          pointers.addAll(change.ordinals.first, change.subsequentPointers.map { it.pointer as RNotebookIntervalPointerImpl })
        }
        is OnRemoved -> {
          for (p in change.subsequentPointers.asReversed()) {
            pointers.removeAt(p.interval.ordinal)
            (p.pointer as RNotebookIntervalPointerImpl).interval = null
          }
        }
        is OnSwapped -> {
          trySwapPointers(null, RNotebookIntervalPointerFactory.Swap(change.firstOrdinal, change.secondOrdinal))
        }
      }
    }
  }

  private fun makeSnapshot(interval: Interval) =
    PointerSnapshot(pointers[interval.ordinal], interval)

  private fun hasSingleIntervalsWithSameTypeAndLanguage(
    oldIntervals: List<Interval>,
    newIntervals: List<Interval>,
  ): Boolean {
    val old = oldIntervals.singleOrNull() ?: return false
    val new = newIntervals.singleOrNull() ?: return false
    return old.type == new.type && old.language == new.language
  }

  private fun updateChangedIntervals(e: RNotebookCellLinesEvent, eventChanges: NotebookIntervalPointersEventChanges) {
    when {
      !e.isIntervalsChanged() -> {
        // content edited without affecting intervals values
        for (editedInterval in LinkedHashSet(e.oldAffectedIntervals) + e.newAffectedIntervals) {
          eventChanges.add(OnEdited(pointers[editedInterval.ordinal], editedInterval, editedInterval))
        }
      }
      hasSingleIntervalsWithSameTypeAndLanguage(e.oldIntervals, e.newIntervals) -> {
        // only one interval changed size
        for (editedInterval in e.newAffectedIntervals) {
          val ptr = pointers[editedInterval.ordinal]
          eventChanges.add(OnEdited(ptr, ptr.interval!!, editedInterval))
        }
        if (e.newIntervals.first() !in e.newAffectedIntervals) {
          val ptr = pointers[e.newIntervals.first().ordinal]
          eventChanges.add(OnEdited(ptr, ptr.interval!!, e.newIntervals.first()))
        }

        pointers[e.newIntervals.first().ordinal].interval = e.newIntervals.first()
      }
      else -> {
        if (e.oldIntervals.isNotEmpty()) {
          eventChanges.add(OnRemoved(e.oldIntervals.map(::makeSnapshot)))

          for (old in e.oldIntervals.asReversed()) {
            pointers[old.ordinal].interval = null
            pointers.removeAt(old.ordinal)
          }
        }

        if (e.newIntervals.isNotEmpty()) {
          pointers.addAll(e.newIntervals.first().ordinal, e.newIntervals.map { RNotebookIntervalPointerImpl(it) })
          eventChanges.add(OnInserted(e.newIntervals.map(::makeSnapshot)))
        }

        for (interval in e.newAffectedIntervals - e.newIntervals.toSet()) {
          val ptr = pointers[interval.ordinal]
          eventChanges.add(OnEdited(ptr, ptr.interval!!, interval))
        }
      }
    }
  }

  private fun updateShiftedIntervals(event: RNotebookCellLinesEvent) {
    val invalidPointersStart =
      event.newIntervals.firstOrNull()?.let { it.ordinal + event.newIntervals.size }
      ?: event.oldIntervals.firstOrNull()?.ordinal
      ?: pointers.size

    val currentIntervals = notebookCellLines.snapshot.intervals
    for (i in invalidPointersStart until pointers.size) {
      pointers[i].interval = currentIntervals[i]
    }
  }

  private fun applyChanges(changes: Iterable<RNotebookIntervalPointerFactory.Change>, eventChanges: NotebookIntervalPointersEventChanges) {
    for (hint in changes) {
      when (hint) {
        is RNotebookIntervalPointerFactory.Invalidate -> {
          val ptr = create(hint.interval) as RNotebookIntervalPointerImpl
          invalidatePointer(eventChanges, ptr)
        }
        is RNotebookIntervalPointerFactory.Swap ->
          trySwapPointers(eventChanges, hint)
      }
    }
  }

  private fun invalidatePointer(
    eventChanges: NotebookIntervalPointersEventChanges,
    ptr: RNotebookIntervalPointerImpl,
  ) {
    val interval = ptr.interval
    if (interval == null) return

    val newPtr = RNotebookIntervalPointerImpl(interval)
    pointers[interval.ordinal] = newPtr
    ptr.interval = null

    eventChanges.add(OnRemoved(listOf(PointerSnapshot(ptr, interval))))
    eventChanges.add(OnInserted(listOf(PointerSnapshot(newPtr, interval))))
  }

  private fun trySwapPointers(
    eventChanges: NotebookIntervalPointersEventChanges?,
    hint: RNotebookIntervalPointerFactory.Swap,
  ) {
    val firstPtr = pointers.getOrNull(hint.firstOrdinal)
    val secondPtr = pointers.getOrNull(hint.secondOrdinal)

    if (firstPtr == null || secondPtr == null) {
      thisLogger().error("cannot swap invalid NotebookIntervalPointers: ${hint.firstOrdinal} and ${hint.secondOrdinal}")
      return
    }

    if (hint.firstOrdinal == hint.secondOrdinal) return // nothing to do

    val interval = firstPtr.interval!!
    firstPtr.interval = secondPtr.interval
    secondPtr.interval = interval

    pointers[hint.firstOrdinal] = secondPtr
    pointers[hint.secondOrdinal] = firstPtr

    eventChanges?.add(OnSwapped(PointerSnapshot(firstPtr, firstPtr.interval!!),
                                PointerSnapshot(secondPtr, secondPtr.interval!!)))
  }

  private fun invertChanges(changes: List<Change>): List<Change> =
    changes.asReversed().map(::invertChange)

  private fun invertChange(change: Change): Change =
    when (change) {
      is OnEdited -> change.copy(intervalAfter = change.intervalBefore, intervalBefore = change.intervalAfter)
      is OnInserted -> OnRemoved(change.subsequentPointers)
      is OnRemoved -> OnInserted(change.subsequentPointers)
      is OnSwapped -> OnSwapped(first = PointerSnapshot(change.first.pointer, change.second.interval),
                                second = PointerSnapshot(change.second.pointer, change.first.interval))
    }

  private fun onUpdated(event: RNotebookIntervalPointersEvent) {
    safelyUpdate(changeListeners.multicaster, event)
    safelyUpdate(ApplicationManager.getApplication().messageBus.syncPublisher(RNotebookIntervalPointerFactory.ChangeListener.TOPIC), event)
  }

  private fun safelyUpdate(listener: RNotebookIntervalPointerFactory.ChangeListener, event: RNotebookIntervalPointersEvent) {
    try {
      listener.onUpdated(event)
    }
    catch (t: Throwable) {
      thisLogger().error("$listener shouldn't throw exceptions", t)
    }
  }

  @TestOnly
  fun pointersCount(): Int = pointers.size
}
