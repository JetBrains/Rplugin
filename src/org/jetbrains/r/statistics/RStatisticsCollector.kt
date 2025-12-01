/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project
import com.intellij.r.psi.interpreter.*
import com.intellij.r.psi.rinterop.RCondaUtil
import java.io.File

object RWorkflowCollector : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  private val GROUP = EventLogGroup("r.workflow", 3)
  private val reportCallsFromConsole = setOf("install.packages", "install_github")
  private val CALL_METHOD_FROM_CONSOLE = GROUP.registerEvent("call.method.from.console",
                                                             EventFields.String("name", reportCallsFromConsole.toList()))

  fun logConsoleMethodCall(project: Project?, name: String) {
    if (reportCallsFromConsole.contains(name)) {
      CALL_METHOD_FROM_CONSOLE.log(project, name)
    }
  }
}

object RInterpretersCollector : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  private val GROUP = EventLogGroup("r.interpreters", 3)
  private val IS_CONDA = EventFields.Boolean("is_conda")
  private val SUGGESTED = EventFields.StringListValidatedByInlineRegexp("suggested", "(\\d+\\.?)*\\d+_(true|false)")
  private val SETUP_INTERPRETER = GROUP.registerVarargEvent("setup.interpreter",
                                                            EventFields.Version,
                                                            IS_CONDA,
                                                            SUGGESTED)

  fun logSetupInterpreter(project: Project, interpreter: RInterpreter, suggestedInterpreters: List<String>) {
    val isConda = isConda(interpreter.interpreterLocation)
    val data = ArrayList<EventPair<*>>()
    data.add(EventFields.Version.with(interpreter.version.toCompactString()))
    data.add(IS_CONDA.with(isConda))
    data.add(SUGGESTED.with(suggestedInterpreters))
    SETUP_INTERPRETER.log(project, data)
  }

  private fun isConda(interpreterLocation: RInterpreterLocation): Boolean {
    return RCondaUtil.findCondaByRInterpreter(File(interpreterLocation.toLocalPathOrNull() ?: return false)) != null
  }

  suspend fun collectFoundInterpreters(selected: RInterpreter): List<String> {
    val allInterpreters = RInterpreterUtil.suggestAllInterpreters(false)
    val selectedInfo: RInterpreterInfo? = allInterpreters.find { it.interpreterLocation == selected.interpreterLocation }
    return if (selectedInfo == null) {
      // WTF actually??? This means we report information about interpreter and it is missing in all available interpreter list
      allInterpreters
    }
    else {
      // Place current interpreter to the beginning
      val others = allInterpreters - selectedInfo
      (listOf(selectedInfo) + others)
    }.map { infoToString(it) }
  }

  private fun infoToString(info: RInterpreterInfo): String =
    """${info.version.toCompactString()}_${isConda(info.interpreterLocation)}"""
}