/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.BaseCodeInsightAction
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.packages.RequiredPackageException
import org.jetbrains.r.packages.RequiredPackageInstaller
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RFile
import org.jetbrains.r.rinterop.RRef
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

    try {
      val rInterop = file.runtimeInfo?.rInterop ?: return
      val viewer = rInterop.dataFrameGetViewer(RRef.expressionRef(expr, rInterop))
      RVisualizeTableUtil.showTable(project, viewer, expr)
    } catch (e: RDataFrameException) {
      HintManager.getInstance()
        .showErrorHint(editor, RBundle.message("visualize.table.action.error.hint", e.message.orEmpty()))
    }
    catch (e: RequiredPackageException) {
      RequiredPackageInstaller.getInstance(project)
        .installPackagesWithUserPermission(RBundle.message("visualize.table.action.error.utility.name"), e.missingPackages, null)
    }
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
}
