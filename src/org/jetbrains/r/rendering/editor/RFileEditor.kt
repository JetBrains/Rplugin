/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.editor

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.r.RBundle
import org.jetbrains.r.actions.RDumbAwareBgtAction
import org.jetbrains.r.actions.isVirtualFileForTest
import java.awt.BorderLayout

class RFileEditor(project: Project, textEditor: TextEditor, virtualFile: VirtualFile)
  : AdvancedTextEditor(project, textEditor, virtualFile) {
  init {
    val isTestable = isVirtualFileForTest(virtualFile, project)
    mainComponent.add(createREditorToolbar(isTestable).component, BorderLayout.NORTH)
  }

  override fun getName() = RBundle.message("editor.r.editor.name")

  private fun createREditorToolbar(isTestable: Boolean): ActionToolbar =
    ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, createActionGroup(isTestable), true).also {
      it.setTargetComponent(textEditor.editor.contentComponent)
    }

  private fun createActionGroup(isTestable: Boolean): ActionGroup = object : DefaultActionGroup() {
    private val basicActions = listOf<AnAction>(
      ToolbarAction("org.jetbrains.r.actions.RRunAction"),
      ToolbarAction("org.jetbrains.r.actions.RDebugAction"),
      ToolbarAction("org.jetbrains.r.console.jobs.RunRJobAction"),
      Separator(),
      ToolbarAction("org.jetbrains.r.actions.RunSelection"),
      ToolbarAction("org.jetbrains.r.actions.DebugSelection"))
    private val testsActions = listOf<AnAction>(
      Separator(),
      ToolbarAction("org.jetbrains.r.actions.CreateRTestFileAction"))
    private val helpActions = listOf<AnAction>(
      Separator(),
      ToolbarAction("org.jetbrains.r.actions.REditorHelpAction"))

    init {
      addAll(basicActions)
      if (isTestable) addAll(testsActions)
      addAll(helpActions)
    }
  }

  private inner class ToolbarAction(actionId: String) : RDumbAwareBgtAction() {
    private val action = ActionManager.getInstance().getAction(actionId).also { copyFrom(it) }

    override fun actionPerformed(e: AnActionEvent) {
      ActionUtil.performAction(action, createEvent(e))
    }

    override fun update(e: AnActionEvent) {
      action.update(createEvent(e))
    }

    private fun createEvent(e: AnActionEvent): AnActionEvent {
      val file = FileDocumentManager.getInstance().getFile(textEditor.editor.document)
      return AnActionEvent.createFromInputEvent(
        e.inputEvent, "", e.presentation,
        SimpleDataContext.builder()
          .add(CommonDataKeys.EDITOR, textEditor.editor)
          .add(CommonDataKeys.VIRTUAL_FILE, file)
          .add(CommonDataKeys.PSI_FILE, file?.let { PsiManager.getInstance(project).findFile(it) })
          .setParent(e.dataContext)
          .build())
    }
  }
}
