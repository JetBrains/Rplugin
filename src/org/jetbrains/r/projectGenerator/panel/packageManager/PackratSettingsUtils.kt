/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package icons.org.jetbrains.r.projectGenerator.panel.packageManager

import icons.org.jetbrains.r.RBundle
import org.jetbrains.r.execution.ExecuteExpressionUtils
import java.util.*
import javax.xml.bind.ValidationException

private const val SCRIPT_PATH = "projectGenerator/getPackratOptions.R"

fun getAllPackratSettings(rScriptPath: String): Array<PackratSettings<*>> {
  if (LastRequest.scriptPath == rScriptPath) return LastRequest.result
  LastRequest.scriptPath = rScriptPath
  val allPackratSettings = mutableListOf<PackratSettings<*>>()
  val packratOptionsResult = ExecuteExpressionUtils
    .executeScriptInBackground(rScriptPath, SCRIPT_PATH, emptyList(), RBundle.message("project.settings.get.all.packrat.settings.title"))
  if (packratOptionsResult.stderr.isNotEmpty()) {
    throw ValidationException(packratOptionsResult.stderr)
  }

  val packratOptions = packratOptionsResult.stdout.filter { it != '>' }.split("$").drop(1)
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

  LastRequest.result = allPackratSettings.toTypedArray()
  return LastRequest.result
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

private object LastRequest {
  var scriptPath: String? = null
  var result: Array<PackratSettings<*>> = emptyArray()
}

