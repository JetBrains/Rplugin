/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import icons.org.jetbrains.r.RBundle
import org.jetbrains.annotations.TestOnly
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isFunctionFromLibrary
import org.jetbrains.r.psi.references.RReferenceBase
import org.jetbrains.r.rmarkdown.RMarkdownFileType
import org.jetbrains.r.skeleton.psi.RSkeletonBase

/**
 * In IJ codebase there seems to be a different inline impls for methods, parameters, consts etc.
 *
 *
 * Best too to complex example org.intellij.grammar.refactor.BnfInlineRuleActionHandler
 */
class RInlineAssignmentHandler : InlineActionHandler() {

  override fun isEnabledForLanguage(language: Language): Boolean {
    return language == RLanguage.INSTANCE
  }

  override fun canInlineElement(psiElement: PsiElement): Boolean {
    if (psiElement is RSkeletonBase) return false
    return psiElement is RAssignmentStatement || (psiElement is RIdentifierExpression && !RPsiUtil.isNamedArgument(psiElement))
  }

  override fun inlineElement(project: Project, editor: Editor?, psiElement: PsiElement) {
    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, psiElement)) return

    if (PsiTreeUtil.hasErrorElements(psiElement)) RInlineUtil.showErrorAndExit(project, editor, RBundle.message(
      "inline.assignment.handler.error.rule.with.errors.description")) { return }

    val targetElement = if (editor != null) TargetElementUtil.findTargetElement(editor, TargetElementUtil.getInstance().getAllAccepted()) else null
    if (targetElement is RIdentifierExpression && RPsiUtil.isNamedArgument(targetElement)) return

    val searchScope = GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), RFileType, RMarkdownFileType)
    if (ReferencesSearch.search(psiElement, searchScope).findFirst() == null) {
      RInlineUtil.showErrorAndExit(project, editor, RBundle.message(
        "inline.assignment.handler.error.rule.never.used.description")) { return }
    }

    // needed to allow for for current caret element only inlining
    var reference = if (editor != null) TargetElementUtil.findReference(editor) else null
    if (reference != null && reference.element.parent != psiElement) {
      val targets = (reference as RReferenceBase<*>).multiResolve(false).map { it.element }
      if (psiElement !in targets) reference = null
    }

    val assignmentStatement = psiElement as? RAssignmentStatement ?: psiElement.parent as RAssignmentStatement
    val name = assignmentStatement.name
    val controlFlow = RInlineUtil.getScope(assignmentStatement).controlFlow

    val lastDefs = RInlineUtil.getLatestDefs(controlFlow, name, reference?.element)
    if (lastDefs.size > 1) RInlineUtil.showErrorAndExit(project, editor, RBundle.message(
      "inline.assignment.handler.error.no.dominating.definition")) { return }

    val realAssignmentStatement = lastDefs.firstOrNull() as? RAssignmentStatement ?: assignmentStatement
    var refElement = reference?.element as? RIdentifierExpression

    // To prevent inlining left part of assignments
    if (refElement == (refElement?.parent as? RAssignmentStatement)?.assignee) {
      refElement = null
    }

    val isFunction = realAssignmentStatement.isFunctionDeclaration
    if (isFunction) {
      val errorMessage = when {
        hasFunctionInside(realAssignmentStatement) -> RBundle.message("inline.assignment.handler.error.fun.in.fun")
        hasRecursiveCall(realAssignmentStatement) -> RBundle.message("inline.assignment.handler.error.fun.self.ref")
        hasInterruptIfs(project, realAssignmentStatement) -> RBundle.message("inline.assignment.handler.error.interrupts.flow")
        hasUseMethod(realAssignmentStatement) -> RBundle.message("inline.assignment.handler.error.has.use.method")
        else -> null
      }
      if (errorMessage != null) {
        RInlineUtil.showErrorAndExit(project, editor, errorMessage) { return }
      }
    }

    if (ApplicationManager.getApplication().isUnitTestMode) {
      RInlineAssignmentProcessor(project, editor, realAssignmentStatement, refElement, inlineThisOnly, isFunction, removeDefinition).run()
    }
    else {
      RInlineAssignmentDialog(project, editor, isFunction, realAssignmentStatement, refElement).show()
    }
  }

  private fun hasRecursiveCall(function: RAssignmentStatement): Boolean {
    return SyntaxTraverser.psiTraverser((function.assignedValue as RFunctionExpression).expression)
      .any { element ->
        if (element !is RCallExpression) return@any false
        val name = (element.expression as? RIdentifierExpression)?.name ?: return@any false
        if (name != function.name) return@any false
        val reference = element.expression.reference ?: return@any false
        val resolves = reference.multiResolve(false).map { it.element }
        resolves.any { it == function }
      }
  }

  private fun hasFunctionInside(function: RAssignmentStatement): Boolean {
    return RInlineUtil.collectAssignments(function.assignedValue).any { it.isFunctionDeclaration }
  }

  private fun hasUseMethod(function: RAssignmentStatement): Boolean {
    var result = false
    function.assignedValue?.acceptChildren(object : RInlineUtil.RRecursiveElementVisitor() {
      override fun visitCallExpression(o: RCallExpression) {
        if (result) return
        if (!o.isFunctionFromLibrary("UseMethod", "base")) return
        result = true
      }
    })
    return result
  }

  private fun hasInterruptIfs(project: Project, function: RAssignmentStatement): Boolean {
    val returns = RInlineUtil.collectReturns(project, function.assignedValue as RFunctionExpression).map { it.returnStatement }
    val cache = mutableSetOf<RIfStatement>()
    return returns
      .mapNotNull { PsiTreeUtil.getParentOfType(it, RIfStatement::class.java, true, RFunctionExpression::class.java) }
      .distinct()
      .any { checkInterruptsControlFlow(it, cache) }
  }

  private fun checkInterruptsControlFlow(ifStatement: RIfStatement, cache: MutableSet<RIfStatement>): Boolean {
    if (ifStatement in cache) return false
    cache.add(ifStatement)
    val ifPart = ifStatement.ifBody ?: return true
    val elsePart = ifStatement.elseBody ?: return true

    if (checkLastStatement(ifPart, cache)) return true
    if (checkLastStatement(elsePart, cache)) return true

    val parentIfStatement = PsiTreeUtil.getParentOfType(ifStatement, RIfStatement::class.java, true, RFunctionExpression::class.java)
    if (parentIfStatement != null && checkInterruptsControlFlow(parentIfStatement, cache)) return true
    return false
  }

  private fun checkLastStatement(statement: RExpression, cache: MutableSet<RIfStatement>): Boolean {
    val last = if (statement is RBlockExpression) statement.expressionList.lastOrNull() else statement
    return last is RIfStatement && checkInterruptsControlFlow(last, cache)
  }

  @TestOnly
  fun withInlineThisOnly(inlineThisOnly: Boolean): RInlineAssignmentHandler {
    val res = RInlineAssignmentHandler()
    res.removeDefinition = removeDefinition
    res.inlineThisOnly = inlineThisOnly
    return res
  }

  @TestOnly
  fun withRemoveDefinition(removeDefinition: Boolean): RInlineAssignmentHandler {
    val res = RInlineAssignmentHandler()
    res.inlineThisOnly = inlineThisOnly
    res.removeDefinition = removeDefinition
    return res
  }

  @TestOnly
  private var inlineThisOnly: Boolean = false
  @TestOnly
  private var removeDefinition: Boolean = false
}
