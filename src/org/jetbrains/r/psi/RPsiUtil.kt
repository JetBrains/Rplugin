/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.google.common.collect.Lists
import com.intellij.lang.ASTNode
import com.intellij.psi.*
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.parsing.RElementTypes.*
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.impl.RAssignmentStatementImpl
import org.jetbrains.r.psi.stubs.RParameterStub
import org.jetbrains.r.skeleton.psi.RSkeletonBase


object RPsiUtil {

  val RESERVED_WORDS = TokenSet.create(
    R_IF, R_ELSE, R_REPEAT,
    R_WHILE, R_FUNCTION, R_FOR,
    R_IN, R_NEXT, R_BREAK)
  val OPERATORS = TokenSet.create(
    R_MINUS, R_PLUS, R_NOT, R_TILDE, R_HELP,
    R_COLON, R_MULT, R_DIV, R_EXP,
    R_INFIX_OP, R_LT, R_GT, R_EQEQ, R_GE,
    R_LE, R_AND, R_ANDAND, R_OR, R_OROR,
    R_LEFT_ASSIGN, R_RIGHT_ASSIGN, R_LIST_SUBSET, R_AT)

  private val PACKAGE_IMPORT_METHODS = Lists.newArrayList("require", "library")

  /**
   * Returns true if `element` is the LHS of named argument in a function call.
   */
  fun isNamedArgument(element: RIdentifierExpression): Boolean {
    val parent = element.parent
    return parent is RNamedArgument && parent.identifyingElement == element
  }

  fun getFunction(callExpression: RCallExpression): RFunctionExpression? {
    val expression = callExpression.expression
    return if (expression is RIdentifierExpression) {
      getFunctionFromReference(expression.reference)
    }
    else null
  }

  private fun getFunctionFromReference(reference: PsiReference?): RFunctionExpression? {
    if (reference == null) {
      return null
    }
    val functionDef = reference.resolve() ?: return null
    if (functionDef is RAssignmentStatement) {
      return PsiTreeUtil.getChildOfType(functionDef, RFunctionExpression::class.java)
    }
    val assignmentStatement = functionDef.parent
    return PsiTreeUtil.getChildOfType(assignmentStatement, RFunctionExpression::class.java)
  }

  fun isReturn(expression: PsiElement): Boolean {
    return expression is RCallExpression && expression.expression.text in listOf("return", "base::return", "base:::return")
  }

  fun isReturnValue(o: PsiElement): Boolean {
    // the recursion is needed to resolve final blocks
    //        return PsiTreeUtil.nextVisibleLeaf(o) == null && isReturnValue(o.getParent());
    return PsiTreeUtil.getNextSiblingOfType(o, RExpression::class.java) == null
           && (o.parent is RFunctionExpression || o.parent != null && isReturnValue(o.parent))
  }

  fun isLibraryElement(element: PsiElement): Boolean {
    return element is RSkeletonBase
  }

  fun isImportStatement(psiElement: PsiElement): Boolean {
    return psiElement is RCallExpression && PACKAGE_IMPORT_METHODS.contains(psiElement.expression.text)
  }

  fun getCallByExpression(expression: RExpression) : RCallExpression? {
    val call = expression.parent ?: return null
    return if (call is RCallExpression && call.expression == expression) call else null
  }

  fun getAssignmentByAssignee(assignee: RExpression) : RAssignmentStatement? {
    val assignment = assignee.parent
    return if (assignment is RAssignmentStatement && assignment.assignee == assignee) assignment else null
  }

  fun getNamedArgumentByNameIdentifier(assignee: RExpression) : RNamedArgument? {
    val assignment = assignee.parent
    return if (assignment is RNamedArgument && assignment.nameIdentifier == assignee) assignment else null
  }

  fun resolveCall(call: RCallExpression, isIncomplete: Boolean = true): List<RAssignmentStatement> {
    val reference = when (val expression = call.expression) {
      is RNamespaceAccessExpression -> expression.identifier?.reference
      is RIdentifierExpression -> expression.reference
      else -> return emptyList()
    }

    reference?.let { ref ->
      val multiResolve = ref.multiResolve(isIncomplete)
      return multiResolve.map { it.element }.filterIsInstance<RAssignmentStatement>().filter { it.isFunctionDeclaration }
    }
    return emptyList()
  }

  /**
   * It is possible to use `@` and `$` operators as user-defined binary operators.
   * But in most cases users will not do it. And so we want to treat the right part of these operators specially.
   *
   * @return Whether the [expression] is a right part of `@` or `$` operator
   */
  fun isFieldLikeComponent(expression: RExpression): Boolean {
    val parent = expression.parent
    return parent is ROperatorExpression &&
           parent.isBinary &&
           (parent.operator is RAtOperator || parent.operator is RListSubsetOperator) &&
           parent.rightExpr == expression
  }

  fun isSectionDivider(psi: PsiElement): Boolean {
    if (psi !is PsiComment) return false
    val text: String = psi.text
    return text.endsWith("----") ||
           text.endsWith("====") ||
           text.endsWith("####")

  }

  fun extractNameFromSectionComment(element: PsiComment): String {
    val comment = element.text
    val start = comment.indexOfFirst { it != '#' }.takeIf { it != -1 } ?: return "Untitled"
    val end = comment.indexOfLast { it != '-' && it != '=' && it != '#' }.takeIf { it != -1 } ?: return "Untitled"
    return comment.substring(start, end + 1).trim()
  }

  fun isWhitespaceWithNL(psi: PsiElement): Boolean = isWhitespaceWithNL(psi.node)
  fun isWhitespaceWithNL(node: ASTNode): Boolean = node.elementType == TokenType.WHITE_SPACE && node.textContains('\n')
}

val RIdentifierExpression.isInsideSubscription: Boolean
  get() {
    var current: PsiElement = this
    var parent = current.parent
    if (parent is RCallExpression && parent.expression == current) return false
    while (parent != null) {
      if (parent is RSubscriptionExpression && current is RExpression && parent.expressionList.indexOf(current) > 0) return true
      if (parent is PsiFile || parent is RFunctionExpression || parent is RBlockExpression) return false
      current = parent
      parent = parent.parent
    }
    return false
  }

val RIdentifierExpression.isDependantIdentifier: Boolean
  get() = parent.let { parent ->
    !(parent is RMemberExpression && parent.expressionList.firstOrNull() == this) &&
    (parent is RNamespaceAccessExpression ||
     parent is RParameter && parent.variable == this ||
     (parent is RMemberExpression && parent.expressionList.first() != this) ||
     RPsiUtil.getNamedArgumentByNameIdentifier(this) != null ||
     isInsideSubscription)
  }

fun RIdentifierExpression.isNamespaceAccess() : Boolean {
  return parent?.let { it is RNamespaceAccessExpression && it.identifier == this } == true
}

fun RExpression.isAssignee(): Boolean {
  val parent = parent
  return parent is RAssignmentStatement && this == parent.assignee
}

fun RAssignmentStatement.getParameters(): List<RParameter> {
  if (!isFunctionDeclaration) {
    return emptyList()
  }
  (this as? RAssignmentStatementImpl)?.greenStub?.let { stub ->
    return stub.childrenStubs.filterIsInstance(RParameterStub::class.java).map { it.psi }
  }
  // TODO: handle somehow binary skeleton case. Ticket R-257
  return (assignedValue as? RFunctionExpression)?.parameterList?.parameterList ?: emptyList()
}

fun RExpression?.withoutParenthesis(): RExpression? {
  return if (this is RParenthesizedExpression) this.expression.withoutParenthesis() else this
}

fun RExpression.findParenthesisParent(): RExpression {
  return parent.let { if (it is RParenthesizedExpression) it.findParenthesisParent() else this }
}

fun PsiElement.findBlockParent(): RPsiElement {
  if (this is RFile || this is RBlockExpression || this is RFunctionExpression) return this as RPsiElement
  val parent = parent
  if (parent is RIfStatement && (this == parent.ifBody || this == parent.elseBody)) return parent
  if (parent is RForStatement && this == parent.body) return parent
  if (parent is RWhileStatement && this == parent.body) return parent
  if (parent is RRepeatStatement && this == parent.body) return parent
  return parent.findBlockParent()
}

fun RCallExpression.isFunctionFromLibrarySoft(functionName: String, packageName: String): Boolean {
  val expr = expression
  val (name, namespaceName) = when (expr) {
    is RIdentifierExpression -> expr.name to ""
    is RNamespaceAccessExpression -> expr.identifier?.name to expr.namespaceName
    else -> return false
  }

  if (namespaceName.isNotEmpty() && namespaceName != packageName) return false
  if (name != functionName) return false
  return true
}

fun RCallExpression.isFunctionFromLibrary(functionName: String, packageName: String): Boolean {
  if (!isFunctionFromLibrarySoft(functionName, packageName)) return false
  val expr = expression
  val reference =
    if (expr is RIdentifierExpression) expr.reference
    else (expr as RNamespaceAccessExpression).identifier?.reference
  val targets = reference?.multiResolve(false)?.mapNotNull { it.element }
  if (targets != null &&
      targets.any {
        RPsiUtil.isLibraryElement(it) &&
        RSkeletonUtil.skeletonFileToRPackage(it.containingFile)?.name == packageName
      }) {
    return true
  }
  return false
}