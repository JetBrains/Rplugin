package org.intellij.datavis.r.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.intellij.datavis.r.inlays.NotebookInlayComponent
import org.intellij.datavis.r.inlays.NotebookInlayComponentImpl
import org.intellij.datavis.r.inlays.components.GraphicsPanel
import org.intellij.datavis.r.inlays.components.ToolbarPane
import org.intellij.images.editor.ImageEditor
import org.intellij.images.editor.impl.ImageEditorImpl
import javax.swing.JComponent
import javax.swing.JPanel

class DefaultUiCustomizer : UiCustomizer {
  override fun createImageEditor(project: Project, file: VirtualFile, graphicsPanel: GraphicsPanel): ImageEditor =
    ImageEditorImpl(project, file)

  override fun toolbarPaneProgressComponentChanged(toolbarPane: ToolbarPane, component: JComponent?): Unit = Unit

  override fun toolbarPaneToolbarComponentChanged(toolbarPane: ToolbarPane, component: JComponent?): Unit = Unit

  override fun toolbarPaneMainPanelCreated(toolbarPane: ToolbarPane, panel: JPanel?): Unit = Unit

  override fun createNotebookInlayComponent(cell: PsiElement, editor: EditorImpl): NotebookInlayComponent =
    NotebookInlayComponentImpl(cell, editor)

  override val showUpdateCellSeparator: Boolean = true
}