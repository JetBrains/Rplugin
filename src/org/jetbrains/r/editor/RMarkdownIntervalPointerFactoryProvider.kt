package org.jetbrains.r.editor

import com.intellij.openapi.editor.Editor
import org.jetbrains.plugins.notebooks.editor.NotebookCellLines
import org.jetbrains.plugins.notebooks.editor.NotebookIntervalPointerFactory
import org.jetbrains.plugins.notebooks.editor.NotebookIntervalPointerFactoryImpl
import org.jetbrains.plugins.notebooks.editor.NotebookIntervalPointerFactoryProvider

class RMarkdownIntervalPointerFactoryProvider : NotebookIntervalPointerFactoryProvider {
  override fun create(editor: Editor): NotebookIntervalPointerFactory =
    NotebookIntervalPointerFactoryImpl(NotebookCellLines.get(editor))
}