package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Key

/**
 * Pointer may become invalid and return null interval
 * Pointer becomes invalid when code cell is edited
 * There are no more than one pointer for each interval
 */
interface NotebookIntervalPointer {
  fun get(): NotebookCellLines.Interval?
}

private val key = Key.create<NotebookIntervalPointerFactory>(NotebookIntervalPointerFactory::class.java.name)

interface NotebookIntervalPointerFactory {
  /** same calls will return same pointer */
  fun create(ordinal: Int): NotebookIntervalPointer

  companion object {
    fun get(editor: EditorImpl): NotebookIntervalPointerFactory =
      key.get(editor) ?: install(editor)

    private fun install(editor: EditorImpl): NotebookIntervalPointerFactory =
      NotebookIntervalPointerFactoryImpl(NotebookCellLines.get(editor)).also {
        key.set(editor, it)
      }
  }
}
