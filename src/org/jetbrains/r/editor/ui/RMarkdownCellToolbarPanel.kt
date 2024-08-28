package org.jetbrains.r.editor.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.plugins.notebooks.ui.SteadyUIPanel
import org.jetbrains.plugins.notebooks.ui.visualization.notebookAppearance
import org.jetbrains.plugins.notebooks.visualization.NotebookIntervalPointer
import org.jetbrains.r.rendering.chunk.RunChunkActions
import java.awt.Cursor
import javax.swing.BoxLayout

internal class RMarkdownCellToolbarPanel(editor: EditorImpl, val pointer: NotebookIntervalPointer) : SteadyUIPanel(RMarkdownCellToolbarPanelUI(editor)) {
  init {
    isOpaque = false
    background = editor.notebookAppearance.getCodeCellBackground(editor.colorsScheme)
    layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

    val toolbar = ActionManager.getInstance().createActionToolbar("InlineToolbar", RunChunkActions.createToolbarActionGroup(), true)
    toolbar.setTargetComponent(this)
    add(toolbar.component)
    toolbar.component.isOpaque = false
    toolbar.component.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  }
}

internal val AnActionEvent.rMarkdownCellToolbarPanel: RMarkdownCellToolbarPanel?
  get() = getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? RMarkdownCellToolbarPanel