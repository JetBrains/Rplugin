/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.remote.BaseRemoteProcessHandler
import com.intellij.ssh.SftpChannelException
import com.intellij.ssh.process.SshExecProcess
import com.intellij.util.PathUtil
import com.jetbrains.plugins.remotesdk.ui.RemoteBrowseActionListener
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterBase
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.remote.filesystem.RRemoteHostViewManager
import org.jetbrains.r.remote.filesystem.RRemoteVFS
import org.jetbrains.r.remote.host.RRemoteHost
import org.jetbrains.r.remote.host.mkDirs
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.util.RPathUtil
import java.io.File
import java.io.IOException
import java.nio.file.Paths

class RRemoteInterpreterImpl(
  override val interpreterLocation: RRemoteInterpreterLocation,
  private val remoteHost: RRemoteHost,
  versionInfo: Map<String, String>,
  project: Project) : RInterpreterBase(versionInfo, project) {
  private val remotePath get() = interpreterLocation.remotePath
  override val basePath = remoteHost.remoteBasePath
  override val hostOS get() = remoteHost.operatingSystem

  override val interpreterName: String
    get() {
      val remoteHostName = PathUtil.suggestFileName(remoteHost.presentableName, true, false)
      return "Remote_${remoteHostName}_${super.interpreterName}"
    }

  init {
    removeLocalRDataTmpFiles()
  }

  override fun getProcessOutput(scriptText: String): ProcessOutput? {
    val commandLine = arrayOf<String>(remotePath, "--no-restore", "--quiet", "--slave", "-e", scriptText)
    try {
      return remoteHost.runCommand(GeneralCommandLine(*commandLine))
    } catch (e: Throwable) {
      LOG.info("""
        Failed to run R executable on remote host ${remoteHost.presentableName}
        Remote interpreter path: $remotePath
        Exception occurred: ${e.message}
      """.trimIndent())
    }
    return null
  }

  override fun runHelper(helper: File, workingDirectory: String?, args: List<String>, errorHandler: ((ProcessOutput) -> Unit)?): String {
    return RRemoteUtil.runHelper(remoteHost, remotePath, helper, args, errorHandler)
  }

  override fun runHelperProcess(script: String, args: List<String>, workingDirectory: String?): ProcessHandler {
    val commands = RInterpreterUtil.getRunHelperCommands(remotePath, script, args)
    val commandLine = RInterpreterUtil.createCommandLine(remotePath, commands, workingDirectory)
    return BaseRemoteProcessHandler<SshExecProcess>(remoteHost.createProcess(commandLine, workingDir = workingDirectory),
                                                    commandLine.commandLineString, null)
  }

  override fun suggestConsoleName(workingDirectory: String): String = "[ ${remoteHost.presentableName}:$workingDirectory ]"

  override fun onSetAsProjectInterpreter() {
    super.onSetAsProjectInterpreter()
    RRemoteHostViewManager.getInstance(project).addRemoteHost(remoteHost)
  }

  override fun onUnsetAsProjectInterpreter() {
    super.onUnsetAsProjectInterpreter()
    RRemoteHostViewManager.getInstance(project).removeRemoteHost(remoteHost)
  }

  override fun getFilePathAtHost(file: VirtualFile): String? {
    return RRemoteVFS.getHostAndPath(file)?.let { (host, path) ->
      path.takeIf { host == remoteHost }
    }
  }

  override fun findFileByPathAtHost(path: String): VirtualFile? {
    return RRemoteVFS.instance.findFileByPath(remoteHost, path)
  }

  override fun uploadHelperToHost(helper: File): String {
    return remoteHost.uploadRHelper(helper)
  }

  override fun runProcessOnHost(command: GeneralCommandLine): ProcessHandler {
    val process = remoteHost.createProcess(command, 0, remoteHost.remoteBasePath)
    return BaseRemoteProcessHandler<SshExecProcess>(process, command.exePath, null)
  }

  override fun createRInteropForProcess(process: ProcessHandler, port: Int): RInterop {
    val session = ((process as? BaseRemoteProcessHandler<*>)?.process as? SshExecProcess)?.session
                  ?: throw RuntimeException("Process is not remote")
    val localPort = session.addLocalTunnelWithRandomLocalPort("127.0.0.1", port)
    val rInterop = RInterop(this, process, "127.0.0.1", localPort, project)
    val workspaceFile = interpreterLocation.remotePath.hashCode().toString().let {
      RPathUtil.join(basePath, ".RDataFiles", "$it.RData")
    }
    val rScriptsDir = RPluginUtil.findFileInRHelpers("R").takeIf { it.exists() }
                       ?: throw RuntimeException("R Scripts not found")
    remoteHost.uploadRHelpersRecursively(rScriptsDir)
    val remoteScriptsPath = RPathUtil.join(remoteHost.remoteHelpersRoot, "R")
    val baseDir = remoteHost.remoteBasePath
    rInterop.init(remoteScriptsPath, baseDir, workspaceFile)
    return rInterop
  }

  override fun uploadFileToHostIfNeeded(file: VirtualFile): String {
    return remoteHost.uploadFileIfNeeded(file)
  }

  override fun createFileChooserForHost(value: String, selectFolder: Boolean): TextFieldWithBrowseButton {
    return TextFieldWithBrowseButton().also { component ->
      component.text = value
      component.addActionListener(RemoteBrowseActionListener(component.textField,
                                                             RRemoteBundle.message("remote.file.chooser.title")) { consumer ->
        consumer.consume(remoteHost.credentials)
      }.withFoldersOnly(selectFolder))
    }
  }

  override fun createTempFileOnHost(name: String, content: ByteArray?): String {
    return remoteHost.uploadTmpFile(name, content ?: ByteArray(0))
  }

  override fun getGuaranteedWritableLibraryPath(libraryPaths: List<RInterpreter.LibraryPath>, userPath: String): Pair<String, Boolean> {
    val writable = libraryPaths.find { it.isWritable }
    return if (writable != null) {
      Pair(writable.path, false)
    } else {
      remoteHost.useSftpChannel { channel ->
        val existed = channel.file(userPath).exists()
        channel.mkDirs(userPath)
        Pair(userPath, !existed)
      }
    }
  }

  private fun removeLocalRDataTmpFiles() {
    try {
      remoteHost.useSftpChannel { channel ->
        val dir = channel.file(RPathUtil.join(basePath, ".RDataFiles"))
        if (dir.isDir()) {
          dir.list().filter { ".RDataTmp" in it.name() }.forEach { it.rm() }
        }
      }
    } catch (e: SftpChannelException) {
      LOG.error(e)
    }
  }

  companion object {
    val LOG = Logger.getInstance(RRemoteInterpreterImpl::class.java)
  }
}