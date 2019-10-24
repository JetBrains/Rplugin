/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.r.psi.api.RFile
import javax.swing.Icon

abstract class REditorActionBase(text: String, description: String, icon: Icon?) : AnAction(text, description, icon), RPromotedAction {
  override fun update(event: AnActionEvent) {
    event.presentation.isEnabledAndVisible = event.psiFile is RFile
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