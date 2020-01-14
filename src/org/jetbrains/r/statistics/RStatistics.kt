/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.statistics

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.rinterop.RCondaUtil
import java.io.File

enum class RStatisticsEvent(val id: String) {
  SETUP_INTERPRETER("setup.interpreter")
}

object RStatistics {
  const val GROUP_ID = "r.interpreters"

  fun logEvent(eventId: RStatisticsEvent, dataInitializer: FeatureUsageData.() -> Unit = { }) {
    val data = FeatureUsageData()
    dataInitializer(data)
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, eventId.id, data)
  }

  fun logSetupInterpreter(interpreter: RInterpreter) {
    val suggestedInterpreters = collectFoundInterpreters(interpreter)
    val isConda = isConda(interpreter.interpreterPath)
    logEvent(RStatisticsEvent.SETUP_INTERPRETER) {
      addData("version", interpreter.version.toCompactString())
      addData("is.conda", isConda)
      addData("suggested", suggestedInterpreters)
    }
  }

  private fun isConda(path: String) =
    RCondaUtil.findCondaByRInterpreter(File(path)) != null

  private fun collectFoundInterpreters(selected: RInterpreter): List<String> {
    val allInterpreters = RInterpreterUtil.suggestAllInterpreters(false)
    val selectedInfo: RInterpreterInfo? = allInterpreters.find { it.interpreterPath == selected.interpreterPath }
    return if (selectedInfo == null) {
      // WTF actually???
      allInterpreters
    }
    else {
      val others = allInterpreters - selectedInfo
      (listOf(selectedInfo) + others)
    }.map { infoToString(it) }
  }

  private fun infoToString(info: RInterpreterInfo): String =
    """${info.version.toCompactString()}_${isConda(info.interpreterPath)}"""
}