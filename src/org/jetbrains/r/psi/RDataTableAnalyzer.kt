/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import org.jetbrains.r.console.RConsoleRuntimeInfo
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RNamedArgument

object RDataTableAnalyzer : TableManipulationAnalyzer<DataTableFunction>() {
  override val nameToFunction = DataTableFunction.values()
    .mapNotNull { function ->
      listOf((function.functionName ?: return@mapNotNull null) to function) + function.extraFunctionNames.map { it to function }
    }
    .flatten()
    .toMap()

  override val packageName = "data.table"
  override val subscriptionOperator = DataTableFunction.SUBSCRIPTION_OPERATOR
  override val doubleSubscriptionOperator = DataTableFunction.DOUBLE_SUBSCRIPTION_OPERATOR
  override val defaultTransformValue = ""

  // TODO(DS-226): Put more functions here
  @Suppress("SpellCheckingInspection")
  override val safeFunctions = super.safeFunctions + mapOf<String, Set<String>>(
    "data.table" to setOf<String>(
      ".", "%between%", "%chin%", "%inrange%", "IDateTime", "J", "CJ", "SJ", "address", "as.Date", "as.IDate", "as.ITime", "chgroup",
      "chmatch", "chorder", "copy", "first", "fsort", "getDTthreads", "getNumericRounding", "haskey", "hour", "isoweek", "key", "last",
      "mday", "minute", "month", "patterns", "quarter", "rleid", "rowid", "second", "shouldPrint", "tables", "timetaken", "truelength",
      "tstrsplit", "wday", "week", "yday", "year"
    ) + nameToFunction.keys
  )

  override fun transformNotCall(expr: RExpression, command: StringBuilder, runtimeInfo: RConsoleRuntimeInfo, preserveRows: Boolean) {
    if (RDplyrAnalyzer.isSafe(expr, runtimeInfo)) {
      command.append(expr.text)
    }
    else {
      command.append("(data.table::data.table())")
    }
  }
}

@Suppress("SpellCheckingInspection")
enum class DataTableFunction(
  override val functionName: String? = null,
  override val contextType: TableManipulationContextType? = TableManipulationContextType.NORMAL,
  override val returnsTable: Boolean = true,
  override val ignoreInTransform: Boolean = false,
  val inheritedFunction: Boolean = false,
  val fullSuperFunctionName: String? = null,
  val extraFunctionNames: List<String> = emptyList(),
  val allowWithoutQuotes: List<String> = emptyList(),
  val s3Function: Boolean = false,
  val tablesArguments: List<String> = listOf("x")
) : TableManipulationFunction {
  ALL_EQUAL("all.equal", tablesArguments = listOf("target", "current"), s3Function = true, returnsTable = false),
  ALLOC_COL("alloc.col", tablesArguments = listOf("DT"), returnsTable = false), // experimental, might change
  ANY_DUPLICATED("anyDuplicated", s3Function = true, returnsTable = false),
  AS_DATA_TABLE("as.data.table"),
  AS_MATRIX("as.matrix", s3Function = true),
  AS_XTS("as.xts", s3Function = true, returnsTable = false),
  CUBE("cube", allowWithoutQuotes = listOf("j"), s3Function = true),
  DATA_TABLE("data.table", allowWithoutQuotes = listOf("...")) {
    override fun haveTableArguments(psiCall: RExpression, argumentList: List<RExpression>, runtimeInfo: RConsoleRuntimeInfo?) = true
  },
  DCAST("dcast", allowWithoutQuotes = listOf("formula"), tablesArguments = listOf("data"), s3Function = true),
  DUPLICATED("duplicated", s3Function = true, returnsTable = false),
  FINTERSECT("fintersect", tablesArguments = listOf("x", "y")),
  FOVERLAPS("foverlaps", tablesArguments = listOf("x", "y")),
  FRANK("frank", allowWithoutQuotes = listOf("..."), returnsTable = false),
  FRANKV("frankv", returnsTable = false),
  FROLLMEAN("frollmean", returnsTable = false), // experimental, might change
  FSETDIFF("fsetdiff", tablesArguments = listOf("x", "y")),
  FSETEQUAL("fsetequal", tablesArguments = listOf("x", "y"), returnsTable = false),
  FUNION("funion", tablesArguments = listOf("x", "y")),
  GROUPING_SETS("groupingsets", allowWithoutQuotes = listOf("j"), s3Function = true),
  INDICES("indices", returnsTable = false),
  MELT("melt", tablesArguments = listOf("data"), s3Function = true),
  MERGE("merge", tablesArguments = listOf("x", "y"), s3Function = true),
  NA_OMIT("na.omit", tablesArguments = listOf("object"), s3Function = true),
  PRINT("print", s3Function = true, returnsTable = false, ignoreInTransform = true),
  RBIND("rbind", tablesArguments = listOf("..."), s3Function = true, inheritedFunction = true, fullSuperFunctionName = "base::rbind"),
  //RBINDLIST("rbindlist", tablesArguments = listOf(???)), //Tables are enclosed in a list. It is not clear how to process it at this moment
  RLE_IDV("rleidv", returnsTable = false),
  ROLLUP("rollup", allowWithoutQuotes = listOf("j"), s3Function = true),
  ROW_IDV("rowidv", returnsTable = false),
  SET_ATTR("setattr", ignoreInTransform = true),
  SET_COL_ORDER("setcolorder", ignoreInTransform = true),
  SET_DF("setDF", ignoreInTransform = true),
  SET_INDEX("setindex", allowWithoutQuotes = listOf("..."), tablesArguments = emptyList(), ignoreInTransform = true),
  SET_INDEXV("setindexv", ignoreInTransform = true),
  SET_KEY("setkey", allowWithoutQuotes = listOf("..."), ignoreInTransform = true),
  SET_KEYV("setkeyv", ignoreInTransform = true),
  SET_NAMES("setnames", ignoreInTransform = true),
  SET_ORDER("setorder", allowWithoutQuotes = listOf("..."), ignoreInTransform = true),
  SET_ORDERV("setorderv", ignoreInTransform = true),
  SHIFT("shift", returnsTable = false),
  SPLIT("split", s3Function = true, returnsTable = false), //return list of tables
  SUBSET("subset", s3Function = true),
  TRANSFORM("transform", tablesArguments = listOf("`_data`"), allowWithoutQuotes = listOf("..."), s3Function = true),
  TRANSPOSE("transpose", tablesArguments = listOf("l")),
  UNIQUE("unique", s3Function = true),
  UNIQUE_N("uniqueN"),
  WITHIN("within", tablesArguments = listOf("data"), allowWithoutQuotes = listOf("expr", "..."), s3Function = true),

  DOUBLE_SUBSCRIPTION_OPERATOR("`[[.data.table`",
                               extraFunctionNames = listOf("\"[[.data.table\"", "`[[<-.data.table`", "\"[[<-.data.table\""),
                               contextType = TableManipulationContextType.SUBSCRIPTION, returnsTable = false, s3Function = true,
                               inheritedFunction = true, fullSuperFunctionName = "base:::`[[.data.frame`"),
  SUBSCRIPTION_OPERATOR("`[.data.table`",
                        extraFunctionNames = listOf("\"[.data.table\"", "`[<-.data.table`", "\"[<-.data.table\""),
                        allowWithoutQuotes = listOf("i", "j", "by"),
                        contextType = TableManipulationContextType.SUBSCRIPTION, s3Function = true);

  override fun isNeedCompletionInsideArgument(argumentList: List<RExpression>, currentArgument: TableManipulationArgument): Boolean {
    return true
  }

  override fun haveTableArguments(psiCall: RExpression, argumentList: List<RExpression>, runtimeInfo: RConsoleRuntimeInfo?): Boolean {
    return getTableArguments(psiCall, argumentList, runtimeInfo).isNotEmpty()
  }

  override fun isTableArgument(psiCall: RExpression,
                               argumentList: List<RExpression>,
                               currentArgument: TableManipulationArgument,
                               runtimeInfo: RConsoleRuntimeInfo?): Boolean {
    return tablesArguments.contains(getArgumentName(psiCall, argumentList, currentArgument, runtimeInfo) ?: return false)
  }

  fun getTableArguments(psiCall: RExpression, argumentList: List<RExpression>, runtimeInfo: RConsoleRuntimeInfo?): List<RExpression> {

    val resultList = mutableListOf<RExpression>()
    val argumentsNames = argumentList.mapNotNull { (it as? RNamedArgument)?.name }
    val parameters = getMissingParameters(psiCall, argumentsNames, runtimeInfo)

    var realIndex = 0
    for (argument in argumentList) {
      val argumentName = (argument as? RNamedArgument)?.name
      if (argumentName != null) {
        if (tablesArguments.contains(argumentName)) {
          argument.assignedValue?.let {
            resultList.add(it)
          }
        }
        continue
      }

      if (parameters == null || parameters.size <= realIndex) continue
      if (parameters[realIndex] == "...") {
        if (tablesArguments.contains("...")) {
          resultList.add(argument)
        }
        continue
      }

      if (tablesArguments.contains(parameters[realIndex])) {
        resultList.add(argument)
      }

      ++realIndex
    }

    return resultList
  }

  private fun getArgumentName(call: RExpression,
                              argumentList: List<RExpression>,
                              currentArgument: TableManipulationArgument,
                              runtimeInfo: RConsoleRuntimeInfo?): String? {
    currentArgument.name?.let { return it }

    val argumentsNames = argumentList.mapNotNull { (it as? RNamedArgument)?.name }
    val parameters = getMissingParameters(call, argumentsNames, runtimeInfo) ?: return null

    var realIndex = 0
    argumentList.forEachIndexed { currentIndex, argument ->
      if (parameters.size <= realIndex) {
        return null
      }

      if (parameters[realIndex] == "...") {
        return "..."
      }
      if (argument !is RNamedArgument) {
        if (currentIndex == currentArgument.index) {
          return parameters[realIndex]
        }

        ++realIndex
      }
    }

    return null
  }

  fun getArgumentByName(call: RExpression,
                        argumentList: List<RExpression>,
                        argumentName: String,
                        runtimeInfo: RConsoleRuntimeInfo?): RExpression? {
    val argumentsNames = argumentList.map { (it as? RNamedArgument)?.name }.apply {
      indexOf(argumentName).let {
        if (it != -1) return (argumentList[it] as RNamedArgument).assignedValue
      }
    }.filterNotNull()

    val parameters = getMissingParameters(call, argumentsNames, runtimeInfo) ?: return null

    var realIndex = 0
    for (argument in argumentList) {
      if (parameters.size <= realIndex) {
        return null
      }

      if (argument is RNamedArgument) continue

      if (parameters[realIndex] == argumentName) {
        return argument
      }

      ++realIndex
    }

    return null
  }

  fun isQuotesNeeded(call: RExpression,
                     argumentList: List<RExpression>,
                     currentArgument: TableManipulationArgument,
                     runtimeInfo: RConsoleRuntimeInfo): Boolean {
    val argumentName = getArgumentName(call, argumentList, currentArgument, runtimeInfo) ?: return true
    return !allowWithoutQuotes.contains(argumentName)
  }

  protected fun getMissingParameters(call: RExpression, argumentsNames: List<String>, runtimeInfo: RConsoleRuntimeInfo?): List<String>? {
    val allArgumentNames = if (s3Function) {
      if (runtimeInfo == null) return null
      val fullFunctionName = when {
        inheritedFunction -> fullSuperFunctionName ?: return emptyList()
        this == SUBSCRIPTION_OPERATOR || this == DOUBLE_SUBSCRIPTION_OPERATOR -> "data.table:::$functionName"
        else -> "data.table:::$functionName.data.table"
      }
      runtimeInfo.getFormalArguments(fullFunctionName)
    }
    else {
      RPsiUtil.resolveCall((call as? RCallExpression) ?: return null).firstOrNull()?.parameterNameList?.map { it }
    }
    return allArgumentNames?.filter { !argumentsNames.contains(it) }
  }

  //Inner functions
  // %between%
  // %inrange%
  // %like%
  // :=

  //Not inner
  // patterns (missing DT)
}