/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.remote.BaseRemoteProcessHandler
import com.intellij.ssh.SftpChannelException
import com.intellij.ssh.SshTransportException
import com.intellij.ssh.process.SshExecProcess
import org.jetbrains.annotations.Nls
import org.jetbrains.r.interpreter.RInterpreterBase
import org.jetbrains.r.interpreter.RInterpreterLocation
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.remote.host.RRemoteHost
import org.jetbrains.r.remote.host.mkDirs
import java.io.File
import java.io.IOException

data class RRemoteInterpreterLocation(val remoteHost: RRemoteHost, val remotePath: String, val basePath: String): RInterpreterLocation {
  override fun toString(): String = "${remoteHost.presentableName} : $remotePath ($basePath)"

  @Nls
  override fun additionalShortRepresentationSuffix(): String {
    return RRemoteBundle.message("interpreter.remote.short.representation.suffix")
  }

  override fun getWidgetSwitchInterpreterActionHeader(): String {
    return RRemoteBundle.message("interpreter.status.bar.remote.interpreters.header")
  }

  override fun runInterpreterOnHost(args: List<String>, workingDirectory: String?, environment: Map<String, String>?): BaseProcessHandler<*> {
    return runProcessOnHost(GeneralCommandLine().withExePath(remotePath).withParameters(args).withEnvironment(environment), workingDirectory)
  }

  override fun runProcessOnHost(command: GeneralCommandLine, workingDirectory: String?, isSilent: Boolean): BaseProcessHandler<*> {
    val process = remoteHost.createProcess(command, 0, workingDirectory ?: basePath)
    return BaseRemoteProcessHandler<SshExecProcess>(process, command.exePath, null)
  }

  override fun uploadFileToHost(file: File, preserveName: Boolean): String {
    return remoteHost.uploadFileIfNeeded(file, preserveName)
  }

  override fun createInterpreter(project: Project): RInterpreterBase {
    try {
      remoteHost.ensureHasCredentials()
      RRemoteInterpreterImpl.checkBasePath(basePath, remoteHost)?.let {
        invokeLater { RInterpreterUtil.showInvalidLocationErrorMessage(project, this, it) }
        throw RuntimeException(it)
      }
      if (!RInterpreterUtil.checkInterpreterLocation(project, this)) {
        throw RuntimeException("Invalid R Interpreter")
      }
      remoteHost.useSftpChannel { it.mkDirs(basePath) }
      val versionInfo = RInterpreterUtil.loadInterpreterVersionInfo(this)
      return RRemoteInterpreterImpl(this, versionInfo, project)
    } catch (e: Throwable) {
      if (e is SshTransportException || e is SftpChannelException || e is IOException) {
        invokeLater { RInterpreterUtil.showInvalidLocationErrorMessage(project, this, e.cause?.message ?: e.message) }
      }
      throw e
    }
  }

  override fun canRead(path: String): Boolean = remoteHost.checkPermissions(path, "r")

  override fun canWrite(path: String): Boolean = remoteHost.checkPermissions(path, "w")

  override fun canExecute(path: String): Boolean = remoteHost.checkPermissions(path, "x")
}

