/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.hints.parameterInfo

import org.jetbrains.r.psi.RPsiUtil
import org.jetbrains.r.psi.api.*

object RParameterInfoUtil {

  fun getArgumentByName(call: RCallExpression, name: String): RPsiElement? {
    return getArgumentByName(call, name, RPsiUtil.resolveCall(call, false).firstOrNull())
  }

  fun getArgumentByName(call: RCallExpression, name: String, definition: RAssignmentStatement?): RPsiElement? {
    val argumentList = call.argumentList
    argumentList.namedArgumentList.firstOrNull { it.name == name }?.let { return it.assignedValue }
    val parameterNameList = definition?.parameterNameList ?: return null
    val parameterIndex = parameterNameList.indexOf(name)
    if (parameterIndex == -1) return null
    val argumentIndex = getArgumentsPermutation(parameterNameList, argumentList).first.indexOf(parameterIndex)
    return argumentList.expressionList.getOrNull(argumentIndex)
  }

  fun getArgumentsPermutation(parameterNames: List<String>, argumentList: RArgumentList): Pair<List<Int>, List<Int>> {
    var curArgIndex = 0
    val skipNames = mutableSetOf<String>()
    val argumentNames = argumentList.namedArgumentList.map { it.name }
    val resultPermutation = mutableListOf<Int>()
    for (arg in argumentList.expressionList) {
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

    val unusedArguments = mutableListOf<Int>()
    parameterNames.forEachIndexed { ind, name ->
      if (name != DOTS && name !in skipNames || name == DOTS && ind !in resultPermutation)  unusedArguments.add(ind)
    }
    return resultPermutation to unusedArguments
  }

  private const val DOTS = "..."
}