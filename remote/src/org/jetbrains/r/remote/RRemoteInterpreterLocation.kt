/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import org.jetbrains.r.interpreter.RInterpreterBase
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.remote.host.RRemoteHost
import org.jetbrains.r.remote.host.RRemoteHostManager
import java.io.File

data class RRemoteInterpreterLocation(val sshConfigName: String, val remotePath: String): RInterpreterLocation {
  private val remoteHost: RRemoteHost?
    get() = RRemoteHostManager.getInstance().getRemoteHostBySshConfigName(sshConfigName)
  private val remoteHostNotNull: RRemoteHost
    get() = remoteHost ?: throw RuntimeException("No such SSH config \"$sshConfigName\"")

  override fun toString(): String = "$sshConfigName : $remotePath"

  override fun getVersion(): Version? {
    return remoteHost?.let { RRemoteUtil.getInterpreterVersion(it, remotePath) }
  }

  override fun runHelper(helper: File, workingDirectory: String?, args: List<String>, errorHandler: ((ProcessOutput) -> Unit)?): String {
    return RRemoteUtil.runHelper(remoteHostNotNull, remotePath, helper, args, errorHandler)
  }

  override fun runHelperScript(helper: File, args: List<String>, timeout: Int): ProcessOutput {
    return RRemoteUtil.runHelperScript(remoteHostNotNull, remotePath, helper, args, timeout)
  }

  override fun createInterpreter(project: Project): RInterpreterBase {
    val host = remoteHostNotNull
    val versionInfo = RRemoteUtil.loadInterpreterVersionInfo(host, remotePath)
    return RRemoteInterpreterImpl(this, host, versionInfo, project)
  }
}

