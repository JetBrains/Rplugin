/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring

import com.intellij.codeInsight.PsiEquivalenceUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.IntroduceTargetChooser
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.withoutParenthesis

abstract class RIntroduceLocalHandler : RefactoringActionHandler {
  override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
    if (editor == null) return
    if (file == null) return
    val operation = IntroduceOperation(project, editor, file)
    invokeOperation(operation)
  }

  fun invokeOperation(operation: IntroduceOperation) {
    val selectionModel = operation.editor.selectionModel
    if (selectionModel.hasSelection()) {
      val file = operation.file
      var selectionStart = file.findElementAt(selectionModel.selectionStart)
      var selectionEnd = file.findElementAt(selectionModel.selectionEnd - 1)
      if (selectionStart is PsiWhiteSpace) {
        selectionStart = file.findElementAt(selectionStart.textRange.endOffset)
      }
      if (selectionEnd is PsiWhiteSpace) {
        selectionEnd = file.findElementAt(selectionEnd.textRange.startOffset - 1)
      }
      if (selectionStart == null) return
      if (selectionEnd == null) return
      val result = PsiTreeUtil.getNonStrictParentOfType(PsiTreeUtil.findCommonParent(selectionStart, selectionEnd), RExpression::class.java)
        .withoutParenthesis()
      if (isValidIntroduceVariant(result)) {
        operation.expression = result as RExpression
        processExpression(operation)
      } else {
        showCannotPerformError(operation)
      }
    } else {
      val offset = operation.editor.caretModel.offset
      var element = operation.file.findElementAt(offset) ?: return showCannotPerformError(operation)
      val variants = mutableListOf<RExpression>()
      val end = PsiTreeUtil.getParentOfType(element, RBlockExpression::class.java, RFunctionExpression::class.java) ?: operation.file
      while (element != end) {
        val currentElement = (element as? RExpression).withoutParenthesis()
        if (currentElement != null &&
            (variants.isEmpty() || element !is RParenthesizedExpression) &&
            isValidIntroduceVariant(currentElement)) {
          variants.add(currentElement)
        }
        element = element.parent
      }

      if (variants.isEmpty()) return showCannotPerformError(operation)
      if (variants.size == 1) {
        operation.expression = variants[0]
        processExpression(operation)
      } else {
        IntroduceTargetChooser.showChooser(operation.editor, variants, object : Pass<RExpression>() {
          override fun pass(t: RExpression?) {
            if (t != null) {
              operation.expression = t
              processExpression(operation)
            }
          }
        }) { it.text }
      }
    }
  }

  private fun processExpression(operation: IntroduceOperation) {
    val expr = operation.expression
    operation.occurrences = findOccurrences(operation)
    if (operation.replaceAll != null) {
      processOccurrences(operation)
      return
    }

    if (operation.occurrences.size > 1) {
      OccurrencesChooser.simpleChooser<RExpression>(operation.editor).showChooser(
        expr, operation.occurrences,
        object : Pass<OccurrencesChooser.ReplaceChoice>() {
          override fun pass(choice: OccurrencesChooser.ReplaceChoice?) {
            operation.replaceAll = true
            processOccurrences(operation)
          }
        }
      )
    } else {
      operation.replaceAll = false
      processOccurrences(operation)
    }
  }

  private fun processOccurrences(operation: IntroduceOperation) {
    if (operation.replaceAll == false) {
      operation.occurrences = listOf(operation.expression)
    }

    operation.suggestedNames = getSuggestedNames(operation)
    val declarationIdentifier = performReplace(operation)

    if (operation.editor.settings.isVariableInplaceRenameEnabled) {
      val introducer = object : InplaceVariableIntroducer<PsiElement>(
        declarationIdentifier,
        operation.editor,
        operation.project,
        dialogTitle,
        operation.replacedOccurrences.toTypedArray(),
        null
      ) {
        override fun checkLocalScope() = operation.file
        override fun collectRefs(referencesSearchScope: SearchScope?) = mutableListOf(declarationIdentifier.reference) +
                                                                        operation.replacedOccurrences.map { it.reference }
      }
      introducer.performInplaceRefactoring(LinkedHashSet(operation.suggestedNames))
    }
  }

  protected abstract fun performReplace(operation: IntroduceOperation): RIdentifierExpression

  private fun findOccurrences(operation: IntroduceOperation): List<RExpression> {
    val expr = operation.expression
    val context = PsiTreeUtil.getParentOfType(expr, RFunctionExpression::class.java) ?: operation.file
    val occurrences = mutableListOf(expr)

    val visitor = object : RRecursiveElementVisitor() {
      override fun visitExpression(currentExpr: RExpression) {
        if (currentExpr == expr) {
          return
        }
        if (PsiEquivalenceUtil.areElementsEquivalent(currentExpr, expr)) {
          occurrences.add(currentExpr)
          return
        }
        super.visitExpression(currentExpr)
      }
    }

    if (context is RFunctionExpression) {
      context.expression?.accept(visitor)
    } else {
      context.acceptChildren(visitor)
    }
    return occurrences
  }

  private fun getSuggestedNames(operation: IntroduceOperation): List<String> {
    val expr = operation.expression.withoutParenthesis()
    val names = mutableListOf<String>()

    when (expr) {
      is RStringLiteralExpression -> {
        val s = expr.name
        if (s != null && RNamesValidator.isIdentifier(s)) {
          names.add(s)
        }
        names.add("s")
      }
      is RBooleanLiteral -> {
        names.add("value")
        names.add(if (expr.isTrue) "true" else "false")
      }
      is RNumericLiteralExpression -> {
        names.add("value")
      }
      is RNaLiteral -> names.add("na")
      is RNamespaceAccessExpression -> {
        val name = expr.identifier?.name
        if (name != null) {
          names.add(name)
        }
      }
      is RMemberExpression -> names.add(expr.tag)
      is RCallExpression -> names.add(expr.expression.text + "Result")
    }
    if (names.isEmpty()) {
      names.add("value")
    }

    val newNames = LinkedHashSet<String>()
    for (name in names) {
      for (index in 1..9) {
        val newName = if (index == 1) name else "$name$index"
        if (RNamesValidator.isIdentifier(newName) && !operation.occurrences.any { isNameUsed(newName, it) }) {
          newNames.add(newName)
          break
        }
      }
    }
    return newNames.toList()
  }

  private fun showCannotPerformError(operation: IntroduceOperation) {
    CommonRefactoringUtil.showErrorHint(operation.project, operation.editor,
                                        "Cannot perform refactoring using selected element(s)",
                                        dialogTitle, "refactoring.extractMethod")
  }

  protected abstract val dialogTitle: String

  protected open fun isValidIntroduceVariant(element: PsiElement?): Boolean {
    if (element !is RExpression) return false
    when (element) {
      is RAssignmentStatement -> return false
      is RNamedArgument -> return false
      is RBlockExpression -> return false
      is RBreakStatement -> return false
      is REmptyExpression -> return false
      is RForStatement -> return false
      is RFunctionExpression -> return false
      is RIdentifierExpression -> return false
      is RNextStatement -> return false
      is RParameter -> return false
      is RParameterList -> return false
      is RRepeatStatement -> return false
      is RWhileStatement -> return false
    }
    if (RPsiUtil.isReturn(element)) return false
    if (RPsiUtil.getAssignmentByAssignee(element) != null) return false
    if (RPsiUtil.getNamedArgumentByNameIdentifier(element) != null) return false
    return true
  }

  companion object {
    class IntroduceOperation(val project: Project, val editor: Editor, val file: PsiFile) {
      lateinit var expression: RExpression
      lateinit var occurrences: List<RExpression>
      lateinit var suggestedNames: List<String>
      lateinit var replacedOccurrences: List<RIdentifierExpression>
      var replaceAll: Boolean? = null
    }

    private fun isNameUsed(name: String, expression: RExpression): Boolean {
      val controlFlowHolder = PsiTreeUtil.getParentOfType(expression, RControlFlowHolder::class.java) ?: return false
      val variables = controlFlowHolder.getLocalVariableInfo(expression)?.variables ?: return false
      return name in variables
    }
  }
}
