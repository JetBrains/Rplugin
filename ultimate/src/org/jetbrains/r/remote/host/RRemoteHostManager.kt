/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote.host

import com.intellij.application.subscribe
import com.intellij.openapi.components.service
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.config.unified.SshConfigManager
import java.util.concurrent.ConcurrentHashMap

class RRemoteHostManager {
  private val remoteHosts = ConcurrentHashMap<String, RRemoteHost>()

  init {
    SshConfigManager.SSH_CONFIGS.subscribe(null, object : SshConfigManager.Listener {
      override fun sshConfigsChanged() {
        remoteHosts.values.forEach { it.refresh() }
      }
    })
    SshConfigManager.SSH_CONFIG_AUTH.subscribe(null, SshConfigManager.SshConfigAuthListener {
      remoteHosts[it.id]?.refresh()
    })
  }

  fun getRemoteHostBySshConfig(config: SshConfig): RRemoteHost {
    return remoteHosts[config.id] ?: RRemoteHost(config).also {
      remoteHosts[config.id] = it
    }
  }

  fun getRemoteHostByConfigId(id: String): RRemoteHost? {
    return remoteHosts[id] ?: SshConfigManager.getInstance(null).findConfigById(id)?.let { getRemoteHostBySshConfig(it) }
  }

  companion object {
    fun getInstance(): RRemoteHostManager = service()
  }
}