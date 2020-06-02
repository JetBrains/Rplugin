/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.diagnostic

import com.intellij.diagnostic.ITNReporter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import org.jetbrains.r.RPluginUtil
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant

class RExceptionAnalyzerReporter : ITNReporter() {
  private val pluginDescriptor = RPluginUtil.getPlugin()

  private val expired =
    Duration.between(Instant.now(), SimpleDateFormat("yyyy-MM-dd").parse(EXPIRING_DATE).toInstant()).toDays() <= 0

  override fun showErrorInRelease(event: IdeaLoggingEvent): Boolean = !expired

  override fun getPluginDescriptor() = pluginDescriptor
}

private const val EXPIRING_DATE = "2020-06-30"