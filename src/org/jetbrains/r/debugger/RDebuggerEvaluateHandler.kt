/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import com.intellij.xdebugger.impl.evaluate.XDebuggerEvaluationDialog
import org.jetbrains.r.RLanguage
import org.jetbrains.r.rmarkdown.RMarkdownLanguage
import org.jetbrains.r.run.debug.stack.RXDebuggerEvaluator

internal object RDebuggerEvaluateHandler {
  fun perform(project: Project, evaluator: RXDebuggerEvaluator, dataContext: DataContext) {
    RXDebuggerEvaluationDialog(project, evaluator, getSelectedExpression(project, dataContext)).show()
  }

  private fun getSelectedExpression(project: Project, dataContext: DataContext): XExpression {
    val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return XExpressionImpl.EMPTY_EXPRESSION
    val language = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)?.language
    if (language != RLanguage.INSTANCE && language != RMarkdownLanguage) return XExpressionImpl.EMPTY_EXPRESSION
    val text = editor.selectionModel.selectedText?.trim() ?: return XExpressionImpl.EMPTY_EXPRESSION
    return XExpressionImpl.fromText(text, if (text.lines().size > 1) EvaluationMode.CODE_FRAGMENT else EvaluationMode.EXPRESSION)
  }
}

private class RXDebuggerEvaluationDialog(project: Project, evaluator: RXDebuggerEvaluator, expression: XExpression) :
  XDebuggerEvaluationDialog(evaluator, project, RDebuggerEditorsProvider, expression, null,
                            expression.mode == EvaluationMode.CODE_FRAGMENT) {
  init {
    evaluator.registerParentDisposable(disposable)
  }
}
