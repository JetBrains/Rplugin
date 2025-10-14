/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.BaseCodeInsightAction
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.RPluginCoroutineScope
import com.intellij.r.psi.psi.api.RExpression
import com.intellij.r.psi.psi.api.RFile
import com.intellij.r.psi.rinterop.RReference
import com.intellij.r.psi.rmarkdown.RMarkdownLanguage
import com.intellij.r.psi.run.visualize.RVisualization
import kotlinx.coroutines.launch
import org.jetbrains.r.console.runtimeInfo

class VisualizeTableAction : BaseCodeInsightAction() {
  override fun getHandler() = VisualizeTableHandler()

  override fun isValidForFile(project: Project, editor: Editor, psiFile: PsiFile): Boolean {
    return psiFile.language == RLanguage.INSTANCE || psiFile.language == RMarkdownLanguage
  }
}

class VisualizeTableHandler : CodeInsightActionHandler {
  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
    if (psiFile !is RFile) return
    val expr = getSelectedExpression(editor, psiFile)?.text ?: return

    val rInterop = psiFile.runtimeInfo?.rInterop ?: return
    val ref = RReference.expressionRef(expr, rInterop)

    RPluginCoroutineScope.getScope(project).launch(ModalityState.defaultModalityState().asContextElement()) {
      RVisualization.getInstance(project).visualizeTable(rInterop, ref, expr)
    }
  }

  private fun getSelectedExpression(editor: Editor, file: RFile): RExpression? {
    val selectionModel = editor.selectionModel
    if (!selectionModel.hasSelection()) return null

    var selectionStart = file.findElementAt(selectionModel.selectionStart) ?: return null
    var selectionEnd = file.findElementAt(selectionModel.selectionEnd - 1) ?: return null
    while (selectionStart.text.isNullOrBlank() || selectionStart.text == ";") {
      if (selectionStart == selectionEnd) return null
      selectionStart = PsiTreeUtil.nextLeaf(selectionStart) ?: return null
    }
    while (selectionEnd.text.isNullOrBlank() || selectionEnd.text == ";") {
      selectionEnd = PsiTreeUtil.prevLeaf(selectionEnd) ?: return null
    }

    val expr = PsiTreeUtil.findCommonParent(selectionStart, selectionEnd)
    return PsiTreeUtil.getNonStrictParentOfType(expr, RExpression::class.java)
  }
}
