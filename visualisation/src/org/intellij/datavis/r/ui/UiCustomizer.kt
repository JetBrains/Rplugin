package org.intellij.datavis.r.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.intellij.datavis.r.inlays.NotebookInlayComponent
import org.intellij.datavis.r.inlays.components.GraphicsPanel
import org.intellij.datavis.r.inlays.components.ToolbarPane
import org.intellij.images.editor.ImageEditor
import javax.swing.JComponent
import javax.swing.JPanel

interface UiCustomizer {
  fun createImageEditor(project: Project,
                        file: VirtualFile,
                        graphicsPanel: GraphicsPanel): ImageEditor

  fun toolbarPaneToolbarComponentChanged(toolbarPane: ToolbarPane, component: JComponent?)

  fun toolbarPaneProgressComponentChanged(toolbarPane: ToolbarPane, component: JComponent?)

  fun toolbarPaneMainPanelCreated(toolbarPane: ToolbarPane, panel: JPanel?)

  fun createNotebookInlayComponent(cell: PsiElement, editor: EditorImpl): NotebookInlayComponent

  companion object {
    val EP_NAME: ExtensionPointName<UiCustomizer> = ExtensionPointName("org.intellij.datavis.r.inlays.visualisation.uiCustomizer")

    val instance: UiCustomizer get() = EP_NAME.extensionList.first()
  }
}