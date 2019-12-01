/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.r.psi.api.RFile
import javax.swing.Icon

abstract class REditorActionBase : DumbAwareAction, RPromotedAction {
  constructor() : super()

  constructor(text: String, description: String, icon: Icon?) : super(text, description, icon)

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = e.isFromActionToolbar || e.psiFile is RFile
    e.presentation.isEnabled = e.psiFile is RFile
  }
}

abstract class REditorRunActionBase : REditorActionBase {
  constructor() : super()

  constructor(text: String, description: String, icon: Icon?) : super(text, description, icon)

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabled = e.presentation.isEnabled && !REditorActionUtil.isRunningCommand(e.project)
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
