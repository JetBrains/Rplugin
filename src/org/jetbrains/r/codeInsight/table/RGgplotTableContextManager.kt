package org.jetbrains.r.codeInsight.table

import com.intellij.psi.PsiElement
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.impl.PsiElementBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo
import org.jetbrains.r.psi.TableColumnInfo
import org.jetbrains.r.psi.api.*
import org.jetbrains.r.psi.isFunctionFromLibrarySoft
import org.jetbrains.r.psi.references.RResolveUtil

class RGgplotTableContextManager : RTableContextManager {
  override fun processColumnsInContext(context: PsiElement, processor: Processor<TableColumnInfo>): Boolean {
    if (!METHOD_TAKING_COLUMN_PARAMETER_FILTER.accepts(context)) {
      return true
    }

    val call = PsiTreeUtil.getParentOfType(context, RCallExpression::class.java, false) ?: return true
    return processElement(call, processor)
  }

  private fun isGgplotDefineCall(expression: RCallExpression) = expression.isFunctionFromLibrarySoft("qplot|ggplot", "ggplot2")

  /**
   * Looks element that defines the table ggplot working on. And processes the columns of the table
   */
  private fun processElement(element: PsiElement, processor: Processor<TableColumnInfo>): Boolean {
    var currentElement: PsiElement? = element
    while (currentElement != null && currentElement !is RFunctionExpression) {
      if (currentElement is RCallExpression) {
        if (isGgplotDefineCall(currentElement)) {
          // handle qplot(x = cty, y = hwy, data = mpg, geom = "point") or ggplot(mpg, aes(cty, hwy))
          return processColumnsFromDataArgument(currentElement, processor)
        }
      } else if (currentElement is ROperatorExpression && currentElement.isBinary && currentElement.operator?.name == "+") {
        return processPlusOperator(currentElement, processor)
      } else if (currentElement is RIdentifierExpression) {
        return processGgplotVariable(currentElement, processor)
      }
      currentElement = currentElement.parent
    }
    return true
  }

  /**
   * Processes ggplot plus operation in order to find the table
   */
  private fun processPlusOperator(operator: ROperatorExpression, processor: Processor<TableColumnInfo>): Boolean {
    val left = operator.leftExpr
    if (left != null) {
      return processElement(left, processor)
    }
    return true
  }

  /**
   * Processes columns of the defined by variable
   */
  private fun processGgplotVariable(variable: RIdentifierExpression, processor: Processor<TableColumnInfo>): Boolean {
    val precedingModification = RResolveUtil.findPrecedingInstruction(variable) { element: PsiElement ->
      if (element is RAssignmentStatement && element.name == variable.name) {
        return@findPrecedingInstruction element.assignedValue
      }
      return@findPrecedingInstruction null
    }
    if (precedingModification is RExpression) {
      return processElement(precedingModification, processor)
    }

    return processElementByConsole(variable, processor)
  }

  /**
   * Retrieves columns of the variable from the console
   */
  private fun processElementByConsole(variable: RIdentifierExpression, processor: Processor<TableColumnInfo>): Boolean {
    val runtimeInfo = variable.containingFile.originalFile.runtimeInfo ?: return true
    val loadTableColumns = runtimeInfo.loadTableColumns(variable.name + "$" + "data")
    for (column in loadTableColumns.columns) {
      if (!processor.process(column)) {
        return false
      }
    }
    return true
  }

  private fun processColumnsFromDataArgument(ggplotCall: RCallExpression, processor: Processor<TableColumnInfo>): Boolean {
    val dataParameter = RArgumentInfo.getArgumentByName(ggplotCall, "data") ?: return true

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
  "vars" to emptyList()
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