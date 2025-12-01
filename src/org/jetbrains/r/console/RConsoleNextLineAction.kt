/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.r.actions.RPromotedAction
import org.jetbrains.r.actions.editor

class RConsoleNextLineAction : AnAction(), RPromotedAction {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor as? EditorEx ?: return
    val project = e.project ?: return
    RConsoleEnterHandler.executeEnterHandler(project, editor)
  }

  override fun update(e: AnActionEvent) {
    val editor = e.editor ?: return
    val document = editor.document
    val project = e.project ?: return
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
    val presentation = e.presentation
    presentation.isEnabled = psiFile?.getUserData(RConsoleViewImpl.IS_R_CONSOLE_KEY) == true
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
