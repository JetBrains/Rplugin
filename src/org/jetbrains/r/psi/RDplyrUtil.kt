/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import icons.org.jetbrains.r.psi.*
import org.jetbrains.r.console.RConsoleRuntimeInfo
import org.jetbrains.r.psi.api.*

object RDplyrUtil : AbstractTableManipulation<DplyrFunction>() {
  const val PIPE_OPERATOR = "%>%"

  override val nameToFunction = DplyrFunction.values()
    .mapNotNull { (it.functionName ?: return@mapNotNull null) to it }
    .toMap()

  override val packageName = "dplyr"
  override val subscriptionOperator = DplyrFunction.SUBSCRIPTION_OPERATOR
  override val doubleSubscriptionOperator = DplyrFunction.DOUBLE_SUBSCRIPTION_OPERATOR
  override val defaultTransformValue = "NA"

  // TODO(DS-226): Put more functions here
  @Suppress("SpellCheckingInspection")
  override val safeFunctions = super.safeFunctions + mapOf<String, Set<String>>(
    "dplyr" to setOf(
      "all_vars", "any_vars", "between", "case_when", "coalesce", "contains", "cumall", "cumany", "cume_dist", "cummean", "dense_rank",
      "desc", "ends_with", "everything", "first", "if_else", "lag", "last", "last_col", "lead", "matches", "min_rank", "n", "n_distinct",
      "na_if", "near", "nth", "ntile", "num_range", "one_of", "order_by", "percent_rank", "recode", "recode_factor", "row_number",
      "starts_with"
    ) + nameToFunction.keys
  )

  override fun getContextInfoFromArgumentList(expression: RArgumentList,
                                              argument: RPsiElement,
                                              argumentList: MutableList<RExpression>): TableManipulationContextInfo<DplyrFunction>? {
    val callParent = (expression.parent as RCallExpression).parent
    if (callParent is ROperatorExpression && callParent.isBinary && callParent.operator?.name == PIPE_OPERATOR) {
      argumentList.add(0, callParent.leftExpr ?: return null)
    }

    return super.getContextInfoFromArgumentList(expression, argument, argumentList)
  }

  override fun getCustomCallInfo(expression: RExpression?): TableManipulationCallInfo<DplyrFunction>? {
    when (expression) {
      is ROperatorExpression -> {
        if (!expression.isBinary) return null
        if (expression.operator?.name != PIPE_OPERATOR) return null
        val function: DplyrFunction
        val argList: List<RExpression>
        val call = expression.rightExpr ?: return null
        if (call is RCallExpression) {
          function = getTableManipulationFunctionByExpressionName(call.expression) ?: return null
          argList = call.argumentList.expressionList
        }
        else {
          function = getTableManipulationFunctionByExpressionName(call) ?: return null
          argList = listOf()
        }
        if (argList.size + 1 < function.tableArguments) return null
        return TableManipulationCallInfo(call, function, listOf(expression.leftExpr ?: return null) + argList)
      }
      else -> return null
    }
  }

  override fun transformNotCall(expr: RExpression, command: StringBuilder, runtimeInfo: RConsoleRuntimeInfo, preserveRows: Boolean) {
    if (isSafe(expr, runtimeInfo)) {
      if (!preserveRows) command.append("dplyr::filter((")
      command.append(expr.text)
      if (!preserveRows) command.append("),FALSE)")
    }
    else {
      command.append("(dplyr::tibble())")
    }
  }

  fun isPipeCall(expr: RCallExpression): Boolean {
    val parent = expr.parent
    return parent is ROperatorExpression && parent.operator?.name == PIPE_OPERATOR && expr == parent.rightExpr
  }

  fun addCurrentColumns(columns: List<TableManipulationColumn>,
                        dplyrCall: TableManipulationCallInfo<DplyrFunction>,
                        currentArgIndex: Int): List<TableManipulationColumn> {
    val function = dplyrCall.function
    if (currentArgIndex <= function.tableArguments) return columns
    return columns + dplyrCall.arguments.subList(function.tableArguments, currentArgIndex).mapNotNull {
      when (it) {
        is RIdentifierExpression -> TableManipulationColumn(it.name)
        is RNamedArgument -> {
            val name = it.name
            if (name.startsWith(".")) return@mapNotNull null
            TableManipulationColumn(name)
        }
        else -> null
      }
    }
  }
}

@Suppress("SpellCheckingInspection")
enum class DplyrFunction(
  override val functionName: String? = null,
  override val contextType: TableManipulationContextType? = TableManipulationContextType.NORMAL,
  override val returnsTable: Boolean = true,
  override val ignoreInTransform: Boolean = false,
  val varargTables: Boolean = false,
  val tableArguments: Int = 1
) : TableManipulationFunction {
  ADD_COUNT("add_count"),
  ADD_ROW("add_row"),
  ADD_TALLY("add_tally"),
  ARRANGE("arrange"),
  ARRANGE_ALL("arrange_all"),
  ARRANGE_AT("arrange_at"),
  ARRANGE_IF("arrange_if"),
  COUNT("count"),
  DISTINCT("distinct"),
  DISTINCT_ALL("distinct_all"),
  DISTINCT_AT("distinct_at"),
  DISTINCT_IF("distinct_if"),
  FILTER("filter"),
  GROUP_BY("group_by"),
  GROUP_BY_ALL("group_by_all"),
  GROUP_BY_AT("group_by_at"),
  GROUP_BY_IF("group_by_if"),
  MUTATE("mutate"),
  MUTATE_ALL("mutate_all"),
  MUTATE_AT("mutate_at"),
  MUTATE_IF("mutate_if"),
  PULL("pull", returnsTable = false),
  RENAME("rename"),
  RENAME_ALL("rename_all"),
  RENAME_AT("rename_at"),
  RENAME_IF("rename_if"),
  SAMPLE_FRAC("sample_frac", ignoreInTransform = true),
  SAMPLE_N("sample_n", ignoreInTransform = true),
  SELECT("select"),
  SELECT_ALL("select_all"),
  SELECT_AT("select_at"),
  SELECT_IF("select_if"),
  SLICE("slice"),
  SUMMARISE("summarise"),
  SUMMARISE_ALL("summarise_all"),
  SUMMARISE_AT("summarise_at"),
  SUMMARISE_IF("summarise_if"),
  SUMMARIZE("summarize"),
  SUMMARIZE_ALL("summarize_all"),
  SUMMARIZE_AT("summarize_at"),
  SUMMARIZE_IF("summarize_if"),
  TALLY("tally"),
  TIBBLE("tibble", tableArguments = 0),
  TOP_FRAC("top_frac"),
  TOP_N("top_n"),
  TRANSMUTE("transmute"),
  TRANSMUTE_ALL("transmute_all"),
  TRANSMUTE_AT("transmute_at"),
  TRANSMUTE_IF("transmute_if"),
  UNGROUP("ungroup"),

  DOUBLE_SUBSCRIPTION_OPERATOR(contextType = TableManipulationContextType.SUBSCRIPTION, returnsTable = false) {
    override fun isNeedCompletionInsideArgument(argumentList: List<RExpression>, currentArgument: TableManipulationArgument): Boolean {
      return isNeedCompletionInsideSubscription(argumentList, currentArgument)
    }
  },
  SUBSCRIPTION_OPERATOR(contextType = TableManipulationContextType.SUBSCRIPTION) {
    override fun isNeedCompletionInsideArgument(argumentList: List<RExpression>, currentArgument: TableManipulationArgument): Boolean {
      return isNeedCompletionInsideSubscription(argumentList, currentArgument)
    }
  },

  BIND_COLS("bind_cols", tableArguments = 0, varargTables = true, contextType = null),
  BIND_ROWS("bind_rows", tableArguments = 0, varargTables = true, contextType = null),

  INTERSECT("intersect", tableArguments = 2, contextType = null),
  SETDIFF("setdiff", tableArguments = 2, contextType = null),
  UNION("union", tableArguments = 2),
  UNION_ALL("union_all", tableArguments = 2, contextType = null),

  ANTI_JOIN("anti_join", tableArguments = 2, contextType = TableManipulationContextType.JOIN),
  FULL_JOIN("full_join", tableArguments = 2, contextType = TableManipulationContextType.JOIN),
  INNER_JOIN("inner_join", tableArguments = 2, contextType = TableManipulationContextType.JOIN),
  LEFT_JOIN("left_join", tableArguments = 2, contextType = TableManipulationContextType.JOIN),
  NEST_JOIN("nest_join", tableArguments = 2, contextType = TableManipulationContextType.JOIN),
  RIGHT_JOIN("right_join", tableArguments = 2, contextType = TableManipulationContextType.JOIN),
  SEMI_JOIN("semi_join", tableArguments = 2, contextType = TableManipulationContextType.JOIN);

  override fun haveTableArguments(psiCall: RExpression, argumentList: List<RExpression>, runtimeInfo: RConsoleRuntimeInfo?): Boolean {
    return argumentList.size >= tableArguments
  }

  override fun isNeedCompletionInsideArgument(argumentList: List<RExpression>, currentArgument: TableManipulationArgument): Boolean {
    return currentArgument.index >= tableArguments
  }

  override fun isTableArgument(psiCall: RExpression,
                               argumentList: List<RExpression>,
                               currentArgument: TableManipulationArgument,
                               runtimeInfo: RConsoleRuntimeInfo?): Boolean {
    return !isNeedCompletionInsideArgument(argumentList, currentArgument) || (varargTables && currentArgument.name != null)
  }

  protected fun isNeedCompletionInsideSubscription(argumentList: List<RExpression>, currentArgument: TableManipulationArgument): Boolean {
    // table[1:3] - select columns
    // table[,1:3] - select columns
    // table[1:3,] - select rows
    val size = argumentList.size
    val argIndex = currentArgument.index
    return !(argIndex == 0 || size > 3 || (size == 3 && argIndex == 1))
  }
}

