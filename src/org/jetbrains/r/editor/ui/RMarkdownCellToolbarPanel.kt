package org.jetbrains.r.editor.ui

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.ui.SteadyUIPanel
import org.jetbrains.plugins.notebooks.ui.visualization.notebookAppearance
import org.jetbrains.plugins.notebooks.visualization.NotebookIntervalPointer
import org.jetbrains.r.rendering.chunk.RunChunkNavigator
import java.awt.Cursor
import javax.swing.BoxLayout

internal class RMarkdownCellToolbarPanel(editor: EditorImpl, pointer: NotebookIntervalPointer) : SteadyUIPanel(RMarkdownCellToolbarPanelUI(editor)) {
  init {
    isOpaque = false
    background = editor.notebookAppearance.getCodeCellBackground(editor.colorsScheme)
    layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

    val toolbar = ActionManager.getInstance().createActionToolbar("InlineToolbar", createToolbarActionGroup(editor, pointer), true)
    toolbar.setTargetComponent(this)
    add(toolbar.component)
    toolbar.component.isOpaque = false
    toolbar.component.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  }

  private fun createToolbarActionGroup(editor: Editor, pointer: NotebookIntervalPointer): ActionGroup {
    val actions = RunChunkNavigator.createChunkToolbarActionsList(pointer, editor)
    return DefaultActionGroup(actions)
  }
}