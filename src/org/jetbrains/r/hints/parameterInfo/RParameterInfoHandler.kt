/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints.parameterInfo

import com.intellij.lang.parameterInfo.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.r.RBundle
import org.jetbrains.r.parsing.RElementTypes
import org.jetbrains.r.psi.RElementFactory
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.RArgumentList
import org.jetbrains.r.psi.api.RCallExpression
import org.jetbrains.r.psi.api.RFunctionExpression

class RParameterInfoHandler : ParameterInfoHandler<RArgumentList, RParameterInfoHandler.RParameterInfoArgumentList> {

  override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): RArgumentList? {
    return findArgumentList(context, context.parameterListStart)
  }

  override fun findElementForParameterInfo(context: CreateParameterInfoContext): RArgumentList? {
    val argumentList = findArgumentList(context) ?: return null
    context.itemsToShow = RPsiUtil.resolveCall(argumentList.parent as RCallExpression).map { assignment ->
      val rFunctionExpression = RElementFactory.createRPsiElementFromText(context.project,
                                                                          "function${assignment.functionParameters}") as RFunctionExpression
      val args = rFunctionExpression.parameterList?.parameterList ?: emptyList()
      val names = args.map { it.name }
      val values = args.map { it.defaultValue?.text }
      RParameterInfoArgumentList(names, values, names.indices.toList())
    }.toTypedArray()

    return argumentList
  }

  override fun showParameterInfo(element: RArgumentList, context: CreateParameterInfoContext) {
    context.showHint(element, element.textOffset, this)
  }

  override fun updateParameterInfo(parameterOwner: RArgumentList, context: UpdateParameterInfoContext) {
    val carriageOffset = context.offset
    if (!parameterOwner.textRange.contains(carriageOffset)) {
      context.removeHint()
      return
    }

    context.objectsToView.map { it as RParameterInfoArgumentList }.forEach {
      val names = it.names
      it.currentArgumentIndex = 0
      it.isDisabled = false

      val argumentPermutationInfo = RArgumentInfo.getArgumentInfo(parameterOwner, names)
      val expressions = argumentPermutationInfo.expressionListWithPipeExpression
      val permutation = argumentPermutationInfo.argumentPermutationIndWithPipeExpression
      val notPassedParameters = argumentPermutationInfo.notPassedParameterInd
      if (!argumentPermutationInfo.isValid) {
        it.isDisabled = true
        it.permutation = names.indices.toList()
        return
      }

      val probableNewPermutation = mutableListOf<Int>()
      permutation.forEachIndexed { ind, permInd ->
        if (probableNewPermutation.lastOrNull() != permInd) probableNewPermutation.add(permInd)

        val arg = expressions[ind]
        val stOffset = findPrevCommaOffset(arg) + 1
        val fnOffset = findNextCommaOffset(arg)
        if (carriageOffset in stOffset..fnOffset) it.currentArgumentIndex = probableNewPermutation.lastIndex
      }

      it.permutation = probableNewPermutation + notPassedParameters
    }
  }

  override fun updateUI(argumentList: RParameterInfoArgumentList, context: ParameterInfoUIContext) {
    var highlightOffsetStart = -1
    var highlightOffsetEnd = 0
    val isDisabled = argumentList.isDisabled
    var text = buildString {
      val (names, defaultValues, permutation, currentArgumentIndex) = argumentList
      for (i in permutation.indices) {
        if (i != 0) append(", ")
        if (!isDisabled && currentArgumentIndex == i) highlightOffsetStart = length

        val argumentInd = permutation[i]
        if (argumentInd != i) append("[")
        append(names[argumentInd])
        defaultValues[argumentInd]?.let {
          val renderedValue = when {
            it.length <= MAX_DEFAULT_VALUE_LEN -> it
            it.startsWith("\"") -> "\"$DOTS\""
            it.startsWith("'") -> "'$DOTS'"
            else -> DOTS
          }
          append(" = $renderedValue")
        }
        if (argumentInd != i) append("]")
        if (!isDisabled && currentArgumentIndex == i) highlightOffsetEnd = length
      }
    }

    if (text.isEmpty()) text = RBundle.message("parameter.info.no.parameters")
    context.setupUIComponentPresentation(text, highlightOffsetStart, highlightOffsetEnd, isDisabled,
                                         false, false, context.getDefaultParameterColor())
  }

  private fun findArgumentList(context: ParameterInfoContext, parameterListStart: Int = -1): RArgumentList? {
    var argumentList = ParameterInfoUtils.findParentOfType(context.file, context.offset - 1, RArgumentList::class.java)
    if (argumentList == null || parameterListStart < 0) return argumentList

    while (parameterListStart != argumentList!!.textRange.startOffset) {
      argumentList = PsiTreeUtil.getParentOfType(argumentList, RArgumentList::class.java) ?: return null
      if (parameterListStart > argumentList.textRange.startOffset) return null
    }

    return argumentList
  }

  private fun findPrevCommaOffset(arg: PsiElement): Int {
    var child = arg.prevSibling
    while (child != null) {
      if (child.elementType == RElementTypes.R_COMMA) return child.textOffset
      child = child.prevSibling
    }

    return Int.MIN_VALUE
  }

  private fun findNextCommaOffset(arg: PsiElement): Int {
    var child = arg.nextSibling
    while (child != null) {
      if (child.elementType == RElementTypes.R_COMMA) return child.textOffset
      child = child.nextSibling
    }

    return Int.MAX_VALUE
  }

  data class RParameterInfoArgumentList(var names: List<String>,
                                        val defaultValues: List<String?>,
                                        var permutation: List<Int>,
                                        var currentArgumentIndex: Int = -1,
                                        var isDisabled: Boolean = false)

  companion object {
    private const val DOTS = "..."
    private const val MAX_DEFAULT_VALUE_LEN = 32
  }
}