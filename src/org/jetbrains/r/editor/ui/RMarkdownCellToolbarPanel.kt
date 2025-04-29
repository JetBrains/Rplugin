package org.jetbrains.r.editor.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.editor.impl.EditorImpl
import org.jetbrains.r.editor.RMarkdownEditorAppearance
import org.jetbrains.r.rendering.chunk.RunChunkActions
import org.jetbrains.r.visualization.RNotebookIntervalPointer
import java.awt.Cursor
import javax.swing.BoxLayout

internal class RMarkdownCellToolbarPanel(editor: EditorImpl, val pointer: RNotebookIntervalPointer) : SteadyUIPanel(RMarkdownCellToolbarPanelUI(editor)) {
  init {
    isOpaque = false
    background = RMarkdownEditorAppearance.getCodeCellBackgroundColor(editor.colorsScheme)
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
