/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import javax.swing.Icon

abstract class REditorActionBase : DumbAwareAction(), RPromotedAction {
  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = e.isFromActionToolbar || e.psiFile is RFile || e.virtualFile?.fileType == RMarkdownFileType
    e.presentation.isEnabled = e.psiFile is RFile || e.virtualFile?.fileType == RMarkdownFileType
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

class REditorHelpAction : REditorActionBase() {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.editor ?: return
    val elementAtCaret = PsiUtilBase.getElementAtCaret(editor) ?: return
    RToolWindowFactory.showDocumentation(elementAtCaret)
  }
}

val AnActionEvent.psiFile: PsiFile?
  get() = getData(CommonDataKeys.PSI_FILE)

val AnActionEvent.caret: Caret?
  get() = getData(CommonDataKeys.CARET)

val AnActionEvent.virtualFile: VirtualFile?
  get() = getData(CommonDataKeys.VIRTUAL_FILE)

val AnActionEvent.editor: Editor?
  get() = getData(CommonDataKeys.EDITOR)
