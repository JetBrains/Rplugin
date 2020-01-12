/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.statistics

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import org.jetbrains.r.interpreter.RInterpreter
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
    logEvent(RStatisticsEvent.SETUP_INTERPRETER) {
      addData("version", interpreter.version.toCompactString())
      val isConda = RCondaUtil.findCondaByRInterpreter(File(interpreter.interpreterPath)) != null
      addData("is.conda", isConda)
    }
  }
}