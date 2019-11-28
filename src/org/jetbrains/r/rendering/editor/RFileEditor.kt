/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.editor

import com.intellij.diff.util.FileEditorBase
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiManager
import java.awt.BorderLayout
import javax.swing.JPanel

class RFileEditor(val project: Project, private val editor: TextEditor, val virtualFile: VirtualFile) : FileEditorBase(), TextEditor {
  private val toolbarComponent = createREditorToolbar().component
  private val editorComponent = editor.component
  private val mainComponent = JPanel(BorderLayout())

  init {
    mainComponent.add(toolbarComponent, BorderLayout.NORTH)
    mainComponent.add(editorComponent, BorderLayout.CENTER)
  }

  override fun getComponent() = mainComponent
  override fun getName() = "R Editor"
  override fun getPreferredFocusedComponent() = editor.preferredFocusedComponent

  override fun dispose() {
    TextEditorProvider.getInstance().disposeEditor(editor)
    super.dispose()
  }

  override fun getStructureViewBuilder(): StructureViewBuilder? = editor.structureViewBuilder
  override fun getEditor(): Editor = editor.editor
  override fun navigateTo(navigatable: Navigatable) = editor.navigateTo(navigatable)
  override fun canNavigateTo(navigatable: Navigatable): Boolean = editor.canNavigateTo(navigatable)

  private fun createREditorToolbar(): ActionToolbar =
    ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, createActionGroup(), true).also {
      it.setTargetComponent(editor.editor.contentComponent)
    }

  private fun createActionGroup(): ActionGroup = DefaultActionGroup(
    ToolbarAction("org.jetbrains.r.actions.RRunAction"),
    ToolbarAction("org.jetbrains.r.actions.RDebugAction"),
    Separator(),
    ToolbarAction("org.jetbrains.r.actions.RunSelection"),
    ToolbarAction("org.jetbrains.r.actions.DebugSelection"))

  private inner class ToolbarAction(actionId: String) : AnAction() {
    private val action = ActionManager.getInstance().getAction(actionId).also { copyFrom(it) }

    override fun actionPerformed(e: AnActionEvent) {
      action.actionPerformed(createEvent(e))
    }

    override fun update(e: AnActionEvent) {
      action.update(createEvent(e))
    }

    private fun createEvent(e: AnActionEvent): AnActionEvent {
      val file = FileDocumentManager.getInstance().getFile(editor.editor.document)
      return AnActionEvent.createFromInputEvent(
        e.inputEvent, "", e.presentation,
        SimpleDataContext.getSimpleContext(
          mapOf(CommonDataKeys.EDITOR.name to editor.editor,
                CommonDataKeys.VIRTUAL_FILE.name to file,
                CommonDataKeys.PSI_FILE.name to file?.let { PsiManager.getInstance(project).findFile(it) }),
          e.dataContext))
    }
  }
}
