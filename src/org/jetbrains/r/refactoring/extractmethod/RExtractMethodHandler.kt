/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.extractmethod

import com.intellij.codeInsight.codeFragment.CannotCreateCodeFragmentException
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.extractMethod.AbstractExtractMethodDialog
import com.intellij.refactoring.extractMethod.ExtractMethodDecorator
import com.intellij.refactoring.extractMethod.ExtractMethodValidator
import com.intellij.refactoring.util.AbstractVariableData
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.CommonRefactoringUtil.RefactoringErrorHintException
import org.jetbrains.r.RBundle
import org.jetbrains.r.RFileType
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isAssignee
import org.jetbrains.r.refactoring.quoteIfNeeded
import org.jetbrains.r.refactoring.rNamesValidator

class RExtractMethodHandler : RefactoringActionHandler {
  override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
    if (editor == null) return
    if (file !is RFile) return

    val codeFragment: RCodeFragment
    try {
      val expressions = getSelectedExpressions(editor, file) ?:
                        throw RefactoringErrorHintException("Cannot perform extract method using selected element(s)")
      try {
        codeFragment = RCodeFragment.createCodeFragment(expressions)
      } catch (e: CannotCreateCodeFragmentException) {
        throw RefactoringErrorHintException(e.message!!)
      }
    } catch (e: RefactoringErrorHintException) {
      CommonRefactoringUtil.showErrorHint(project, editor, e.message!!, RBundle.message("dialog.title.extract.method"), "refactoring.extractMethod")
      return
    }

    val functionName: String
    val variableData: Array<AbstractVariableData>
    if (ApplicationManager.getApplication().isUnitTestMode) {
      functionName = "foo"
      variableData = codeFragment.inputVariables.map {
        val data = AbstractVariableData()
        data.name = it
        data.originalName = it
        data.passAsParameter = true
        data
      }.toTypedArray()
    } else {
      val dialog = createDialog(project, file, codeFragment)
      dialog.show()
      if (!dialog.isOK) return
      functionName = dialog.methodName
      variableData = dialog.abstractVariableData
    }
    RExtractMethodUtil.performExtraction(project, editor, file, codeFragment, functionName, variableData)
  }

  companion object {
    private fun getSelectedExpressions(editor: Editor, file: RFile): List<RExpression>? {
      val selectionModel = editor.selectionModel
      if (!selectionModel.hasSelection()) {
        selectionModel.selectLineAtCaret()
      }

      var selectionStart = file.findElementAt(selectionModel.selectionStart) ?: return null
      var selectionEnd = file.findElementAt(selectionModel.selectionEnd - 1) ?: return null
      while (StringUtil.isEmptyOrSpaces(selectionStart.text) || selectionStart.text == ";") {
        if (selectionStart == selectionEnd) return null
        selectionStart = PsiTreeUtil.nextLeaf(selectionStart) ?: return null
      }
      while (StringUtil.isEmptyOrSpaces(selectionEnd.text) || selectionEnd.text == ";") {
        selectionEnd = PsiTreeUtil.prevLeaf(selectionEnd) ?: return null
      }

      val expressions: List<RExpression>
      val parent = PsiTreeUtil.findCommonParent(selectionStart, selectionEnd) ?: return null
      if (parent is RBlockExpression || parent is RFile) {
        val statementStart = PsiTreeUtil.findPrevParent(parent, selectionStart) as? RExpression
        val statementEnd = PsiTreeUtil.findPrevParent(parent, selectionEnd) as? RExpression
        expressions = if (statementStart == null || statementEnd == null) {
          listOf(parent as? RExpression ?: return null)
        } else if (statementStart == statementEnd) {
          listOf(statementStart)
        } else {
          val list = when (parent) {
            is RBlockExpression -> parent.expressionList
            is RFile -> parent.children.filterIsInstance(RExpression::class.java)
            else -> return null
          }
          val indexStart = list.indexOf(statementStart)
          val indexEnd = list.indexOf(statementEnd)
          list.subList(indexStart, indexEnd + 1)
        }
      } else {
        val expr = PsiTreeUtil.getNonStrictParentOfType(parent, RExpression::class.java) ?: return null
        val newExpr = if (expr.parent is RCallExpression &&
                          (expr is RIdentifierExpression || expr is RNamespaceAccessExpression || expr is RArgumentList)) {
          expr.parent as RExpression
        } else {
          expr
        }
        expressions = listOf(newExpr)
      }

      return if (expressions.all { isValidExpression(it) }) expressions else null
    }

    private fun createDialog(project: Project, file: RFile, codeFragment: RCodeFragment): AbstractExtractMethodDialog<Any> {
      val validator = object : ExtractMethodValidator {
        override fun check(name: String?): String? {
          if (name in codeFragment.controlFlowHolder.getLocalVariableInfo(codeFragment.entryPoint)!!.variables ||
              file.children.filterIsInstance<RNamedArgument>().any { it.name == name }) {
            return RBundle.message("dialog.message.name.clashes.with.already.existing.name")
          }
          return null
        }

        override fun isValidName(name: String?): Boolean =
          name != null && rNamesValidator.isIdentifier(name, project)
      }
      val decorator = ExtractMethodDecorator<Any> { settings ->
        val name = settings.methodName
        val parameters = settings.abstractVariableData.filter { it.passAsParameter }.map { it.name }
        "$name <- function(${parameters.joinToString(", ") { rNamesValidator.quoteIfNeeded(it, project) }})"
      }
      return object : AbstractExtractMethodDialog<Any>(
        project, "foo",
        codeFragment,
        arrayOf(), validator, decorator, RFileType
      ) {
      }
    }

    private fun isValidExpression(expr: RExpression): Boolean {
      when (expr) {
        is RArgumentList -> return false
        is RParameter -> return false
        is RParameterList -> return false
      }
      if (expr.isAssignee()) return false
      if (expr is RNamedArgument) return false
      return true
    }
  }
}
