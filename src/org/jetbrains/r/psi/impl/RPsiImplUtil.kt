// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.impl

import com.google.common.base.CharMatcher
import com.google.common.base.Joiner
import com.google.common.collect.Lists
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.IncorrectOperationException
import org.jetbrains.r.RElementGenerator
import org.jetbrains.r.classes.r6.R6ClassInfo
import org.jetbrains.r.classes.r6.R6ClassInfoUtil
import org.jetbrains.r.classes.s4.RS4ClassInfo
import org.jetbrains.r.classes.s4.RS4ClassInfoUtil
import org.jetbrains.r.parsing.RElementTypes.*
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RElementFilters
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.cfg.RControlFlow
import org.jetbrains.r.psi.cfg.buildControlFlow
import org.jetbrains.r.psi.getParameters
import org.jetbrains.r.psi.isNamespaceAccess
import org.jetbrains.r.psi.references.*
import org.jetbrains.r.refactoring.RNamesValidator
import java.util.*

/**
 * Should be used for implementing methods of generated PSI classes
 */
internal object RPsiImplUtil {
  const val UNNAMED = "<unnamed>"

  private val LEFT_ASSIGNMENTS = TokenSet.create(R_LEFT_ASSIGN, R_LEFT_COMPLEX_ASSIGN)

  private val RIGHT_ASSIGNMENTS = TokenSet.create(R_RIGHT_ASSIGN, R_RIGHT_COMPLEX_ASSIGN)

  private val CLOSURE_ASSIGNMENTS = TokenSet.create(R_LEFT_COMPLEX_ASSIGN, R_RIGHT_COMPLEX_ASSIGN)

  @JvmStatic
  fun getName(binaryOperator: ROperator): String {
    return binaryOperator.text
  }

  @JvmStatic
  fun isLeft(assignment: RAssignmentStatement): Boolean {
    val operator = assignment.node.findChildByType(LEFT_ASSIGNMENTS)
    return operator != null
  }

  @JvmStatic
  fun isEqual(assignment: RAssignmentStatement): Boolean {
    return assignment.children.firstOrNull { it is ROperator }?.node?.findChildByType(R_EQ) != null
  }

  @JvmStatic
  fun getTag(memberExpression: RMemberExpression): String {
    return getTagImpl(memberExpression.leftExpr)
  }

  @JvmStatic
  fun isRight(assignment: RAssignmentStatementImpl): Boolean {
    return assignment.greenStub?.isRight() ?: (assignment.node.findChildByType(RIGHT_ASSIGNMENTS) != null)
  }

  @JvmStatic
  fun getAssignedValue(assignment: RAssignmentStatement): RExpression? {
    return if (assignment.isRight) findFirstExpression(assignment) else findLastExpression(assignment)
  }

  @JvmStatic
  fun getAssignee(assignment: RAssignmentStatement): RExpression? {
    return if (assignment.isRight) findLastExpression(assignment) else findFirstExpression(assignment)
  }

  @JvmStatic
  fun setName(assignment: RAssignmentStatement, name: String): PsiElement {
    val nameIdentifier = assignment.nameIdentifier ?: throw IncorrectOperationException("Empty name: " + this)
    nameIdentifier.setName(name)
    return assignment
  }

  @JvmStatic
  fun getName(assignment: RAssignmentStatementImpl): String {
    return assignment.greenStub?.name ?: assignment.nameIdentifier?.name ?: UNNAMED
  }

  @JvmStatic
  fun getNameIdentifier(assignment: RAssignmentStatement): PsiNamedElement? {
    return getAssignee(assignment) as? PsiNamedElement
  }

  @JvmStatic
  fun getName(namedArgument: RNamedArgument): String {
    return getNameIdentifier(namedArgument)?.name ?: UNNAMED
  }

  @JvmStatic
  fun getNameIdentifier(namedArgument: RNamedArgument): PsiNamedElement? {
    return PsiTreeUtil.getChildOfAnyType(namedArgument, RIdentifierExpression::class.java, RStringLiteralExpression::class.java)
  }

  @JvmStatic
  fun isFunctionDeclaration(assignment: RAssignmentStatementImpl): Boolean {
    return assignment.greenStub?.isFunctionDeclaration ?: (assignment.assignedValue is RFunctionExpression)
  }

  @JvmStatic
  fun isPrimitiveFunctionDeclaration(assignment: RAssignmentStatementImpl): Boolean {
    return false
  }

  @JvmStatic
  fun getFunctionParameters(assignment: RAssignmentStatementImpl): String {
    if (isFunctionDeclaration(assignment)) {
      (assignment.assignedValue as? RFunctionExpression)?.parameterList?.text?.let {
        return it
      }
    }
    return ""
  }

  @JvmStatic
  fun getParameterNameList(assignment: RAssignmentStatementImpl): List<String> {
    return if (isFunctionDeclaration(assignment)) {
      assignment.getParameters().map { it.name }
    }
    else {
      emptyList()
    }
  }

  @JvmStatic
  fun isClosureAssignment(assignment: RAssignmentStatement): Boolean {
    return PsiTreeUtil.findChildOfType(assignment, ROperator::class.java)?.node?.findChildByType(CLOSURE_ASSIGNMENTS) != null
  }

  @JvmStatic
  fun getName(stringLiteral: RStringLiteralExpressionImpl): String? {
    val text = stringLiteral.text

    rowStringMinusNumber(text)?.let {
      return text.substring(3 + it, text.length - (2 + it))
    }
    if (text.length >= 2) {
      return text.substring(1, text.length - 1)
    }
    return null
  }

  /** This method relay on correct [string] (It should be checked in lexer already) */
  private fun rowStringMinusNumber(string: String): Int? {
    if (string[0].toLowerCase() != 'r') return null

    var minusNumber = 0
    while(string[2 + minusNumber] == '-') minusNumber++
    return minusNumber
  }

  @JvmStatic
  fun setName(stringLiteral: RStringLiteralExpressionImpl, name: String): PsiElement {
    val text = stringLiteral.text
    if (text.length < 2) {
      throw IncorrectOperationException("incorrect string literal: " + text)
    }
    val result = rowStringMinusNumber(text)?.let {
      text.replaceRange(3 + it, text.length - (2 + it), name)
    } ?: text[0].let {
      it + name + it
    }
    val replacement = RElementFactory.createRPsiElementFromText(stringLiteral.project, result)
    return stringLiteral.replace(replacement)
  }

  @JvmStatic
  fun setName(expression: RIdentifierExpression, name: String): PsiElement {
    val oldNameIdentifier = expression.node.findChildByType(R_IDENTIFIER)
    if (oldNameIdentifier != null) {
      renameIdentifier(name, expression, oldNameIdentifier)
    }
    return expression
  }

  @JvmStatic
  fun setName(parameter: RParameter, name: String): PsiElement {
    val nameIdentifier = parameter.nameIdentifier ?: throw IncorrectOperationException("Empty name: " + this)
    nameIdentifier.name = name
    return parameter
  }

  @JvmStatic
  fun setName(namedArgument: RNamedArgument, name: String): PsiElement {
    val nameIdentifier = namedArgument.nameIdentifier ?: throw IncorrectOperationException("Empty name: " + this)
    nameIdentifier.setName(name)
    return namedArgument
  }

  @JvmStatic
  fun setName(infixOperator: RInfixOperator, name: String): PsiElement {
    val dummyExpression = RElementFactory.buildRFileFromText(infixOperator.project, "dummy $name dummy")
    val newOperator = PsiTreeUtil.findChildOfType(dummyExpression, RInfixOperator::class.java) ?: return infixOperator
    return infixOperator.replace(newOperator)
  }

  @JvmStatic
  fun getName(parameter: RParameterImpl): String {
    parameter.greenStub?.let { return it.name }
    return parameter.nameIdentifier?.name ?: UNNAMED
  }

  @JvmStatic
  fun getNameIdentifier(parameter: RParameter): RIdentifierExpression? {
    return getVariable(parameter)
  }

  @JvmStatic
  fun getVariable(parameter: RParameter): RIdentifierExpression? {
    return PsiTreeUtil.getChildOfType(parameter, RIdentifierExpression::class.java)
  }

  @JvmStatic
  fun getDefaultValue(parameter: RParameter): RExpression? {
    return if (parameter.expressionList.size > 1) parameter.expressionList[1] else null
  }

  @JvmStatic
  fun getReference(expression: RExpression): RReferenceBase<*>? = null

  @JvmStatic
  fun getReference(binaryOperator: ROperator): ROperatorReference {
    return ROperatorReference(binaryOperator)
  }

  @JvmStatic
  fun getReference(identifierExpression: RIdentifierExpression): RReferenceBase<*> {
    if (RElementFilters.IMPORT_FILTER.isAcceptable(null, identifierExpression)) {
      return RImportReference(identifierExpression)
    }
    if (identifierExpression.isNamespaceAccess()) {
      return RNamespaceReference(identifierExpression, (identifierExpression.parent as RNamespaceAccessExpression).namespaceName)
    }
    return RReferenceImpl(identifierExpression)
  }

  @JvmStatic
  fun isSingle(subscription: RSubscriptionExpression) = subscription.node.lastChildNode.elementType === R_RBRACKET

  @JvmStatic
  fun getDocStringValue(functionExpression: RFunctionExpression): String? {
    val roxyComment = StringUtil.nullize(getRoxygenComment(functionExpression))
    if (roxyComment != null) {
      return roxyComment
    }

    val blockExpression = PsiTreeUtil.findChildOfType(functionExpression, org.jetbrains.r.psi.api.RBlockExpression::class.java) ?: return null

    val comments = ArrayList<PsiComment>()
    var sibling: PsiElement? = blockExpression.firstChild
    while (sibling != null && sibling !is RExpression) {
      if (sibling is PsiComment) {
        comments.add(sibling)
      }
      sibling = sibling.nextSibling
    }

    return if (!comments.isEmpty()) {
      val stringifiedComments = comments.map { c -> trimCommentPrefix(c.text) }
      Joiner.on("\n").join(stringifiedComments)
    } else null
  }

  @JvmStatic
  fun getNamespaceName(referenceExpression: RNamespaceAccessExpression): String {
    return referenceExpression.namespace.text
  }

  @JvmStatic
  fun getName(identifierExpression: RIdentifierExpression): String {
    val text = identifierExpression.text
    val namespaceIndex = text.indexOf("::")
    if (namespaceIndex > 0) {
      return text.substring(namespaceIndex + 2)
    }

    // also support operator assignments here
    if (identifierExpression.parent is RAssignmentStatement) {
      val charMatcher = CharMatcher.anyOf("`\"'")
      return charMatcher.trimFrom(text)
    }

    // since any r method can be called with surrunding backticks we discard them here since they are not part of the stub index
    val charMatcher = CharMatcher.anyOf("`")
    return charMatcher.trimFrom(text)
  }

  // operator expressions
  @JvmStatic
  fun isBinary(expr: ROperatorExpression): Boolean {
    return expr.children.size == 3
  }

  @JvmStatic
  fun getOperator(expr: ROperatorExpression): ROperator? {
    return PsiTreeUtil.getChildOfType(expr, ROperator::class.java)
  }

  @JvmStatic
  fun getExpr(expr: ROperatorExpression): RExpression? {
    if (expr.isBinary) {
      return null
    }
    val expressions = PsiTreeUtil.getChildrenOfType(expr, RExpression::class.java)
    return if (expressions == null || expressions.size != 1) null else expressions[0]
  }

  @JvmStatic
  fun getLeftExpr(expr: ROperatorExpression): RExpression? {
    if (!expr.isBinary) {
      return null
    }
    val expressions = PsiTreeUtil.getChildrenOfType(expr, RExpression::class.java)
    return if (expressions == null || expressions.size != 2) null else expressions[0]


  }

  @JvmStatic
  fun getRightExpr(expr: ROperatorExpression): RExpression? {
    if (!expr.isBinary) {
      return null
    }
    val expressions = PsiTreeUtil.getChildrenOfType(expr, RExpression::class.java)
    return if (expressions == null || expressions.size != 2) null else expressions[1]

  }

  @JvmStatic
  fun isTrue(boolExpr: RBooleanLiteral): Boolean {
    val node = boolExpr.node
    return node.findChildByType(TokenSet.create(R_TRUE)) != null
  }

  @JvmStatic
  fun isFalse(boolExpr: RBooleanLiteral): Boolean {
    return !isTrue(boolExpr)
  }

  @JvmStatic
  fun getLoop(next: RNextStatement): RLoopStatement? {
    return getLoopImpl(next)
  }

  @JvmStatic
  fun getLoop(rBreak: RBreakStatement): RLoopStatement? {
    return getLoopImpl(rBreak)
  }

  @JvmStatic
  fun getTarget(forLoop: RForStatement): RIdentifierExpression? {
    var child: PsiElement? = forLoop.getFirstChild()
    while (child != null) {
      if (child is RIdentifierExpression) return child
      if (child.elementType == R_IN) return null
      child = child.nextSibling
    }

    return null
  }

  @JvmStatic
  fun getControlFlow(function: RFunctionExpression): RControlFlow =
    CachedValuesManager.getCachedValue(function) { CachedValueProvider.Result.create(buildControlFlow(function), function) }

  @JvmStatic
  fun getAssignedValue(namedArgument: RNamedArgument): RExpression? {
    val expressionList = PsiTreeUtil.getChildrenOfTypeAsList(namedArgument, RExpression::class.java)
    return if (expressionList.size > 1) expressionList[1] else null
  }

  @JvmStatic
  fun getArgumentList(callExpression: RCallExpressionImpl): RArgumentList {
    return PsiTreeUtil.getChildOfType(callExpression, RArgumentList::class.java)!!
  }

  @JvmStatic
  fun getExpression(callExpression: RCallExpressionImpl): RExpression {
    return PsiTreeUtil.getChildOfType(callExpression, RExpression::class.java)!!
  }

  @JvmStatic
  fun getAssociatedS4ClassInfo(callExpression: RCallExpressionImpl): RS4ClassInfo? {
    return callExpression.greenStub?.s4ClassInfo ?: RS4ClassInfoUtil.parseS4ClassInfo(callExpression)
  }

  @JvmStatic
  fun getAssociatedR6ClassInfo(callExpression: RCallExpressionImpl): R6ClassInfo? {
    return callExpression.greenStub?.r6ClassInfo ?: R6ClassInfoUtil.parseR6ClassInfo(callExpression)
  }

  private fun getLoopImpl(element: RPsiElement): RLoopStatement? {
    val loop = PsiTreeUtil.getParentOfType(element, RLoopStatement::class.java, RFunctionExpression::class.java)
    return if (loop is RLoopStatement) loop else null
  }

  private fun trimCommentPrefix(text: String): String {
    var string = text

    string = StringUtil.trimStart(string, "#'") // roxygen  style comment trimming
    string = StringUtil.trimStart(string, "#'") // regular comment style comment trimming
    return string
  }

  private fun getTagImpl(expression: RExpression?): String {
    if (expression is RIdentifierExpression) {
      return expression.text
    }
    else if (expression is RStringLiteralExpression) {
      val text = expression.text
      return text.substring(1, text.length - 1)
    }
    return "..."
  }

  private fun getRoxygenComment(functionExpression: RFunctionExpression): String {
    val detectObjectDocs = ArrayList<String>()

    // traverse back before the function def and collect roxygen comment lines
    var newLineCounter = 0
    var sibling: PsiElement? = functionExpression.parent.prevSibling
    while (sibling != null && (sibling is PsiComment || sibling.text == "\n")) {

      if (sibling is PsiComment && sibling.text.startsWith("#'")) {
        detectObjectDocs.add(trimCommentPrefix(sibling.text).trim { it <= ' ' })
        newLineCounter = 0
      } else {
        newLineCounter++

        if (newLineCounter > 2) {
          break
        }
      }
      sibling = sibling.prevSibling
    }

    return Joiner.on("<br>").join(Lists.reverse(detectObjectDocs))
  }


  private fun findLastExpression(parent: PsiElement): RExpression? {
    var child = parent.lastChild
    while (child != null && child !is RExpression) {
      if (child is PsiErrorElement)
        return null
      child = child.prevSibling
    }
    return child as RExpression
  }

  private fun findFirstExpression(parent: PsiElement): RExpression? {
    var child = parent.firstChild
    while (child != null && child !is RExpression) {
      if (child is PsiErrorElement)
        return null
      child = child.nextSibling
    }
    return child as RExpression
  }

  private fun renameIdentifier(name: String, expression: RIdentifierExpression, oldNameIdentifier: ASTNode) {
    val text = oldNameIdentifier.chars
    val realName = if (text.startsWith('`') && text.endsWith('`')) "`$name`" else RNamesValidator.quoteIfNeeded(name)
    val dummyFile = RElementGenerator.createDummyFile(realName, false, expression.project)
    val identifier = dummyFile.node.firstChildNode.findChildByType(R_IDENTIFIER)
    if (identifier != null) {
      expression.node.replaceChild(oldNameIdentifier, identifier)
    }
  }
}