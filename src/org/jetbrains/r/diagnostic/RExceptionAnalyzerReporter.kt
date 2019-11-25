/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.diagnostic

import com.intellij.diagnostic.ITNReporter
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.extensions.PluginId

class RExceptionAnalyzerReporter : ITNReporter() {
  private val pluginDescriptor =  PluginManager.getPlugin(PluginId.getId(PLUGIN_ID))!!

  override fun showErrorInRelease(event: IdeaLoggingEvent): Boolean = true

  override fun getPluginDescriptor() = pluginDescriptor
}

private const val PLUGIN_ID = "R4Intellij"