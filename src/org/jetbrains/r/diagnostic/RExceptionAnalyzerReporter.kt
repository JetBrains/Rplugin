/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.diagnostic

import com.intellij.diagnostic.ITNReporter
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.r.packages.RHelpersUtil
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant

class RExceptionAnalyzerReporter : ITNReporter() {
  private val pluginDescriptor =  PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))

  private val expired = RHelpersUtil.findFileInRHelpers("timestamp").takeIf { it.exists() }?.let {
    Duration.between(SimpleDateFormat("yyyy-MM-dd").parse(it.readText()).toInstant(), Instant.now()).toDays() > 30
  } ?: true

  override fun showErrorInRelease(event: IdeaLoggingEvent): Boolean = !expired

  override fun getPluginDescriptor() = pluginDescriptor
}

private const val PLUGIN_ID = "R4Intellij"