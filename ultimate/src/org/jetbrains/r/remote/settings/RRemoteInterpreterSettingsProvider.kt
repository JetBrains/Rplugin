/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote.settings

import com.intellij.icons.AllIcons.Nodes.Console
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.remote.AuthType
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.util.EnvironmentUtil
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.remote.RAddOrEditRemoteInterpreterDialog
import org.jetbrains.r.remote.RRemoteBundle
import org.jetbrains.r.remote.RRemoteInterpreterLocation
import org.jetbrains.r.remote.host.RRemoteHostManager
import org.jetbrains.r.settings.RInterpreterSettingsProvider
import org.jetbrains.r.settings.RSerializableInterpreter
import org.jetbrains.r.settings.RSettings
import java.net.URI
import javax.swing.Icon

class RRemoteInterpreterSettingsProvider : RInterpreterSettingsProvider {
  override fun getLocationFromState(state: RSettings.State): RInterpreterLocation? {
    val interpreterPath = state.interpreterPath?.takeIf { it.isNotEmpty() } ?: return null
    val remoteHost = state.remoteHost?.let { RRemoteHostManager.getInstance().getRemoteHostByConfigId(it) } ?: return null
    val basePath = state.remoteBasePath ?: return null
    return RRemoteInterpreterLocation(remoteHost, interpreterPath, basePath)
  }

  override fun putLocationToState(state: RSettings.State, location: RInterpreterLocation): Boolean {
    if (location !is RRemoteInterpreterLocation) return false
    state.interpreterPath = location.remotePath
    state.remoteHost = location.remoteHost.sshConfig.id
    state.remoteBasePath = location.basePath
    return true
  }

  override fun deserializeLocation(serializable: RSerializableInterpreter): RInterpreterLocation? {
    val interpreterPath = serializable.path.takeIf { it.isNotEmpty() } ?: return null
    val remoteHost = RRemoteHostManager.getInstance().getRemoteHostByConfigId(serializable.remoteHost) ?: return null
    val basePath = serializable.remoteBasePath.takeIf { it.isNotEmpty() } ?: return null
    return RRemoteInterpreterLocation(remoteHost, interpreterPath, basePath)
  }

  override fun serializeLocation(location: RInterpreterLocation, serializable: RSerializableInterpreter): Boolean {
    if (location !is RRemoteInterpreterLocation) return false
    serializable.path = location.remotePath
    serializable.remoteHost = location.remoteHost.sshConfig.id
    serializable.remoteBasePath = location.basePath
    return true
  }

  override fun getAddInterpreterActionName(): String {
    return RRemoteBundle.message("project.settings.details.step.add.remote")
  }

  override fun getAddInterpreterWidgetActionName(): String {
    return RRemoteBundle.message("interpreter.status.bar.add.remote.action.name")
  }

  override fun getAddInterpreterWidgetActionDescription(): String {
    return RRemoteBundle.message("interpreter.status.bar.add.remote.action.description")
  }

  override fun getAddInterpreterWidgetActionIcon(): Icon {
    return Console
  }

  override fun showAddInterpreterDialog(existingInterpreters: List<RInterpreterInfo>, onAdded: (RInterpreterInfo) -> Unit) {
    RAddOrEditRemoteInterpreterDialog.show(existingInterpreters, onDone = onAdded)
  }

  override fun isEditingSupported() = true

  override fun canEditInterpreter(info: RInterpreterInfo): Boolean {
    return info.interpreterLocation is RRemoteInterpreterLocation
  }

  override fun showEditInterpreterDialog(info: RInterpreterInfo, existingInterpreters: List<RInterpreterInfo>,
                                         onEdited: (RInterpreterInfo) -> Unit) {
    RAddOrEditRemoteInterpreterDialog.show(existingInterpreters.minus(info), info) {
      if (it.interpreterName != info.interpreterName || it.interpreterLocation != info.interpreterLocation) {
        onEdited(it)
      }
    }
  }

  override fun createSettingsConfigurable(project: Project): UnnamedConfigurable? {
    return RRemoteSettingsConfigurable(project)
  }

  override fun provideInterpreterForTests(): RInterpreterLocation? {
    val parameters = EnvironmentUtil.getValue("RPLUGIN_TEST_REMOTE_INTERPRETER")
      ?.takeIf { it.isNotBlank() }
      ?.split(',') ?: return null
    check(parameters.size == 3) { "RPLUGIN_TEST_REMOTE_INTERPRETER variable should be in format 'host,interpreterPath,workingDir'" }
    val (host, interpreterPath, workingDir) = parameters.map { it.trim() }
    val uri = URI(host)
    check(uri.scheme == "ssh") { "Protocol should be 'ssh'" }
    val sshConfig = SshConfigManager.getInstance(null).register(
      false,
      uri.host,
      (uri.port.takeIf { it >= 0 } ?: 22).toString(),
      "",
      uri.userInfo.substringBefore(':'),
      AuthType.PASSWORD,
      uri.userInfo.substringAfter(':', ""),
      "",
      false,
      false,
      null,
      null)
    val remoteHost = RRemoteHostManager.getInstance().getRemoteHostBySshConfig(sshConfig)
    return RRemoteInterpreterLocation(remoteHost, interpreterPath, workingDir)
  }
}