/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.CommonProcessors
import com.intellij.util.ProcessingContext
import org.jetbrains.r.console.runtimeInfo
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.psi.RDataTableAnalyzer
import org.jetbrains.r.psi.RDplyrAnalyzer
import org.jetbrains.r.psi.TableColumnInfo
import org.jetbrains.r.psi.api.*
import java.util.function.Consumer

class GGPlot2AesColumnCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val expression = PsiTreeUtil.getParentOfType(parameters.position, RExpression::class.java, false) ?: return
    val parent = expression.parent
    val aesCall = (if (parent is RNamedArgument) parent.parent.parent else parent.parent) as RCallExpression
    val ggplotCall = (if (aesCall.parent is RNamedArgument) aesCall.parent.parent.parent else aesCall.parent.parent) as? RCallExpression
    // handle ggplot(data, aes(<caret>)) case
    if (ggplotCall != null && ggplotCall.expression.text == "ggplot") {
      addCompletionFromGGPlotCall(ggplotCall, parameters, result)
    } else {
      // handle ggplot(data, aes(P1, P2)) + geom_point(aes(<caret>), size=2)
      handlePlusOperator(aesCall, parameters, result)
    }

  }

  private fun handlePlusOperator(aesCall: RCallExpression,
                                 parameters: CompletionParameters,
                                 result: CompletionResultSet) {
    var parent = aesCall.parent
    var operator: ROperatorExpression? = null
    while (parent != null && parent !is RFile && parent !is RFunctionExpression) {
      if (parent is ROperatorExpression && parent.operator.elementType == RElementTypes.R_PLUSMINUS_OPERATOR) operator = parent
      parent = parent.parent
    }
    if (operator is ROperatorExpression) {
      while (operator?.leftExpr is ROperatorExpression) {
        operator = operator?.leftExpr as? ROperatorExpression
      }
      val left = operator?.leftExpr
      if (left is RCallExpression && left.expression.text == "ggplot") {
        addCompletionFromGGPlotCall(left, parameters, result)
      } else {
        val runtimeInfo = parameters.originalFile.runtimeInfo ?: return
        val loadTableColumns = runtimeInfo.loadTableColumns(left?.text.toString() + "$" + "data")
        addCompletionResults(result, loadTableColumns.columns)
      }
    }
  }

  private fun addCompletionFromGGPlotCall(ggplotCall: RCallExpression,
                                          parameters: CompletionParameters,
                                          result: CompletionResultSet) {
    val runtimeInfo = parameters.originalFile.runtimeInfo ?: return
    val dataParameter = RParameterInfoUtil.getArgumentByName(ggplotCall, "data") ?: return // data argument
    val tableInfo = RDataTableAnalyzer.getTableColumns(dataParameter, runtimeInfo)

    val collectProcessor = CommonProcessors.CollectProcessor<TableColumnInfo>()
    tableInfo.columns.forEach(Consumer { collectProcessor.process(it) })

    RDataTableAnalyzer.processStaticTableColumns(dataParameter, collectProcessor)
    RDplyrAnalyzer.processStaticTableColumns(dataParameter, collectProcessor)

    addCompletionResults(result, collectProcessor.results)
  }

  private fun addCompletionResults(result: CompletionResultSet,
                                   columns: Collection<TableColumnInfo>) {
    result.addAllElements(columns.map {
      PrioritizedLookupElement.withPriority(RLookupElement(it.name, true, AllIcons.Nodes.Field, packageName = it.type),
                                            TABLE_MANIPULATION_PRIORITY)
    })
  }
}

val AES_PARAMETER_FILTER = FilterPattern(AesFilter())

class AesFilter : ElementFilter {
  override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
    val expression = PsiTreeUtil.getParentOfType(context, RExpression::class.java, false)
    if (expression !is RIdentifierExpression) return false
    val parent = expression.parent ?: return false
    if (!(parent is RNamedArgument && parent.assignedValue == expression || parent is RArgumentList)) return false
    val aesCall = if (parent is RNamedArgument) parent.parent.parent else parent.parent
    if (aesCall !is RCallExpression) return false
    val aesIdentifier = aesCall.expression
    if (aesIdentifier !is RIdentifierExpression || aesIdentifier.name != "aes") return false
    return true
  }

  override fun isClassAcceptable(hintClass: Class<*>?): Boolean = true
}
