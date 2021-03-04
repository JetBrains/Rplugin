package org.jetbrains.r.codeInsight.table

import com.intellij.psi.PsiElement
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.impl.PsiElementBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.Processor
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.psi.TableColumnInfo
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isFunctionFromLibrarySoft

class RGgplotTableContextManager : RTableContextManager {
  override fun processColumnsInContext(context: PsiElement, processor: Processor<TableColumnInfo>): Boolean {
    if (!METHOD_TAKING_COLUMN_PARAMETER_FILTER.accepts(context)) {
      return true
    }

    val call = PsiTreeUtil.getParentOfType(context, RCallExpression::class.java, false) ?: return true
    if (call.isFunctionFromLibrarySoft("qplot", "ggplot2")) {
      // handle qplot(x = cty, y = hwy, data = mpg, geom = "point")
      return processColumnsFromDataArgument(call, processor)
    }

    val ggplotCall = (if (call.parent is RNamedArgument) call.parent.parent.parent else call.parent.parent) as? RCallExpression
    if (ggplotCall != null && ggplotCall.isFunctionFromLibrarySoft("ggplot", "ggplot2")) {
      // handle ggplot(data, aes(<caret>)) case
      return processColumnsFromDataArgument(ggplotCall, processor)
    }
    else  {
      // handle ggplot(data, aes(P1, P2)) + geom_point(aes(<caret>), size=2)
      return processPlusOperator(call, processor)
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
        return processColumnsFromDataArgument(left, processor)
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

  private fun processColumnsFromDataArgument(ggplotCall: RCallExpression, processor: Processor<TableColumnInfo>): Boolean {
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

val GGPLOT_COLUMN_ARGUMENTS = mapOf(
  "aes" to emptyList(), // emptyList when it's not a RNamedArgument
  "facet_grid" to emptyList(),
  "facet_wrap" to emptyList(),
  "qplot" to listOf("x", "y"),
)

class MethodTakingColumnsFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    var expression: PsiElement? = PsiTreeUtil.getParentOfType(context, RExpression::class.java, false)

    if (expression !is RIdentifierExpression) return false
    val parent = expression.parent ?: return false
    if (parent is RNamedArgument && parent.assignedValue == expression) {
      expression = parent
    }
    if (expression !is PsiElementBase) {
      return false
    }

    val argumentList = expression.parent ?: return false
    if (argumentList !is RArgumentList) return false

    val columnContainerCall = argumentList.parent
    if (columnContainerCall !is RCallExpression) return false
    val columnContainerCallIdentifier = columnContainerCall.expression
    if (columnContainerCallIdentifier !is RIdentifierExpression
        || columnContainerCallIdentifier.name !in GGPLOT_COLUMN_ARGUMENTS.keys) return false
    val columnArgumentArgumentCriterion = GGPLOT_COLUMN_ARGUMENTS[columnContainerCallIdentifier.name]
    return columnArgumentArgumentCriterion!!.isEmpty() || expression.name in columnArgumentArgumentCriterion
  }

  override fun isClassAcceptable(hintClass: Class<*>?): Boolean = true
}