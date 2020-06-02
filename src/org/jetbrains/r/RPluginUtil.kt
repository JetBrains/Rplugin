/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.io.exists
import java.io.File
import java.nio.file.Paths

object RPluginUtil {
  const val PLUGIN_ID = "R4Intellij"

  fun getPlugin(): IdeaPluginDescriptor =
    PluginManager.getPlugin(PluginId.getId(PLUGIN_ID)) ?: PluginManager.getPlugin(PluginId.getId(PLUGIN_ID + "2"))!!

  fun findFileInRHelpers(relativePath: String): File =
    Paths.get(helpersPath, relativePath).toFile()

  val helpersPath =
    Paths.get(PathManager.getPluginsPath(), "rplugin").takeIf { it.exists() }?.toString() ?:
    Paths.get(PathManager.getPreInstalledPluginsPath(), "rplugin").takeIf { it.exists() }?.toString() ?:
    Paths.get(RPluginUtil::class.java.getProtectionDomain().getCodeSource().getLocation().toURI().path, "..").toString()
}
