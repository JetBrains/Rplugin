package org.jetbrains.r.codeInsight.table

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.psi.*
import org.jetbrains.r.psi.api.RExpression
import org.jetbrains.r.psi.api.RStringLiteralExpression
import java.util.function.Consumer

abstract class RTableManipulationAnalyzerManager<T : TableManipulationFunction> : RTableContextManager {
  override fun processColumnsInContext(context: PsiElement, processor: Processor<TableColumnInfo>): Boolean {
    val runtimeInfo = context.containingFile.originalFile.runtimeInfo ?: return true
    val tableContextInfo = getTableManipulationAnalyzer().getContextInfo(context, runtimeInfo) ?: return true

    if (!processCurrentColumns(tableContextInfo, processor)) {
      return false
    }

    return processColumnsOfTables(context, tableContextInfo.callInfo.passedTableArguments, processor, tableContextInfo)
  }

  fun processColumnsOfTables(context: PsiElement,
                             tables: List<RExpression>,
                             processor: Processor<TableColumnInfo>,
                             tableContextInfo: TableManipulationContextInfo<T>? = null): Boolean {
    if (tables.isEmpty()) {
      return true
    }
    val runtimeInfo = tables.first().containingFile.originalFile.runtimeInfo ?: return true
    val tableAnalyser = getTableManipulationAnalyzer()
    val collectProcessor = RTableColumnCollectProcessor()

    var allTableColumnsMustBeQuoted = false
    val tableInfos = tables.map { tableAnalyser.getTableColumns(it, runtimeInfo) }
    if (tableContextInfo != null) {
      val tableCallInfo = tableContextInfo.callInfo
      val isCorrectTableType = tableInfos.all { it.type == tableAnalyser.tableColumnType }
      val isQuotesNeeded = !isCorrectTableType && tableAnalyser.isSubscription(tableCallInfo.function)
                           || tableCallInfo.function.isQuotesNeeded(tableCallInfo.argumentInfo, tableContextInfo.currentTableArgument)
      if (!isQuotesNeeded && context.parent is RStringLiteralExpression) return true
      allTableColumnsMustBeQuoted = isQuotesNeeded && context.parent !is RStringLiteralExpression
    }

    tableInfos.map { it.columns }.flatten().forEach(Consumer { t ->
      t.quoteNeeded = allTableColumnsMustBeQuoted || columnMustBeQuoted(t.name)
      collectProcessor.process(t)
    })
    tables.forEach(Consumer { table -> tableAnalyser.processStaticTableColumns(table, collectProcessor)})

    for (column in collectProcessor.results) {
      if (!processor.process(column)) {
        return false
      }
    }

    return true
  }

  abstract fun getTableManipulationAnalyzer(): TableManipulationAnalyzer<T>

  protected open fun processCurrentColumns(tableContextInfo: TableManipulationContextInfo<T>,
                                           processor: Processor<TableColumnInfo>): Boolean {
    return true
  }

  private fun columnMustBeQuoted(columnName: String): Boolean {
    return !StringUtil.isJavaIdentifier(columnName)
  }
}