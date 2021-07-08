package org.jetbrains.plugins.notebooks.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

val EDITOR_SCROLLING_POSITION_KEEPER_KEY = Key.create<EditorScrollingPositionKeeper>("EditorScrollingPositionKeeper")

interface NotebookCellEditorScrollingPositionKeeper {
  fun savePosition()

  fun savePosition(targetLine: Int?)

  fun restorePosition(stopAnimation: Boolean)

  fun clearScrollingPosition(editor: Editor)
}

val Editor.notebookCellEditorScrollingPositionKeeper
  get() = getUserData(EDITOR_SCROLLING_POSITION_KEEPER_KEY) as? NotebookCellEditorScrollingPositionKeeper

fun saveScrollingPosition(virtualFile: VirtualFile, project: Project) {
  val fileEditors = FileEditorManager.getInstance(project).getAllEditors(virtualFile)
  val editors = fileEditors.filterIsInstance<TextEditor>().map { it.editor }
  for (editor in editors) {
    editor.notebookCellEditorScrollingPositionKeeper?.savePosition()
  }
}

fun saveScrollingPosition(editor: Editor, targetCell: NotebookCellLines.Interval?) {
  editor.notebookCellEditorScrollingPositionKeeper?.savePosition(targetCell?.lines?.first)
}
