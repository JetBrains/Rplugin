/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.BaseCodeInsightAction
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.coroutines.launch
import org.jetbrains.concurrency.await
import org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RPluginCoroutineScope
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.notifications.RNotificationUtil
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropTerminated
import org.jetbrains.r.rinterop.RReference
import org.jetbrains.r.rmarkdown.RMarkdownLanguage

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
      visualizeTable(rInterop, ref, project, expr, editor)
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

  companion object {
    suspend fun visualizeTable(rInterop: RInterop, ref: RReference, project: Project, expr: String, editor: Editor? = null) {
      val viewer = try {
        rInterop.dataFrameGetViewer(ref).await()
      }
      catch (exception: RDataFrameException) {
        notifyError(project, editor, RBundle.message("visualize.table.action.error.hint", exception.message.orEmpty()))
        logger<VisualizeTableAction>().warn(exception)
        null
      }
      catch (exception: RInteropTerminated) {
        notifyError(project, editor, RBundle.message("rinterop.terminated"))
        logger<VisualizeTableAction>().warn(exception)
        null
      }
      catch (exception: RequiredPackageException) {
        RequiredPackageInstaller.getInstance(project).installPackagesWithUserPermission(RBundle.message("visualize.table.action.error.utility.name"), exception.missingPackages)
        logger<VisualizeTableAction>().warn(exception)
        null
      }

      if (viewer != null) {
        RVisualizeTableUtil.showTableAsync(project, viewer, expr)
      }
    }

    private fun notifyError(project: Project, editor: Editor?, @NlsSafe errorMsg: String) {
      if (editor != null) HintManager.getInstance().showErrorHint(editor, errorMsg)
      else RNotificationUtil.notifyExecutionError(project, errorMsg)
    }
  }
}
