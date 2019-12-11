import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

object RPluginUtil {
  val pythonPluginId = PluginId.getId("PythonCore")

  fun isPluginEnabled(id: PluginId): Boolean {
    val ideaPluginDescriptor = PluginManagerCore.getPlugin(id) ?: return false
    return PluginManagerCore.isPluginInstalled(id) && ideaPluginDescriptor.isEnabled
  }

  val isPythonEnabled: Boolean
    get() = isPluginEnabled(pythonPluginId)
}