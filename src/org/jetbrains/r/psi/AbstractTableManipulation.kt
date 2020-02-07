/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.console.RConsoleRuntimeInfo
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.rinterop.Service

enum class TableManipulationContextType {
  NORMAL, SUBSCRIPTION, JOIN
}

interface TableManipulationFunction {
  val functionName: String?
  val contextType: TableManipulationContextType?
  val returnsTable: Boolean
  val ignoreInTransform: Boolean

  fun isNeedCompletionInsideArgument(argumentList: List<RExpression>, currentArgument: TableManipulationArgument): Boolean
  fun haveTableArguments(psiCall: RExpression, argumentList: List<RExpression>, runtimeInfo: RConsoleRuntimeInfo?): Boolean
  fun isTableArgument(psiCall: RExpression,
                      argumentList: List<RExpression>,
                      currentArgument: TableManipulationArgument,
                      runtimeInfo: RConsoleRuntimeInfo?): Boolean
}

data class TableManipulationArgument(val index: Int, val name: String?) {
  constructor(index: Int) : this(index, null)
}

data class TableManipulationCallInfo<T : TableManipulationFunction>(val psiCall: RExpression,
                                                                    val function: T,
                                                                    val arguments: List<RExpression>) {
  init {
    require(psiCall is RCallExpression || psiCall is RSubscriptionExpression) {
      "PsiCall should be RCallExpression or RSubscriptionExpression"
    }
  }
}

data class TableManipulationContextInfo<T : TableManipulationFunction>(val callInfo: TableManipulationCallInfo<T>,
                                                                       val currentTableArgument: TableManipulationArgument)


enum class TableType {
  UNKNOWN, DPLYR, DATA_FRAME, DATA_TABLE;
  companion object  {
    fun toTableType(type: Service.TableColumnsInfo.TableType): TableType = when (type) {
      Service.TableColumnsInfo.TableType.DATA_FRAME -> DATA_FRAME
      Service.TableColumnsInfo.TableType.DATA_TABLE -> DATA_TABLE
      Service.TableColumnsInfo.TableType.DPLYR -> DPLYR
      else -> UNKNOWN
    }
  }
}

data class TableInfo(val columns: List<TableManipulationColumn>, val type: TableType)

data class TableManipulationColumn(val name: String, val type: String? = null)

abstract class AbstractTableManipulation<T : TableManipulationFunction> {

  protected abstract val nameToFunction: Map<String, T>
  protected abstract val packageName: String
  protected abstract val subscriptionOperator: T
  protected abstract val doubleSubscriptionOperator: T
  protected abstract val defaultTransformValue: String

  protected open fun getCallInfoFromCallExpression(expression: RCallExpression): TableManipulationCallInfo<T>? {
    val function = getTableManipulationFunctionByExpressionName(expression.expression) ?: return null
    val argumentList = expression.argumentList.expressionList
    return TableManipulationCallInfo(expression, function, argumentList)
  }

  protected open fun getCallInfoFromSubscriptionExpression(expression: RSubscriptionExpression): TableManipulationCallInfo<T>? {
    val arguments = expression.expressionList
    return if (expression.isSingle) {
      TableManipulationCallInfo(expression, subscriptionOperator, arguments)
    }
    else {
      TableManipulationCallInfo(expression, doubleSubscriptionOperator, arguments)
    }
  }

  protected open fun getCustomCallInfo(expression: RExpression?): TableManipulationCallInfo<T>? {
    return null
  }

  fun getCallInfo(expression: RExpression?): TableManipulationCallInfo<T>? {
    return when (expression) {
      is RCallExpression -> getCallInfoFromCallExpression(expression)
      is RSubscriptionExpression -> getCallInfoFromSubscriptionExpression(expression)
      else -> getCustomCallInfo(expression)
    }
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

  protected open fun getContextInfoFromArgumentList(expression: RArgumentList,
                                                    argument: RPsiElement,
                                                    argumentList: MutableList<RExpression>): TableManipulationContextInfo<T>? {
    val index = argumentList.indexOf(argument)
    if (index == -1) return null

    val call = expression.parent as RCallExpression
    val function = getTableManipulationFunctionByExpressionName(call.expression) ?: return getContextInfo(call)
    if (function.contextType == null) return null

    val tableArgument = TableManipulationArgument(index, (argument as? RNamedArgument)?.name)

    return if (!function.isNeedCompletionInsideArgument(argumentList, tableArgument)) null
    else TableManipulationContextInfo(TableManipulationCallInfo(call, function, argumentList), tableArgument)
  }

  protected open fun getContextInfoFromSubscriptionExpression(expression: RSubscriptionExpression,
                                                              argument: RPsiElement,
                                                              argumentList: MutableList<RExpression>): TableManipulationContextInfo<T>? {
    val index = argumentList.indexOf(argument)
    if (index == -1) return null
    val function = if (expression.isSingle) subscriptionOperator else doubleSubscriptionOperator
    val tableArgument = TableManipulationArgument(index, (argument as? RNamedArgument)?.name)

    return if (!function.isNeedCompletionInsideArgument(argumentList, tableArgument)) null
    else TableManipulationContextInfo(TableManipulationCallInfo(expression, function, argumentList), tableArgument)
  }

  fun getContextInfo(element: PsiElement): TableManipulationContextInfo<T>? {
    val ancestor = PsiTreeUtil.getParentOfType(element, RArgumentList::class.java, RSubscriptionExpression::class.java) ?: return null
    val argument = PsiTreeUtil.findPrevParent(ancestor, element) as RPsiElement
    return when (ancestor) {
      is RArgumentList -> getContextInfoFromArgumentList(ancestor, argument, ancestor.expressionList)
      is RSubscriptionExpression -> getContextInfoFromSubscriptionExpression(ancestor, argument, ancestor.expressionList)
      else -> null
    }
  }

  fun getTableColumns(table: RExpression, runtimeInfo: RConsoleRuntimeInfo): TableInfo {
    val expression = StringBuilder()
    transformExpression(table, expression, runtimeInfo)
    return runtimeInfo.loadTableColumns(expression.toString())
  }

  protected abstract fun transformNotCall(expr: RExpression,
                                          command: StringBuilder,
                                          runtimeInfo: RConsoleRuntimeInfo,
                                          preserveRows: Boolean = false)

  fun transformExpression(expression: RExpression,
                          command: StringBuilder,
                          runtimeInfo: RConsoleRuntimeInfo,
                          preserveRows: Boolean = false) {
    val callInfo = getCallInfo(expression)
    if (callInfo != null && callInfo.function.haveTableArguments(callInfo.psiCall, callInfo.arguments, runtimeInfo)) {
      val function = callInfo.function
      if (function.ignoreInTransform) {
        transformExpression(callInfo.arguments[0], command, runtimeInfo, preserveRows) // TODO
        return
      }

      val transformArguments = { arguments: List<RExpression>, offset: Int ->
        arguments.forEachIndexed { index, argument ->
          if (index != 0) command.append(",")
          if (function.isTableArgument(callInfo.psiCall, callInfo.arguments,
                                       TableManipulationArgument(index + offset, (argument as? RNamedArgument)?.name), runtimeInfo)) {
            transformExpression(argument, command, runtimeInfo, preserveRows)
          }
          else {
            val argumentValue = if (argument is RNamedArgument) argument.assignedValue else argument

            val appendArgument = { it: String ->
              if (it.isNotEmpty() && argument is RNamedArgument) {
                command.append(argument.identifyingElement?.text)
                command.append("=")
              }
              command.append(it)
            }

            appendArgument(if (argumentValue != null && isSafe(argumentValue, runtimeInfo)) argumentValue.text else defaultTransformValue)
          }
        }
      }

      if (function == subscriptionOperator || function == doubleSubscriptionOperator) {
        transformExpression(callInfo.arguments[0], command, runtimeInfo, preserveRows)
        command.append(if (function == subscriptionOperator) "[" else "[[")
        transformArguments(callInfo.arguments.drop(1), 1)
        command.append(if (function == subscriptionOperator) "]" else "]]")
      }
      else {
        command.append("$packageName::")
        command.append(function.functionName)
        command.append("(")
        transformArguments(callInfo.arguments, 0)
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
}