/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi

import com.intellij.psi.PsiElement
import com.intellij.util.CommonProcessors
import com.intellij.util.Processor
import org.jetbrains.r.codeInsight.table.RTableColumnCollectProcessor
import org.jetbrains.r.console.RConsoleRuntimeInfo
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.psi.RDataTableAnalyzer.processSetnames
import org.jetbrains.r.psi.RDataTableAnalyzer.processSubscription
import org.jetbrains.r.psi.TableManipulationAnalyzer.Companion.processAllDotsColumns
import org.jetbrains.r.psi.TableManipulationAnalyzer.Companion.processOperandColumns
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.references.RResolveUtil
import org.jetbrains.r.refactoring.RNamesValidator

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
  override val tableColumnType = TableType.DATA_TABLE

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
    if (isSafe(expr, runtimeInfo)) {
      if (!preserveRows) command.append("data.table:::`[.data.table`(")
      command.append("(").append(expr.text).append(")")
      if (!preserveRows) command.append(",FALSE)")
    }
    else {
      command.append("(data.table::data.table())")
    }
  }

  fun processSetnames(operandProcessorRunner: ProcessOperandColumnRunner?,
                      @Suppress("UNUSED_PARAMETER") expression: RExpression,
                      callInfo: TableManipulationCallInfo<*>,
                      processor: Processor<TableColumnInfo>): Boolean {
    val collectProcessor = CommonProcessors.CollectProcessor<TableColumnInfo>()
    if (operandProcessorRunner != null) {
      operandProcessorRunner(collectProcessor)
    }
    var result = ArrayList<TableColumnInfo>()
    result.addAll(collectProcessor.results)
    val oldColumnNames = getColumnsFromArgument(callInfo.argumentInfo.getArgumentPassedToParameter("old")).mapNotNull { t -> t.name }
    result = ArrayList(result.filter {t -> t.name !in oldColumnNames})

    getColumnsFromArgument(callInfo.argumentInfo.getArgumentPassedToParameter("new")).forEach {
      val columnName = it.name
      if (columnName != null) {
        result.add(TableColumnInfo(columnName, definition = it))
      }
    }

    return result.all { processor.process(it) }
  }

  override fun processTableFromVariable(variable: RIdentifierExpression, processor: Processor<TableColumnInfo>): Boolean {
    val precedingModification = RResolveUtil.findPrecedingInstruction(variable) { element: PsiElement ->
      if (element is RAssignmentStatement && element.name == variable.name) {
        return@findPrecedingInstruction element.assignedValue
      }
      if (element is RSubscriptionExpression) {
        val nameElement = element.firstChild
        if (nameElement is RIdentifierExpression && nameElement.name == variable.name) {
          return@findPrecedingInstruction element
        }
      }
      return@findPrecedingInstruction null
    }
    if (precedingModification is RExpression) {
      return processStaticTableColumns(precedingModification, processor)
    }
    return true
  }

  fun processSubscription(operandProcessorRunner: ProcessOperandColumnRunner?,
                          @Suppress("UNUSED_PARAMETER") expression: RExpression,
                          callInfo: TableManipulationCallInfo<*>,
                          processor: Processor<TableColumnInfo>): Boolean {
    val collectProcessor = RTableColumnCollectProcessor()
    if (operandProcessorRunner != null ) {
      operandProcessorRunner(collectProcessor)
    }
    val parameterJ = callInfo.argumentInfo.getArgumentPassedToParameter("j")
    val arguments = ArrayList<PsiElement>()

    when (parameterJ) {
      is RIdentifierExpression -> {
        arguments.add(parameterJ)
      }
      is RCallExpression -> {
        if (parameterJ.isFunctionFromLibrarySoft("c", "base") || parameterJ.isFunctionFromLibrarySoft(".", "plyr")) {
          parameterJ.argumentList.expressionList.forEach{ arguments.add(it) }
        }
      }
    }

    val processedColumns = ArrayList<String>()
    for (argument in arguments) {
      if (argument is RIdentifierExpression) {
        processedColumns.add(argument.text)
        if (!processor.process(TableColumnInfo(argument.text, definition = argument))) {
          return false
        }
      }
      else if (argument is RStringLiteralExpression) {
        val stringValue = argument.name
        if (stringValue != null) {
          processedColumns.add(stringValue)
          if (!processor.process(TableColumnInfo(stringValue, definition = argument))) {
            return false
          }
        }
      }
      else if (argument is RAssignmentStatement && argument.assignee is RIdentifierExpression) {
        val columnName = argument.assignee!!.text
        processedColumns.add(columnName)
        if (argument.assignedValue !is RNullLiteral) {
          if (!processor.process(TableColumnInfo(columnName, definition = argument))) {
            return false
          }
        }
      }
    }

    for (column in collectProcessor.results) {
      if (column.name !in processedColumns) {
        if (!processor.process(column)) {
          return false
        }
      }
    }
    return true
  }

  private fun getColumnsFromArgument(argument: RExpression?): Collection<RStringLiteralExpression> {
    if (argument == null) {
      return emptySet()
    }

    if (argument is RStringLiteralExpression) {
      return listOf(argument)
    } else if (argument is RCallExpression && argument.expression.name == "c") {
      // c("column1", "column2")
      val result = ArrayList<RStringLiteralExpression>()
      for (cArgument in argument.argumentList.expressionList) {
        if (cArgument is RStringLiteralExpression) {
          result.add(cArgument)
        }
      }
      return result
    }
    return emptySet()
  }
}

@Suppress("SpellCheckingInspection")
enum class DataTableFunction(
  override val functionName: String? = null,
  override val returnsTable: Boolean = true,
  override val ignoreInTransform: Boolean = false,
  override val rowsOmittingUnavailable: Boolean = false,
  override val inheritedFunction: Boolean = false,
  override val fullSuperFunctionName: String? = null,
  val extraFunctionNames: List<String> = emptyList(),
  val allowWithoutQuotes: List<String> = emptyList(),
  override val s3Function: Boolean = false,
  override val tableArguments: List<String> = listOf("x"),
  override val tableColumnsProvider: TableColumnsProvider = ::processOperandColumns
) : TableManipulationFunction {
  ALL_EQUAL("all.equal", tableArguments = listOf("target", "current"), s3Function = true, returnsTable = false),
  ALLOC_COL("alloc.col", tableArguments = listOf("DT"), returnsTable = false), // experimental, might change
  ANY_DUPLICATED("anyDuplicated", s3Function = true, returnsTable = false),
  AS_DATA_TABLE("as.data.table"),
  AS_MATRIX("as.matrix", s3Function = true),
  AS_XTS("as.xts", s3Function = true, returnsTable = false),
  CUBE("cube", allowWithoutQuotes = listOf("j"), s3Function = true),
  DATA_TABLE("data.table", allowWithoutQuotes = listOf("..."), tableColumnsProvider = ::processAllDotsColumns),
  DCAST("dcast", allowWithoutQuotes = listOf("formula"), tableArguments = listOf("data"), s3Function = true),
  DUPLICATED("duplicated", s3Function = true, returnsTable = false),
  FINTERSECT("fintersect", tableArguments = listOf("x", "y")),
  FOVERLAPS("foverlaps", tableArguments = listOf("x", "y")),
  FRANK("frank", allowWithoutQuotes = listOf("..."), returnsTable = false),
  FRANKV("frankv", returnsTable = false),
  FROLLMEAN("frollmean", returnsTable = false), // experimental, might change
  FSETDIFF("fsetdiff", tableArguments = listOf("x", "y")),
  FSETEQUAL("fsetequal", tableArguments = listOf("x", "y"), returnsTable = false),
  FUNION("funion", tableArguments = listOf("x", "y")),
  GROUPING_SETS("groupingsets", allowWithoutQuotes = listOf("j"), s3Function = true),
  INDICES("indices", returnsTable = false),
  MELT("melt", tableArguments = listOf("data"), s3Function = true),
  MERGE("merge", tableArguments = listOf("x", "y"), s3Function = true),
  NA_OMIT("na.omit", tableArguments = listOf("object"), s3Function = true),
  PRINT("print", s3Function = true, returnsTable = false, ignoreInTransform = true),
  RBIND(
    "rbind",
    tableArguments = listOf("..."),
    s3Function = true,
    inheritedFunction = true,
    fullSuperFunctionName = "base::rbind"
  ),

  //RBINDLIST("rbindlist", tablesArguments = listOf(???)), //Tables are enclosed in a list. It is not clear how to process it at this moment
  RLE_IDV("rleidv", returnsTable = false),
  ROLLUP("rollup", allowWithoutQuotes = listOf("j"), s3Function = true),
  ROW_IDV("rowidv", returnsTable = false),
  SET_ATTR("setattr", ignoreInTransform = true),
  SET_COL_ORDER("setcolorder", ignoreInTransform = true),
  SET_DF("setDF", ignoreInTransform = true),
  SET_INDEX("setindex", allowWithoutQuotes = listOf("..."), tableArguments = emptyList(), ignoreInTransform = true),
  SET_INDEXV("setindexv", ignoreInTransform = true),
  SET_KEY("setkey", allowWithoutQuotes = listOf("..."), ignoreInTransform = true),
  SET_KEYV("setkeyv", ignoreInTransform = true),
  SET_NAMES("setnames", ignoreInTransform = true, tableColumnsProvider = ::processSetnames),
  SET_ORDER("setorder", allowWithoutQuotes = listOf("..."), ignoreInTransform = true),
  SET_ORDERV("setorderv", ignoreInTransform = true),
  SHIFT("shift", returnsTable = false),
  SPLIT("split", s3Function = true, returnsTable = false), //return list of tables
  SUBSET("subset", s3Function = true),
  TRANSFORM("transform", tableArguments = listOf("`_data`"), allowWithoutQuotes = listOf("..."), s3Function = true),
  TRANSPOSE("transpose", tableArguments = listOf("l"), rowsOmittingUnavailable = true),
  UNIQUE("unique", s3Function = true),
  UNIQUE_N("uniqueN"),
  WITHIN("within", tableArguments = listOf("data"), allowWithoutQuotes = listOf("expr", "..."), s3Function = true),

  DOUBLE_SUBSCRIPTION_OPERATOR(
    "`[[.data.table`",
    extraFunctionNames = listOf("\"[[.data.table\"", "`[[<-.data.table`", "\"[[<-.data.table\""),
    returnsTable = false, s3Function = true,
    inheritedFunction = true, fullSuperFunctionName = "base:::`[[.data.frame`"),
  SUBSCRIPTION_OPERATOR(
    "`[.data.table`",
    extraFunctionNames = listOf("\"[.data.table\"", "`[<-.data.table`", "\"[<-.data.table\""),
    allowWithoutQuotes = listOf("i", "j", "by"), s3Function = true, tableColumnsProvider = ::processSubscription);

  override fun isQuotesNeeded(argumentInfo: RArgumentInfo, currentArgument: RExpression): Boolean {
    val parameterName = argumentInfo.getParameterNameForArgument(currentArgument)
    return !allowWithoutQuotes.contains(parameterName)
  }

  override fun isComplexColumnName(columnName: String): Boolean {
    return !RNamesValidator.isIdentifier(columnName)
  }

  //Inner functions
  // %between%
  // %inrange%
  // %like%
  // :=

  //Not inner
  // patterns (missing DT)
}