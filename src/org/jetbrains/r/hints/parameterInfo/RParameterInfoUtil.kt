/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints.parameterInfo

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.DataInputOutputUtilRt
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.io.StringRef
import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*

/**
 * @property argumentNames Names of arguments that can be passed directly to **...**
 * @property functionArgNames Names of arguments that are functions whose arguments can also be passed to **...**
 */
data class RExtraNamedArgumentsInfo(val argumentNames: List<String>, val functionArgNames: List<String>) {
  fun serialize(dataStream: StubOutputStream) {
    DataInputOutputUtilRt.writeSeq(dataStream, argumentNames) { dataStream.writeName(it) }
    DataInputOutputUtilRt.writeSeq(dataStream, functionArgNames) { dataStream.writeName(it) }
  }

  companion object {
    fun deserialize(dataStream: StubInputStream): RExtraNamedArgumentsInfo {
      val argNames = DataInputOutputUtilRt.readSeq(dataStream) { StringRef.toString(dataStream.readName()) }
      val funArgNames = DataInputOutputUtilRt.readSeq(dataStream) { StringRef.toString(dataStream.readName()) }
      return RExtraNamedArgumentsInfo(argNames, funArgNames)
    }
  }
}

class RArgumentInfo private constructor(argumentList: RArgumentHolder, val parameterNames: List<String>) {
  private val pipeArgument: RExpression? = findPipeArgument(argumentList)
  val argumentPermutationIndWithPipeExpression: List<Int>
  val notPassedParameterInd: List<Int>

  val expressionListWithPipeExpression = argumentList.expressionList.let {
    if (pipeArgument != null) listOf(pipeArgument) + it else it
  }
  val expressionList = dropPipeValue(expressionListWithPipeExpression)

  init {
    var curArgIndex = 0
    val skipNames = mutableSetOf<String>()
    val argumentNames = argumentList.namedArgumentList.map { it.name }
    val resultPermutation = mutableListOf<Int>()
    for (arg in expressionListWithPipeExpression) {
      if (arg is RNamedArgument) {
        if (arg.name in skipNames) {
          // Multiple named argument with same name
          resultPermutation.add(-1)
          continue
        }

        var ind = parameterNames.indexOf(arg.name)
        if (ind == -1) {
          ind = parameterNames.indexOf(DOTS)
          if (ind == -1) {
            // Unused argument
            resultPermutation.add(-1)
            continue
          }
        }
        else {
          skipNames.add(arg.name)
        }

        resultPermutation.add(ind)
      }
      else {
        while (curArgIndex < parameterNames.size && (parameterNames[curArgIndex] in skipNames || parameterNames[curArgIndex] in argumentNames)) ++curArgIndex
        if (curArgIndex == parameterNames.size) {
          // Too many arguments
          resultPermutation.add(-1)
          continue
        }
        if (parameterNames[curArgIndex] != DOTS) skipNames.add(parameterNames[curArgIndex])
        resultPermutation.add(curArgIndex)
      }
    }

    val notPassedParameters = mutableListOf<Int>()
    parameterNames.forEachIndexed { ind, name ->
      if (name != DOTS && name !in skipNames || name == DOTS && ind !in resultPermutation) notPassedParameters.add(ind)
    }

    this.argumentPermutationIndWithPipeExpression = resultPermutation
    this.notPassedParameterInd = notPassedParameters
  }

  val argumentNamesWithPipeExpression by lazy { argumentPermutationIndWithPipeExpression.map { parameterNames.getOrNull(it) } }

  val argumentNames by lazy { dropPipeValue(argumentNamesWithPipeExpression) }
  val argumentPermutationInd by lazy { dropPipeValue(argumentPermutationIndWithPipeExpression) }
  val notPassedParameterNames by lazy { notPassedParameterInd.map { parameterNames[it] } }

  val isValid by lazy { -1 !in argumentPermutationIndWithPipeExpression }
  val allDotsArguments by lazy {
    val dotsInd = parameterNames.indexOf(DOTS)
    if (dotsInd == -1) return@lazy emptyList<RExpression>()
    expressionListWithPipeExpression.filterIndexed { ind, _ -> argumentPermutationIndWithPipeExpression[ind] == dotsInd }
  }

  fun getParameterNameForArgument(expression: RExpression): String? {
    val parent = expression.parent
    val realArgument = if (parent is RNamedArgument && parent.assignedValue == expression) parent else expression
    val ind = argumentPermutationIndWithPipeExpression.getOrNull(expressionListWithPipeExpression.indexOf(realArgument)) ?: return null
    val name = parameterNames.getOrNull(ind)
    return if (name == DOTS && expression != realArgument) null // if named argument passed to DOTS only RNamedExpression is correct result
    else name
  }

  fun getArgumentPassedToParameter(parameterInd: Int): RExpression? {
    val realInd = argumentPermutationIndWithPipeExpression.indexOf(parameterInd)
    if (realInd == -1) return null
    return expressionListWithPipeExpression[realInd].let { if (it is RNamedArgument) it.assignedValue else it }
  }

  fun getArgumentPassedToParameter(parameterName: String): RExpression? {
    val ind = parameterNames.indexOf(parameterName)
    if (ind == -1) return null
    return getArgumentPassedToParameter(ind)
  }

  private fun <T> dropPipeValue(list: List<T>): List<T> = if (pipeArgument != null) list.drop(1) else list

  companion object {

    @JvmStatic
    fun getParameterInfo(argumentHolder: RArgumentHolder, parameterNames: List<String>): RArgumentInfo {
      val info = argumentHolder.getUserData(ARGUMENT_INFO_KEY)?.upToDateOrNull?.get()
      val result: RArgumentInfo
      if (info == null || info.parameterNames != parameterNames) {
        result = RArgumentInfo(argumentHolder, parameterNames)
        val cachedValue = CachedValuesManager.getManager(argumentHolder.project).createCachedValue{
          CachedValueProvider.Result.create(result, argumentHolder)
        }
        argumentHolder.putUserData(ARGUMENT_INFO_KEY, cachedValue)
      }
      else {
        result = info
      }
      return result
    }

    private const val PIPE_OPERATOR = "%>%"
    private val ARGUMENT_INFO_KEY =
      Key.create<CachedValue<RArgumentInfo>>("org.jetbrains.r.hints.parameterInfo.RArgumentInfo")

    private fun findPipeArgument(argumentHolder: RArgumentHolder): RExpression? {
      val call = if (argumentHolder is RArgumentList) argumentHolder.parent else argumentHolder
      val callParent = call.parent as? ROperatorExpression ?: return null
      return if (callParent.operator?.name == PIPE_OPERATOR && callParent.rightExpr == call) callParent.leftExpr
      else null
    }
  }
}

@Suppress("MemberVisibilityCanBePrivate")
object RParameterInfoUtil {

  fun getArgumentInfo(call: RCallExpression): RArgumentInfo? {
    return getArgumentInfo(call, RPsiUtil.resolveCall(call).singleOrNull())
  }

  fun getArgumentInfo(call: RCallExpression, definition: RAssignmentStatement?): RArgumentInfo? {
    if (definition == null) return null
    return RArgumentInfo.getParameterInfo(call.argumentList, definition.parameterNameList)
  }

  /**
   * If you need information about several parameters, use [RArgumentInfo][getArgumentInfo]
   *
   * @return the first entry of the required parameter if exist.
   * In the correct syntax, there is only one entry for all parameters except dots.
   * If you want to find all arguments passed to dots, use [getAllDotsArguments]
   */
  fun getArgumentByName(call: RCallExpression, name: String): RExpression? {
    return getArgumentByName(call, name, RPsiUtil.resolveCall(call, false).singleOrNull { it.parameterNameList.contains(name) })
  }

  /**
   * @see [getArgumentByName]
   */
  fun getArgumentByName(call: RCallExpression, name: String, definition: RAssignmentStatement?): RExpression? {
    if (definition != null) {
      return getArgumentInfo(call, definition)?.getArgumentPassedToParameter(name)
    }
    for (namedArgument in call.argumentList.namedArgumentList) {
      if (namedArgument.name == name) {
        return namedArgument.assignedValue
      }
    }
    return null
  }

  fun getAllDotsArguments(call: RCallExpression): List<RExpression> {
    return getAllDotsArguments(call, RPsiUtil.resolveCall(call).singleOrNull())
  }

  fun getAllDotsArguments(call: RCallExpression, definition: RAssignmentStatement?): List<RExpression> {
    return getArgumentInfo(call, definition)?.allDotsArguments ?: emptyList()
  }
}

private const val DOTS = "..."