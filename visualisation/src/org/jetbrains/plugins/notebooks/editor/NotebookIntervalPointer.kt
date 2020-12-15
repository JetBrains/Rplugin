package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Key

/**
 * Pointer becomes invalid when code cell is edited
 * Invalid pointer returns null
 */
interface NotebookIntervalPointer {
  fun get(): NotebookCellLines.Interval?
}

private val key = Key.create<NotebookIntervalPointerFactory>(NotebookIntervalPointerFactory::class.java.name)

interface NotebookIntervalPointerFactory {
  /** pointer holds this interval or null if interval is obsolete */
  fun create(interval: NotebookCellLines.Interval): NotebookIntervalPointer

  companion object {
    fun get(editor: EditorImpl): NotebookIntervalPointerFactory =
      key.get(editor) ?: install(editor)

    private fun install(editor: EditorImpl): NotebookIntervalPointerFactory =
      NotebookIntervalPointerFactoryImpl(NotebookCellLines.get(editor)).also {
        key.set(editor, it)
      }
  }
}
