/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.r.console.RConsoleRuntimeInfo
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.rinterop.TableColumnsInfo
import java.util.*
import kotlin.collections.ArrayList

interface TableManipulationFunction {
  val functionName: String?
  val returnsTable: Boolean
  val ignoreInTransform: Boolean
  val rowsOmittingUnavailable: Boolean
  val s3Function: Boolean
  val inheritedFunction: Boolean
  val fullSuperFunctionName: String?
  val tableArguments: List<String>

  fun havePassedTableArguments(argumentInfo: RArgumentInfo): Boolean {
    return argumentInfo.argumentNamesWithPipeExpression.any { it in tableArguments }
  }

  fun isTableArgument(argumentInfo: RArgumentInfo, currentArgument: RExpression): Boolean {
    return argumentInfo.getParameterNameForArgument(currentArgument) in tableArguments
  }

  fun getPassedTableArguments(argumentInfo: RArgumentInfo): List<RExpression> {
    return tableArguments
      .flatMap { if (it != "...") listOf(argumentInfo.getArgumentPassedToParameter(it)) else argumentInfo.allDotsArguments }
      .filterNotNull()
  }

  fun isQuotesNeeded(argumentInfo: RArgumentInfo, currentArgument: RExpression): Boolean

  fun getTableColumns(operandColumns: List<TableManipulationColumn>,
                      callInfo: TableManipulationCallInfo<*>): List<TableManipulationColumn> {
    return operandColumns
  }
}

data class TableManipulationCallInfo<T : TableManipulationFunction>(
  val function: T,
  val argumentInfo: RArgumentInfo
) {
  val passedTableArguments by lazy { function.getPassedTableArguments(argumentInfo) }
  val havePassedTableArguments by lazy { function.havePassedTableArguments(argumentInfo) }

  fun isTableArgument(currentArgument: RExpression): Boolean {
    return function.isTableArgument(argumentInfo, currentArgument)
  }
}

data class TableManipulationContextInfo<T : TableManipulationFunction>(val callInfo: TableManipulationCallInfo<T>,
                                                                       val currentTableArgument: RExpression) {
  val currentArgumentIsTable by lazy { callInfo.isTableArgument(currentTableArgument) }
}


enum class TableType {
  UNKNOWN, DPLYR, DATA_FRAME, DATA_TABLE;

  companion object {
    fun toTableType(type: TableColumnsInfo.TableType): TableType = when (type) {
      TableColumnsInfo.TableType.DATA_FRAME -> DATA_FRAME
      TableColumnsInfo.TableType.DATA_TABLE -> DATA_TABLE
      TableColumnsInfo.TableType.DPLYR -> DPLYR
      else -> UNKNOWN
    }
  }
}

data class TableInfo(val columns: List<TableManipulationColumn>, val type: TableType)

data class TableManipulationColumn(val name: String, val type: String? = null)

abstract class TableManipulationAnalyzer<T : TableManipulationFunction> {

  protected abstract val nameToFunction: Map<String, T>
  protected abstract val packageName: String
  protected abstract val subscriptionOperator: T
  protected abstract val doubleSubscriptionOperator: T
  abstract val tableColumnType: TableType

  fun isSubscription(function: T): Boolean {
    return function == subscriptionOperator || function == doubleSubscriptionOperator
  }

  fun getCallInfo(expression: RExpression?, runtimeInfo: RConsoleRuntimeInfo?): TableManipulationCallInfo<T>? {
    return when (expression) {
      is RCallExpression -> getCallInfoFromCallExpression(expression, runtimeInfo)
      is RSubscriptionExpression -> getCallInfoFromSubscriptionExpression(expression, runtimeInfo)
      is ROperatorExpression -> {
        if (!expression.isBinary) return null
        if (expression.operator?.name != PIPE_OPERATOR) return null

        val function: T
        val argList: RArgumentHolder
        val call = expression.rightExpr ?: return null
        when (call) {
          is RCallExpression -> {
            function = getTableManipulationFunctionByExpressionName(call.expression) ?: return null
            argList = call.argumentList
          }
          is RSubscriptionExpression -> {
            function = getTableManipulationFunctionByExpressionName(call) ?: return null
            argList = call
          }
          else -> return null
        }
        val info = getArgumentInfo(function, call, argList, runtimeInfo) ?: return null
        return TableManipulationCallInfo(function, info)
      }
      else -> return null
    }
  }

  fun getContextInfo(element: PsiElement, runtimeInfo: RConsoleRuntimeInfo?): TableManipulationContextInfo<T>? {
    val ancestor =
      PsiTreeUtil.getParentOfType(element, RArgumentList::class.java, RSubscriptionExpression::class.java) ?: return null
    val argument = PsiTreeUtil.findPrevParent(ancestor, element) as? RExpression ?: return null
    return when (ancestor) {
      is RArgumentList -> getContextInfoFromArgumentList(ancestor, argument, runtimeInfo)
      is RSubscriptionExpression -> getContextInfoFromSubscriptionExpression(ancestor, argument, runtimeInfo)
      else -> null
    }
  }

  /**
   * Statically calculates columns of the variable
   * It could use runtime to get columns for expressions that are used in the definition of the variable
   */
  fun retrieveTableFromVariable(variable: RIdentifierExpression, runtimeInfo: RConsoleRuntimeInfo):TableInfo {
    val scope = LocalSearchScope(variable.containingFile)
    var variableDefinition = variable.reference.resolve()
    if (variableDefinition != null) {
      val variableAssignment = ArrayList<PsiElement>()
      variableAssignment.add(variableDefinition)
      val identifierUsages = ReferencesSearch.search(variableDefinition, scope).findAll()
      for (reference in identifierUsages) {
        val parent = reference.element.parent
        if (parent is RAssignmentStatement) {
          if (parent.startOffset < variable.startOffset && parent.startOffset > variableDefinition!!.startOffset) {
            variableDefinition = parent
          }
        }
      }
      if (variableDefinition is RAssignmentStatement) {
        val assignedValue = variableDefinition.assignedValue
        if (assignedValue != null) {
          return getTableColumns(assignedValue, runtimeInfo)
        }
      }
    }
    return TableInfo(Collections.emptyList(), TableType.UNKNOWN)
  }

  fun getTableFromVariable(variable: RIdentifierExpression, runtimeInfo: RConsoleRuntimeInfo):TableInfo {
    return CachedValuesManager.getCachedValue(variable) {
      CachedValueProvider.Result.create(retrieveTableFromVariable(variable, runtimeInfo), variable)
    }
  }

  fun getTableFromCall(argumentColumns: List<TableManipulationColumn>, call: RCallExpression): TableInfo {
    val callInfo = getCallInfo(call, null)
    if (callInfo != null) {
      val columns = callInfo.function.getTableColumns(argumentColumns, callInfo)
      return TableInfo(columns, TableType.UNKNOWN)
    }
    return TableInfo(Collections.emptyList(), TableType.UNKNOWN)
  }

  /**
   * Retrieve the columns for the element with runtime or statically (if expression is not loaded yet)
   */
  fun getTableColumns(element: RExpression, runtimeInfo: RConsoleRuntimeInfo): TableInfo {
    val columnsFromConsole = getTableColumnsFromConsole(element, runtimeInfo)
    if (columnsFromConsole.columns.isNotEmpty() || columnsFromConsole.type != TableType.UNKNOWN) {
      return columnsFromConsole
    }

    if (element is RIdentifierExpression) {
      return getTableFromVariable(element, runtimeInfo)
    }
    else if (element is ROperatorExpression) {
      val operator = element.operator
      if (operator == null || PIPE_OPERATOR != operator.text) {
        return TableInfo(Collections.emptyList(), TableType.UNKNOWN)
      }
      val columnList = ArrayList<TableManipulationColumn>()
      val leftExpression = element.leftExpr
      if (leftExpression != null) {
        val tableColumns = getTableColumns(leftExpression, runtimeInfo)
        columnList.addAll(tableColumns.columns)
      }

      val rightExpr = element.rightExpr
      if (rightExpr is RCallExpression) {
        return getTableFromCall(columnList, rightExpr)
      }
    }
    else if (element is RCallExpression) {
      return getTableFromCall(Collections.emptyList(), element)
    }
    return TableInfo(Collections.emptyList(), TableType.UNKNOWN)
  }

  fun getTableColumnsFromConsole(table: RExpression, runtimeInfo: RConsoleRuntimeInfo): TableInfo {
    val expression = StringBuilder()
    transformExpression(table, expression, runtimeInfo)
    return runtimeInfo.loadTableColumns(expression.toString())
  }

  protected open fun getTableManipulationFunctionByExpressionName(expression: RExpression): T? {
    val name = when (expression) {
      is RIdentifierExpression -> expression.text
      is RNamespaceAccessExpression ->
        if (expression.namespaceName == packageName) expression.identifier?.name else null
      else -> null
    }
    return nameToFunction[name]
  }

  private fun getCallInfoFromCallExpression(expression: RCallExpression,
                                            runtimeInfo: RConsoleRuntimeInfo?): TableManipulationCallInfo<T>? {
    val function = getTableManipulationFunctionByExpressionName(expression.expression) ?: return null
    val argumentInfo = getArgumentInfo(function, expression, expression.argumentList, runtimeInfo) ?: return null
    return TableManipulationCallInfo(function, argumentInfo)
  }

  private fun getCallInfoFromSubscriptionExpression(expression: RSubscriptionExpression,
                                                    runtimeInfo: RConsoleRuntimeInfo?): TableManipulationCallInfo<T>? {
    val subscription = if (expression.isSingle) subscriptionOperator else doubleSubscriptionOperator
    val argumentInfo = getArgumentInfo(subscription, expression, expression, runtimeInfo) ?: return null
    return TableManipulationCallInfo(subscription, argumentInfo)
  }

  private fun getContextInfoFromArgumentList(argumentList: RArgumentList,
                                             argument: RExpression,
                                             runtimeInfo: RConsoleRuntimeInfo?): TableManipulationContextInfo<T>? {
    val call = argumentList.parent as RCallExpression
    val function = getTableManipulationFunctionByExpressionName(call.expression) ?: return getContextInfo(call, runtimeInfo)
    val info = getArgumentInfo(function, call, argumentList, runtimeInfo) ?: return null
    return TableManipulationContextInfo(TableManipulationCallInfo(function, info), argument)
  }

  private fun getContextInfoFromSubscriptionExpression(expression: RSubscriptionExpression,
                                                       argument: RExpression,
                                                       runtimeInfo: RConsoleRuntimeInfo?): TableManipulationContextInfo<T>? {
    val function = if (expression.isSingle) subscriptionOperator else doubleSubscriptionOperator
    val info = getArgumentInfo(function, expression, expression, runtimeInfo) ?: return null
    return TableManipulationContextInfo(TableManipulationCallInfo(function, info), argument)
  }

  private fun getArgumentInfo(function: T,
                              call: RExpression,
                              argumentHolder: RArgumentHolder,
                              runtimeInfo: RConsoleRuntimeInfo?): RArgumentInfo? {
    val allArgumentNames = if (function.s3Function) {
      if (runtimeInfo == null) return null
      val fullFunctionName = when {
        function.inheritedFunction -> function.fullSuperFunctionName ?: return null
        isSubscription(function) -> "$packageName:::${function.functionName}"
        else -> "$packageName:::${function.functionName}.$packageName"
      }
      runtimeInfo.getFormalArguments(fullFunctionName)
    }
    else {
      RPsiUtil.resolveCall((call as? RCallExpression) ?: return null)
        .maxBy { RSkeletonUtil.skeletonFileToRPackage(it.containingFile)?.name == packageName }
        ?.parameterNameList?.map { it }
    }
    return RArgumentInfo.getParameterInfo(argumentHolder, allArgumentNames ?: return null)
  }

  protected abstract fun transformNotCall(expr: RExpression,
                                          command: StringBuilder,
                                          runtimeInfo: RConsoleRuntimeInfo,
                                          preserveRows: Boolean = false)

  fun transformExpression(expression: RExpression,
                          command: StringBuilder,
                          runtimeInfo: RConsoleRuntimeInfo,
                          preserveRows: Boolean = false) {
    val callInfo = getCallInfo(expression, runtimeInfo)
    if (callInfo != null && callInfo.function.havePassedTableArguments(callInfo.argumentInfo)) {
      val function = callInfo.function
      if (function.ignoreInTransform) {
        callInfo.passedTableArguments.firstOrNull()?.let { transformExpression(it, command, runtimeInfo, preserveRows) }
        return
      }

      val preserveRowsInArgs = if (function.rowsOmittingUnavailable) true else preserveRows
      val transformArguments = { arguments: List<RExpression> ->
        arguments.forEachIndexed { index, argument ->
          if (index != 0) command.append(",")
          if (callInfo.isTableArgument(argument)) {
            transformExpression(argument, command, runtimeInfo, preserveRowsInArgs)
          }
          else {
            if (argument is RNamedArgument) {
              val argumentValue = argument.assignedValue
              val argumentText =
                if (argumentValue != null && isSafe(argumentValue, runtimeInfo)) argumentValue.text
                else "NA"
              command.append(argument.identifyingElement?.text).append("=").append(argumentText)
            }
            else {
              command.append(if (isSafe(argument, runtimeInfo)) argument.text else "")
            }
          }
        }
      }

      if (function == subscriptionOperator || function == doubleSubscriptionOperator) {
        transformExpression(callInfo.argumentInfo.expressionListWithPipeExpression[0], command, runtimeInfo, preserveRowsInArgs)
        command.append(if (function == subscriptionOperator) "[" else "[[")
        transformArguments(callInfo.argumentInfo.expressionListWithPipeExpression.drop(1))
        command.append(if (function == subscriptionOperator) "]" else "]]")
      }
      else {
        command.append("$packageName::")
        command.append(function.functionName)
        command.append("(")
        transformArguments(callInfo.argumentInfo.expressionListWithPipeExpression)
        command.append(")")
      }
    }
    else transformNotCall(expression, command, runtimeInfo, preserveRows)
  }

  fun isSafe(expr: RExpression?, runtimeInfo: RConsoleRuntimeInfo): Boolean {
    if (expr == null) return true
    return when (expr) {
      is RIdentifierExpression -> true
      is RNamespaceAccessExpression -> true
      is RBooleanLiteral -> true
      is RNumericLiteralExpression -> true
      is RStringLiteralExpression -> true
      is RNaLiteral -> true
      is RParenthesizedExpression -> isSafe(expr.expression, runtimeInfo)
      is RMemberExpression -> expr.expressionList.all { isSafe(it, runtimeInfo) }
      is RSubscriptionExpression -> expr.expressionList.all { isSafe(it, runtimeInfo) }
      is ROperatorExpression -> {
        if (!isSafeFunction(expr.operator ?: return false, runtimeInfo)) return false
        isSafe(expr.leftExpr, runtimeInfo) && isSafe(expr.rightExpr, runtimeInfo) && isSafe(expr.expr, runtimeInfo)
      }
      is RCallExpression -> {
        if (!isSafeFunction(expr.expression, runtimeInfo)) return false
        expr.argumentList.expressionList.all {
          if (it is RNamedArgument) {
            isSafe(it.assignedValue, runtimeInfo)
          }
          else {
            isSafe(it, runtimeInfo)
          }
        }
      }
      else -> false
    }
  }

  private fun isSafeFunction(expr: RPsiElement, runtimeInfo: RConsoleRuntimeInfo): Boolean {
    if (RPsiUtil.isReturn(expr)) return false
    when (expr) {
      is RNamespaceAccessExpression -> {
        val name = expr.identifier?.name ?: return false
        val pkg = expr.namespaceName
        return name in safeFunctions[pkg].orEmpty()
      }
      is RIdentifierExpression, is ROperator -> {
        val name = expr.name
        return runtimeInfo.loadedPackages.keys.any { name in safeFunctions[it].orEmpty() }
      }
      else -> return false
    }
  }

  // TODO(DS-226): Put more functions here
  @Suppress("SpellCheckingInspection")
  protected open val safeFunctions = mapOf<String, Set<String>>(
    "base" to setOf(
      "+", "-", "*", "/", "%%", "%/%", "^", "<", ">", "<=", ">=", "==", "!=", "&", "&&", "|", "||", "!", ":", "%in%", "%*%", "abs", "acos",
      "asin",
      "atan", "atan2", "c", "ceiling", "cos", "cospi", "exp", "floor", "is.na", "log", "log10", "paste", "round", "signif", "sin", "sinpi",
      "sqrt", "tan", "tanpi", "trunc"
    )
  )

  companion object {
    private const val PIPE_OPERATOR = "%>%"

    /**
     * Retrieves column from call dot arguments. Columns from argument table ignored.
     * Useful for functions like "summarize".
     */
    fun getAllDotsColumns(callInfo: TableManipulationCallInfo<*>): List<TableManipulationColumn> {
      val result = ArrayList<TableManipulationColumn>()
      for (expression in callInfo.argumentInfo.allDotsArguments) {
        if (expression is RNamedArgument) {
          result.add(TableManipulationColumn(expression.name))
        }
        else if (expression is RIdentifierExpression) {
          result.add(TableManipulationColumn(expression.name))
        }
      }
      return result
    }

    /**
     * @return joined columns from argument table and call dot arguments
     */
    fun joinTableAndAllDotsColumns(tableColumns: List<TableManipulationColumn>,
                                   callInfo: TableManipulationCallInfo<*>): List<TableManipulationColumn> {
      val result = ArrayList<TableManipulationColumn>(tableColumns)
      result.addAll(getAllDotsColumns(callInfo))
      return result
    }
  }
}