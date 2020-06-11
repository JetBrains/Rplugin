/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.BaseCodeInsightAction
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
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

  override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
    return file.language == RLanguage.INSTANCE || file.language == RMarkdownLanguage
  }
}

class VisualizeTableHandler : CodeInsightActionHandler {
  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    if (file !is RFile) return
    val expr = getSelectedExpression(editor, file)?.text ?: return

    val rInterop = file.runtimeInfo?.rInterop ?: return
    val ref = RReference.expressionRef(expr, rInterop)
    visualizeTable(rInterop, ref, project, expr, editor)
  }

  private fun getSelectedExpression(editor: Editor, file: RFile): RExpression? {
    val selectionModel = editor.selectionModel
    if (!selectionModel.hasSelection()) return null

    var selectionStart = file.findElementAt(selectionModel.selectionStart) ?: return null
    var selectionEnd = file.findElementAt(selectionModel.selectionEnd - 1) ?: return null
    while (StringUtil.isEmptyOrSpaces(selectionStart.text) || selectionStart.text == ";") {
      if (selectionStart == selectionEnd) return null
      selectionStart = PsiTreeUtil.nextLeaf(selectionStart) ?: return null
    }
    while (StringUtil.isEmptyOrSpaces(selectionEnd.text) || selectionEnd.text == ";") {
      selectionEnd = PsiTreeUtil.prevLeaf(selectionEnd) ?: return null
    }

    val expr = PsiTreeUtil.findCommonParent(selectionStart, selectionEnd)
    return PsiTreeUtil.getNonStrictParentOfType(expr, RExpression::class.java)
  }

  companion object {
    fun visualizeTable(rInterop: RInterop, ref: RReference, project: Project, expr: String, editor: Editor? = null): Promise<Unit> {
      return rInterop.dataFrameGetViewer(ref).then {
        RVisualizeTableUtil.showTable(project, it, expr)
      }.onError {
        ApplicationManager.getApplication().invokeLater {
          when (it) {
            is RDataFrameException -> {
              if (editor != null) {
                HintManager.getInstance()
                  .showErrorHint(editor, RBundle.message("visualize.table.action.error.hint", it.message.orEmpty()))
              } else {
                RNotificationUtil.notifyExecutionError(
                  project, RBundle.message("visualize.table.action.error.hint", it.message.orEmpty()))
              }
            }
            is RInteropTerminated -> {
              if (editor != null) {
                HintManager.getInstance()
                  .showErrorHint(editor, RBundle.message("rinterop.terminated"))
              } else {
                RNotificationUtil.notifyExecutionError(
                  project, RBundle.message("rinterop.terminated"))
              }
            }
            is RequiredPackageException -> {
              RequiredPackageInstaller.getInstance(project).installPackagesWithUserPermission(
                RBundle.message("visualize.table.action.error.utility.name"), it.missingPackages)
            }
          }
        }
      }
    }

  }
}
