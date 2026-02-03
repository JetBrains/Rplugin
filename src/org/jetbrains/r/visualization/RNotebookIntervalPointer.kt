package org.jetbrains.r.visualization

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import com.intellij.util.messages.Topic
import org.jetbrains.r.rmarkdown.RMarkdownVirtualFile
import org.jetbrains.r.visualization.RNotebookCellLines.Interval
import java.util.EventListener

/**
 * Pointer becomes invalid when code cell is removed.
 * It may become valid again when action is undone or redone.
 * Invalid pointer returns null.
 */
interface RNotebookIntervalPointer {
  /** thread-safe */
  fun get(): Interval?
}

interface RNotebookIntervalPointerFactory {
  /**
   * Interval should be valid, return pointer to it.
   */
  @RequiresReadLock
  fun create(interval: Interval): RNotebookIntervalPointer

  /**
   * Undo and redo will be added automatically.
   */
  @RequiresWriteLock
  fun modifyPointers(changes: Iterable<Change>)

  /**
   * listener shouldn't throw exceptions
   */
  interface ChangeListener : EventListener {
    fun onUpdated(event: RNotebookIntervalPointersEvent)

    companion object {
      val TOPIC: Topic<ChangeListener> =
        Topic.create("NotebookIntervalPointerFactory.ChangeListener", ChangeListener::class.java)
    }
  }

  /**
   * listen events for only one document
   */
  val changeListeners: EventDispatcher<ChangeListener>

  companion object {
    internal val key = Key.create<RNotebookIntervalPointerFactory>(RNotebookIntervalPointerFactory::class.java.name)

    fun get(editor: Editor): RNotebookIntervalPointerFactory =
      getOrNull(editor)!!

    fun get(project: Project, document: Document): RNotebookIntervalPointerFactory =
      getOrNull(project, document)!!

    fun getOrNull(editor: Editor): RNotebookIntervalPointerFactory? {
      val project = editor.project ?: return null
      return getOrNull(project, editor.document)
    }

    fun getOrNull(project: Project, document: Document): RNotebookIntervalPointerFactory? {
      return key.get(document) ?: tryInstall(project, document)
    }

    private fun tryInstall(project: Project, document: Document): RNotebookIntervalPointerFactory {
      require(RMarkdownVirtualFile.isRMarkdownOrQuarto(document))
      synchronized(this) { // prevent race during creation to avoid duplication
        return RNotebookIntervalPointerFactoryImplProvider.create(project, document).also {
          key.set(document, it)
        }
      }
    }
  }

  sealed interface Change

  /** invalidate pointer to interval, create new pointer */
  data class Invalidate(val interval: Interval) : Change

  /** swap two pointers */
  data class Swap(val firstOrdinal: Int, val secondOrdinal: Int) : Change
}
