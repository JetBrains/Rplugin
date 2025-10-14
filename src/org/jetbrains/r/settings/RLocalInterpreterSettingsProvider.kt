/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.icons.AllIcons.Nodes.HomeFolder
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.interpreter.RInterpreterInfo
import com.intellij.r.psi.interpreter.RInterpreterLocation
import org.jetbrains.r.configuration.RAddInterpreterDialog
import org.jetbrains.r.interpreter.RLocalInterpreterLocation
import javax.swing.Icon

class RLocalInterpreterSettingsProvider : RInterpreterSettingsProvider {
  override fun getLocationFromState(state: RSettings.State): RInterpreterLocation? {
    val interpreterPath = state.interpreterPath
    if (!interpreterPath.isNullOrEmpty() && state.remoteHost.isNullOrEmpty()) {
      return RLocalInterpreterLocation(interpreterPath)
    }
    return null
  }

  override fun putLocationToState(state: RSettings.State, location: RInterpreterLocation): Boolean {
    if (location !is RLocalInterpreterLocation) return false
    state.interpreterPath = location.path
    state.remoteHost = null
    return true
  }

  override fun deserializeLocation(serializable: RSerializableInterpreter): RInterpreterLocation? {
    val interpreterPath = serializable.path
    if (interpreterPath.isNotEmpty() && serializable.remoteHost.isEmpty()) {
      return RLocalInterpreterLocation(interpreterPath)
    }
    return null
  }

  override fun serializeLocation(location: RInterpreterLocation, serializable: RSerializableInterpreter): Boolean {
    if (location !is RLocalInterpreterLocation) return false
    serializable.path = location.path
    serializable.remoteHost = ""
    return true
  }

  override fun getAddInterpreterActionName() = RBundle.message("project.settings.details.step.add")

  override fun getAddInterpreterWidgetActionName() = RBundle.message("interpreter.status.bar.add.local.action.name")

  override fun getAddInterpreterWidgetActionDescription(): String = RBundle.message("interpreter.status.bar.add.local.action.description")

  override fun getAddInterpreterWidgetActionIcon(): Icon = HomeFolder

  override fun showAddInterpreterDialog(existingInterpreters: List<RInterpreterInfo>, onAdded: (RInterpreterInfo) -> Unit) {
    return RAddInterpreterDialog.show(existingInterpreters, onAdded)
  }

}