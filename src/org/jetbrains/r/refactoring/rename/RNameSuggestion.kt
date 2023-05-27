/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.rename

import org.jetbrains.r.refactoring.RRefactoringUtil

object RNameSuggestion {
  fun getVariableSuggestedNames(curName: String?, unavailableNames: MutableSet<String> = mutableSetOf()): Set<String> {
    if (curName == null) return emptySet()
    val name = transformName(curName, false)
    return setOf(getUniqueName(name, unavailableNames))
  }

  fun getFunctionSuggestedNames(curName: String?, unavailableNames: MutableSet<String> = mutableSetOf()): Set<String> {
    if (curName == null) return emptySet()
    val name = transformName(curName, true)
    val result = mutableSetOf(getUniqueName(name, unavailableNames, true))
    val probablyAnotherName = transformName(curName, false)
    if (probablyAnotherName != name) {
      result += getUniqueName(probablyAnotherName, unavailableNames, true)
    }
    return result
  }

  fun getTargetForLoopSuggestedNames(curName: String?, unavailableNames: MutableSet<String> = mutableSetOf()): Set<String> {
    val result = getVariableSuggestedNames(curName, unavailableNames)
    for (i in listOf("i", "j", "k")) {
      if (!unavailableNames.contains(i)) {
        unavailableNames.add(i)
        return result + i
      }
    }

    return result + getUniqueName("i", unavailableNames)
  }

  private fun transformName(curName: String, isDotAvailable: Boolean): String {
    val defaultName = StringBuilder()
    var wasLetterOrDigit = false
    var wasDot = false
    curName.forEach {
      when {
        wasDot -> defaultName.append(it)
        it.isUpperCase() -> {
          if (wasLetterOrDigit) defaultName.append("_")
          defaultName.append(it.toLowerCase())
        }
        it == '.' -> {
          if (!isDotAvailable || !wasLetterOrDigit) defaultName.append('_')
          else {
            defaultName.append('.')
            wasDot = true
          }
        }
        it in listOf(' ', '-') -> defaultName.append('_')
        else -> {
          if (it.isLetterOrDigit()) wasLetterOrDigit = true
          defaultName.append(it)
        }
      }
    }
    return removeRepetitive(defaultName.trim { it == '_' }.toString())
  }

  private fun removeRepetitive(name: String): String {
    return name.replace(Regex("_+"), "_").replace("_.", ".")
  }

  private fun getUniqueName(baseName: String, unavailableNames: MutableSet<String>, isFunctionName: Boolean = false): String {
    return RRefactoringUtil.getUniqueName(baseName, unavailableNames, isFunctionName)
  }
}