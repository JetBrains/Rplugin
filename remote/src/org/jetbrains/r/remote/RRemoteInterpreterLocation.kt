/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.remote.BaseRemoteProcessHandler
import com.intellij.ssh.process.SshExecProcess
import org.jetbrains.r.interpreter.RInterpreterBase
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.remote.host.RRemoteHost
import java.io.File

data class RRemoteInterpreterLocation(val remoteHost: RRemoteHost, val remotePath: String): RInterpreterLocation {
  override fun toString(): String = "${remoteHost.sshConfig.name} : $remotePath"

  override fun runInterpreterOnHost(args: List<String>, workingDirectory: String?): BaseProcessHandler<*> {
    return runProcessOnHost(GeneralCommandLine().withExePath(remotePath).withParameters(args), workingDirectory)
  }

  override fun runProcessOnHost(command: GeneralCommandLine, workingDirectory: String?): BaseProcessHandler<*> {
    val process = remoteHost.createProcess(command, 0, workingDirectory)
    return BaseRemoteProcessHandler<SshExecProcess>(process, command.exePath, null)
  }

  override fun uploadFileToHost(file: File, preserveName: Boolean): String {
    return remoteHost.uploadFile(file, preserveName)
  }

  override fun createInterpreter(project: Project): RInterpreterBase {
    val versionInfo = RInterpreterUtil.loadInterpreterVersionInfo(this)
    return RRemoteInterpreterImpl(this, remoteHost, versionInfo, project)
  }
}

