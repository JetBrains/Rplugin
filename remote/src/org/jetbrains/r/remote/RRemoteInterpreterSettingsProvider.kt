/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote

import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.remote.host.RRemoteHostManager
import org.jetbrains.r.settings.RInterpreterSettingsProvider
import org.jetbrains.r.settings.RSerializableInterpreter
import org.jetbrains.r.settings.RSettings

class RRemoteInterpreterSettingsProvider : RInterpreterSettingsProvider {
  override fun getLocationFromState(state: RSettings.State): RInterpreterLocation? {
    val interpreterPath = state.interpreterPath?.takeIf { it.isNotEmpty() } ?: return null
    val remoteHost = state.remoteHost?.let { RRemoteHostManager.getInstance().getRemoteHostBySshConfigName(it) } ?: return null
    return RRemoteInterpreterLocation(remoteHost, interpreterPath)
  }

  override fun putLocationToState(state: RSettings.State, location: RInterpreterLocation): Boolean {
    if (location !is RRemoteInterpreterLocation) return false
    state.interpreterPath = location.remotePath
    state.remoteHost = location.remoteHost.sshConfig.name
    return true
  }

  override fun deserializeLocation(serializable: RSerializableInterpreter): RInterpreterLocation? {
    val interpreterPath = serializable.path.takeIf { it.isNotEmpty() } ?: return null
    val remoteHost = RRemoteHostManager.getInstance().getRemoteHostBySshConfigName(serializable.remoteHost) ?: return null
    return RRemoteInterpreterLocation(remoteHost, interpreterPath)
  }

  override fun serializeLocation(location: RInterpreterLocation, serializable: RSerializableInterpreter): Boolean {
    if (location !is RRemoteInterpreterLocation) return false
    serializable.path = location.remotePath
    serializable.remoteHost = location.remoteHost.sshConfig.name
    return true
  }

  override fun getAddInterpreterActionName(): String {
    return RRemoteBundle.message("project.settings.details.step.add.remote")
  }

  override fun showAddInterpreterDialog(existingInterpreters: List<RInterpreterInfo>, onAdded: (RInterpreterInfo) -> Unit) {
    RAddRemoteInterpreterDialog.show(existingInterpreters, onAdded)
  }
}