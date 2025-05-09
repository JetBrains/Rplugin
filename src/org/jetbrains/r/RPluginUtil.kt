/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.cl.PluginAwareClassLoader
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.extensions.PluginDescriptor
import org.jetbrains.r.RPluginUtil.findPathInRHelpers
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

object RPluginUtil {
  const val PLUGIN_ID = "R4Intellij"
  private const val PLUGIN_NAME = "r-plugin"

  fun getPlugin(): PluginDescriptor =
    (RPluginUtil::class.java.classLoader as? PluginAwareClassLoader)?.pluginDescriptor
    ?: PluginManagerCore.getPlugin(PluginManagerCore.CORE_ID)!!  // Happens when running an IDE from sources

  /** better to use [findPathInRHelpers] instead */
  fun findFileInRHelpers(relativePath: String): File =
    findPathInRHelpers(relativePath).toFile()

  fun findPathInRHelpers(relativePath: String): Path {
    return Path.of(helpersPath, relativePath)
  }

  fun findTextInRHelpersOrNull(relativePath: String): String? {
    val path = findPathInRHelpers(relativePath)
    if (!path.exists()) return null
    return Files.readString(path)
  }

  val helpersPath: String
    get() = helperPathOrNull ?: throw IllegalStateException("Cannot find rplugin directory")

  val helperPathOrNull: String?
    get() = Path.of(PathManager.getPluginsPath(), PLUGIN_NAME).takeIf { it.exists() }?.toString() ?:
            Path.of(PathManager.getPreInstalledPluginsPath(), PLUGIN_NAME).takeIf { it.exists() }?.toString()
}
