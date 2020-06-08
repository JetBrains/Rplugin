/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.packageManager

import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.RInterpreterUtil
import java.util.*

private val SCRIPT_PATH = RPluginUtil.findFileInRHelpers("R/projectGenerator/getPackratOptions.R")

fun getAllPackratSettings(rScriptPath: String): List<PackratSettings<*>> {
  val allPackratSettings = mutableListOf<PackratSettings<*>>()
  val stdout = RInterpreterUtil.runHelper(rScriptPath, SCRIPT_PATH, null, emptyList()) {
    throw IllegalStateException(it.stderr)
  }

  val packratOptions = stdout.filter { it != '>' }.split("$").drop(1)
  for (option in packratOptions) {
    val optionLines = option.split("\n").filter { it.isNotBlank() }
    val optionName = optionLines[0]
    val values = mutableListOf<String>()
    optionLines.drop(1).map { it.split(Regex("\\s+")).drop(1) }.forEach { values.addAll(it) }
    if (values.size == 1) {
      val value = values[0]
      if (value in PackartLogicalConstants.values().map { it.name }) {
        allPackratSettings.add(PackratSettings(optionName, PackartLogicalConstants.valueOf(value)))
        continue
      }
      else if (value == "\"auto\"") {
        allPackratSettings.add(PackratSettings(optionName, PackratExpandedLogicalConstants.AUTO))
        continue
      }
    }

    val value = StringJoiner(", ")
    values.forEach { valuePart -> value.add(valuePart.dropWhile { it == '"' }.dropLastWhile { it == '"' }) }
    allPackratSettings.add(PackratSettings(optionName, value.toString()))
  }

  return allPackratSettings
}

enum class PackartLogicalConstants {
  TRUE,
  FALSE
}

enum class PackratExpandedLogicalConstants {
  TRUE,
  FALSE,
  AUTO;
}

class PackratSettings<T>(val name: String, val value: T)

