/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.google.common.collect.Lists
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.ProjectAndLibrariesScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.Predicate
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.parsing.RElementTypes.*
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.impl.RAssignmentStatementImpl
import org.jetbrains.r.psi.references.RReferenceBase
import org.jetbrains.r.psi.stubs.RAssignmentNameIndex
import org.jetbrains.r.psi.stubs.RParameterStub
import org.jetbrains.r.skeleton.psi.RSkeletonBase


object RPsiUtil {

  val BUILTIN_CONSTANTS = TokenSet.create(
    R_NA_LITERAL, R_NA_LITERAL, R_BOUNDARY_LITERAL, R_BOOLEAN_LITERAL, R_NULL_LITERAL)
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

  private val LOCAL_ASSIGNMENT = TokenSet.create(
    R_LEFT_ASSIGN, R_RIGHT_ASSIGN)

  private val LOG = Logger.getInstance(RPsiUtil::class.java)

  val MINUTE = 60 * 1000
  private val PACKAGE_IMPORT_METHODS = Lists.newArrayList("require", "library")


  fun getParametersExpressions(parameters: List<RParameter>): List<RExpression> {
    return parameters.mapNotNull { it.variable }
  }


  fun getAssignmentStatement(parameter: RParameter): RAssignmentStatement? {
    val functionExpression = getFunction(parameter) ?: return null
    val assignmentStatement = functionExpression.parent
    return if (assignmentStatement == null || assignmentStatement !is RAssignmentStatement) {
      null
    }
    else assignmentStatement
  }


  fun getFunction(parameter: RParameter): RFunctionExpression? {
    //TODO: check some conditions when we should stop
    return PsiTreeUtil.getParentOfType(parameter, RFunctionExpression::class.java)
  }


  /**
   * Returns true if `element` is the LHS of named argument in a function call.
   */
  fun isNamedArgument(element: RIdentifierExpression): Boolean {
    val parent = element.parent as? RAssignmentStatement ?: return false
    val argumentList = parent.parent
    return argumentList is RArgumentList && parent.assignee === element
  }

  fun isNamedArgumentAssignment(element: PsiElement): Boolean {
    return element is RAssignmentStatement && element.isEqual && element.assignee is RIdentifierExpression &&
           (element.parent is RArgumentList || element.parent is RSubscriptionExpression)
  }

  fun getArgumentName(argument: RPsiElement): String? {
    return if (isNamedArgumentAssignment(argument)) argument.name else null
  }

  /**
   * Returns true if `element` is the LHS of named argument in a function call.
   */
  fun isVarDeclaration(element: RIdentifierExpression): Boolean {
    val parent = element.parent

    return if (parent !is RAssignmentStatement) {
      false
    }
    else parent.assignee == element

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


  fun getFunction(binaryExpression: ROperatorExpression): RFunctionExpression? {
    val operator = PsiTreeUtil.getChildOfType(binaryExpression, org.jetbrains.r.psi.api.ROperator::class.java)
    return if (operator != null) {
      getFunctionFromReference(operator.reference)
    }
    else null
  }


  fun getAssignmentStatement(expression: RFunctionExpression): RAssignmentStatement? {
    val assignmentStatement = expression.parent
    return if (assignmentStatement is RAssignmentStatement) {
      assignmentStatement
    }
    else null
  }


  fun isReturn(expression: PsiElement): Boolean {
    return expression is RCallExpression && expression.expression.text in listOf("return", "base::return", "base:::return")
  }


  fun findCall(project: Project, functionName: String, predicate: Predicate<RCallExpression>): RCallExpression? {
    val scope = ProjectAndLibrariesScope(project)
    val possibleDefinitions = RAssignmentNameIndex.find(functionName, project, scope)
    var functionDefinition: RAssignmentStatement? = null
    for (assignment in possibleDefinitions) {
      if (assignment.assignedValue is RFunctionExpression) {
        functionDefinition = assignment
        break
      }
    }
    if (functionDefinition == null) {
      return null
    }
    for (reference in ReferencesSearch.search(functionDefinition, scope)) {
      val referenceFrom = reference.element
      val parent = referenceFrom.parent as? RCallExpression ?: continue
      if (predicate.apply(parent)) {
        return parent
      }
    }
    return null
  }


  fun isNamespacePrefix(element: RIdentifierExpression): Boolean {
    return element.parent is RNamespaceAccessExpression
  }


  fun isReturnValue(o: PsiElement): Boolean {
    // the recursion is needed to resolve final blocks
    //        return PsiTreeUtil.nextVisibleLeaf(o) == null && isReturnValue(o.getParent());
    return PsiTreeUtil.getNextSiblingOfType(o, RExpression::class.java) == null
           && (o.parent is RFunctionExpression || o.parent != null && isReturnValue(o.parent))
  }

  /**
   * Returns method name if `functionExpression` is not an anonymous function definition.
   */
  fun getName(functionExpression: RFunctionExpression): String? {
    return if (functionExpression.parent is RAssignmentStatement) {
      (functionExpression.parent as RAssignmentStatementImpl).name
    }
    else null

  }

  fun isLocalAssignment(rAssignmentStatement: RAssignmentStatement): Boolean {
    return rAssignmentStatement.node.findChildByType(LOCAL_ASSIGNMENT) != null
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

  fun resolveCall(call: RCallExpression, isIncomplete: Boolean = true): List<RAssignmentStatement> {
    call.expression.reference?.let { reference ->
      if (reference !is RReferenceBase<*>) {
        return emptyList()
      }

      val multiResolve = reference.multiResolve(isIncomplete)
      return multiResolve.map { it.element }.filterIsInstance<RAssignmentStatement>()
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
}

fun RIdentifierExpression.isNamespaceAccess() : Boolean {
  return parent?.let { it is RNamespaceAccessExpression && it.identifier == this } == true
}

fun RStringLiteralExpression.isComplete() = text.length > 1 && text.first() == text.last()

fun RAssignmentStatement.isNamedArgumentAssignment() = isEqual && parent is RArgumentList

fun RExpression.isAssignee(): Boolean {
  val parent = parent
  return parent is RAssignmentStatement && this == parent.assignee
}

fun RAssignmentStatement.getParameters(): List<RParameter> {
  if (!isFunctionDeclaration) {
    return emptyList()
  }
  (this as? RAssignmentStatementImpl)?.greenStub?.let {
    return it.childrenStubs.filterIsInstance(RParameterStub::class.java).map { it.psi }
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

fun RCallExpression.isFunctionFromLibrary(functionName: String, packageName: String): Boolean {
  val expr = expression
  val (name, namespaceName, reference) = when (expr) {
    is RIdentifierExpression -> Triple(expr.name, "", expr.reference)
    is RNamespaceAccessExpression -> Triple(expr.identifier?.name, expr.namespaceName, expr.identifier?.reference)
    else -> return false
  }

  if (namespaceName.isNotEmpty() && namespaceName != packageName) return false
  if (name != functionName) return false
  val targets = reference?.multiResolve(false)?.mapNotNull { it.element }
  if (targets != null &&
      targets.any {
        RPsiUtil.isLibraryElement(it) &&
        RSkeletonUtil.parsePackageAndVersionFromSkeletonFilename(it.containingFile.name)?.first == packageName
      }) {
    return true
  }
  return false
}