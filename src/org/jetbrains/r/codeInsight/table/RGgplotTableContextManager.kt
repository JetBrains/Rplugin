package org.jetbrains.r.codeInsight.table

import com.intellij.psi.PsiElement
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.Processor
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.psi.TableColumnInfo
import org.jetbrains.r.psi.api.*

class RGgplotTableContextManager : RTableContextManager {
  override fun processColumnsInContext(context: PsiElement, processor: Processor<TableColumnInfo>): Boolean {
    if (!METHOD_TAKING_COLUMN_PARAMETER_FILTER.accepts(context)) {
      return true
    }

    val expression = PsiTreeUtil.getParentOfType(context, RExpression::class.java, false) ?: return true

    val parent = expression.parent
    val aesCall = (if (parent is RNamedArgument) parent.parent.parent else parent.parent) as RCallExpression
    val ggplotCall = (if (aesCall.parent is RNamedArgument) aesCall.parent.parent.parent else aesCall.parent.parent) as? RCallExpression
    // handle ggplot(data, aes(<caret>)) case
    if (ggplotCall != null && ggplotCall.expression.text == "ggplot") {
      return processGgplotCall(ggplotCall, processor)
    } else {
      // handle ggplot(data, aes(P1, P2)) + geom_point(aes(<caret>), size=2)
      return processPlusOperator(aesCall, processor)
    }
  }

  private fun processPlusOperator(aesCall: RCallExpression, processor: Processor<TableColumnInfo>): Boolean {
    var parent = aesCall.parent
    var operator: ROperatorExpression? = null
    while (parent != null && parent !is RFile && parent !is RFunctionExpression) {
      if (parent is ROperatorExpression && parent.operator.elementType == RElementTypes.R_PLUSMINUS_OPERATOR) operator = parent
      parent = parent.parent
    }
    if (operator is ROperatorExpression) {
      while (operator?.leftExpr is ROperatorExpression) {
        operator = operator.leftExpr as? ROperatorExpression
      }
      val left = operator?.leftExpr
      if (left is RCallExpression && left.expression.text == "ggplot") {
        return processGgplotCall(left, processor)
      } else {
        val runtimeInfo = aesCall.containingFile.originalFile.runtimeInfo ?: return true
        val loadTableColumns = runtimeInfo.loadTableColumns(left?.text.toString() + "$" + "data")
        for (column in loadTableColumns.columns) {
          if (!processor.process(column)) {
            return false
          }
        }
      }
    }
    return true
  }

  private fun processGgplotCall(ggplotCall: RCallExpression, processor: Processor<TableColumnInfo>): Boolean {
    val dataParameter = RParameterInfoUtil.getArgumentByName(ggplotCall, "data") ?: return true

    val collectProcessor = RTableColumnCollectProcessor()
    RDplyrTableContextManager().processColumnsOfTables(ggplotCall, listOf(dataParameter), collectProcessor)
    RDataTableContextManager().processColumnsOfTables(ggplotCall, listOf(dataParameter), collectProcessor)

    for (column in collectProcessor.results) {
      if (!processor.process(column)) {
        return false
      }
    }

    return true
  }
}

val METHOD_TAKING_COLUMN_PARAMETER_FILTER = FilterPattern(MethodTakingColumnsFilter())

val GGPLOT_METHODS_WITH_COLUMNS = listOf("aes", "facet_grid", "facet_wrap")

class MethodTakingColumnsFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, RExpression::class.java, false)
    if (expression !is RIdentifierExpression) return false
    val parent = expression.parent ?: return false
    if (!(parent is RNamedArgument && parent.assignedValue == expression || parent is RArgumentList)) return false
    val columnContainerCall = if (parent is RNamedArgument) parent.parent.parent else parent.parent
    if (columnContainerCall !is RCallExpression) return false
    val columnContainerCallIdentifier = columnContainerCall.expression
    if (columnContainerCallIdentifier !is RIdentifierExpression
        || columnContainerCallIdentifier.name !in GGPLOT_METHODS_WITH_COLUMNS) return false
    return true
  }

  override fun isClassAcceptable(hintClass: Class<*>?): Boolean = true
}