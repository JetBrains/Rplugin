/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote.host

import com.intellij.execution.CommandLineUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.remote.RemoteCredentials
import com.intellij.ssh.ExecBuilder
import com.intellij.ssh.SessionConfig
import com.intellij.ssh.SftpChannelConfig
import com.intellij.ssh.SshConnectionService
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.process.SshExecProcess
import com.intellij.util.PathUtil
import com.intellij.util.io.DigestUtil
import com.jetbrains.plugins.remotesdk.CredentialsDeployable
import com.jetbrains.plugins.remotesdk.transport.remoteCredentialsToSessionConfig
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.isRejected
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.execution.ExecuteExpressionUtils
import org.jetbrains.r.interpreter.OperatingSystem
import org.jetbrains.r.remote.RRemoteUtil
import org.jetbrains.r.remote.filesystem.RRemoteVFS
import org.jetbrains.r.util.RPathUtil
import java.io.File
import java.util.concurrent.TimeUnit

class RRemoteHost internal constructor(private val sshConfig: SshConfig) {
  lateinit var configId: String
    private set
  lateinit var credentials: RemoteCredentials
    private set
  lateinit var sessionConfig: SessionConfig
    private set
  lateinit var config: CredentialsDeployable
    private set

  private val uploadedHelpers = mutableMapOf<String, AsyncPromise<String>>()

  private val operatingSystemValue = object : AtomicClearableLazyValue<OperatingSystem>() {
    override fun compute(): OperatingSystem {
      val unameResult = ExecBuilder(sessionConfig, "uname -s").execute()
        .inputStream.bufferedReader().readText().trim().toLowerCase()
      return when (unameResult) {
        "linux" -> OperatingSystem.LINUX
        "darwin" -> OperatingSystem.MAC_OS
        else -> OperatingSystem.WINDOWS
      }
    }
  }
  val operatingSystem get() = operatingSystemValue.value

  val presentableName: String
    get() {
      val s = StringBuilder()
      if (sshConfig.username.isNotEmpty()) {
        s.append(sshConfig.username).append('@')
      }
      s.append(sshConfig.host)
      if (sshConfig.port != 22) {
        s.append(':').append(sshConfig.port)
      }
      return s.toString()
    }

  private val remoteBasePathValue = object : AtomicClearableLazyValue<String>() {
    override fun compute(): String {
      return useSftpChannel { channel ->
        val home = channel.home.let { if (operatingSystem == OperatingSystem.WINDOWS && it.startsWith('/')) it.substring(1) else it }
        RPathUtil.join(home, "jetbrains_rplugin")
      }
    }
  }
  val remoteBasePath get() = remoteBasePathValue.value

  val remoteHelpersRoot: String
    get() = RPathUtil.join(remoteBasePath, "helpers")

  private val remoteTempDirValue = object : AtomicClearableLazyValue<String>() {
    override fun compute(): String {
      return if (operatingSystem == OperatingSystem.WINDOWS) {
        ExecBuilder(sessionConfig, "echo %Temp%").execute()
          .inputStream.bufferedReader().readText().trim()
          .replace('\\', '/')
      } else {
        "/tmp"
      }
    }
  }
  private val remoteTempDir get() = remoteTempDirValue.value

  init {
    refresh()
  }

  internal fun refresh() {
    configId = sshConfig.id
    credentials = sshConfig.copyToCredentials()
    sessionConfig = remoteCredentialsToSessionConfig(credentials, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS,
                                                     null, null)
    synchronized(uploadedHelpers) { uploadedHelpers.clear() }
    config = object : CredentialsDeployable(credentials) {
      override fun getRootNodePresentableDescription(showRootFolder: Boolean) = presentableName

      override fun refreshCredentials() {
        super.refreshCredentials()
        sshUiData?.let {
          it.loadFromCredentials(this@RRemoteHost.credentials)
          server.fileTransferConfig.setSshConfig(it.config)
        }
      }
    }.also { it.name = presentableName; it.id = configId }
    operatingSystemValue.drop()
    remoteBasePathValue.drop()
    remoteTempDirValue.drop()
  }

  inline fun <T> useSftpChannel(f: (SftpChannel) -> T): T {
    val channel = SshConnectionService.instance.sftp(sessionConfig, SftpChannelConfig())
    channel.use { return f(it) }
  }

  fun uploadRHelper(helper: File): String {
    val helpersRoot = File(RPluginUtil.helpersPath)
    if (!FileUtil.isAncestor(helpersRoot, helper, false)) {
      throw IllegalArgumentException("Helper should be located in helpers directory")
    }
    if (!helper.exists()) {
      throw RuntimeException("Helper $helper does not exist")
    }
    if (!helper.isFile) {
      throw IllegalArgumentException("Helper must be a regular file")
    }
    val relativePath = FileUtil.getRelativePath(helpersRoot, helper)!!
    val wait: Boolean
    val promise = synchronized(uploadedHelpers) {
      val result = uploadedHelpers[relativePath]
      if (result != null && !result.isRejected) {
        wait = true
        result
      } else {
        wait = false
        AsyncPromise<String>().also { uploadedHelpers[relativePath] = it }
      }
    }
    if (wait) return promise.blockingGet(Int.MAX_VALUE)!!
    try {
      return uploadRHelperImpl(helper, relativePath).also { promise.setResult(it) }
    } catch (t: Throwable) {
      promise.setError(t)
      throw t
    }
  }

  fun uploadRHelpersRecursively(file: File) {
    if (!file.exists()) return
    if (file.isFile) {
      uploadRHelper(file)
    } else if (file.isDirectory) {
      file.listFiles()?.forEach { uploadRHelpersRecursively(it) }
    }
  }

  private fun uploadRHelperImpl(localFile: File, relativePath: String): String {
    val messageDigest = DigestUtil.sha1().also { DigestUtil.updateContentHash(it, localFile.toPath()) }
    val localHash = StringUtil.toHexString(messageDigest.digest())
    return useSftpChannel { channel ->
      val remotePath = RPathUtil.join(remoteHelpersRoot, relativePath)
      channel.mkDirs(PathUtil.getParentPath(remotePath))
      val remoteHashPath = remotePath + REMOTE_HELPERS_HASH_SUFFIX
      val remoteHash = channel.file(remoteHashPath).takeIf { it.exists() }
        ?.inputStream()?.bufferedReader()?.use { it.readText().trim() }
      if (remoteHash != localHash) {
        LOG.debug { "Uploading R helper $relativePath to $presentableName : $remotePath" }
        ExecuteExpressionUtils.getSynchronously("Uploading $relativePath to $presentableName") {
          channel.file(remoteHashPath).rm()
          channel.uploadFileOrDir(localFile, PathUtil.getParentPath(remotePath), PathUtil.getFileName(remotePath))
          channel.file(remoteHashPath).outputStream(false).bufferedWriter().use {
            it.write(localHash)
          }
        }
      } else {
        LOG.debug { "R helper $relativePath is up-to-date on $presentableName : $remotePath" }
      }
      remotePath
    }
  }

  fun uploadTmpFile(name: String, content: ByteArray): String {
    return ExecuteExpressionUtils.getSynchronously("Uploading file $name to $presentableName") {
      useSftpChannel { channel ->
        val nameNoExt = FileUtil.getNameWithoutExtension(name)
        val extension = FileUtilRt.getExtension(name).takeIf { it.isNotEmpty() }?.let { ".$it" } ?: ""
        var iter = 0
        val remoteDir = remoteTempDir
        var remotePath: String
        while (true) {
          val remoteName = if (iter == 0) "$nameNoExt$extension" else "$nameNoExt-$iter$extension"
          remotePath = RPathUtil.join(remoteDir, remoteName)
          val remoteFile = channel.file(remotePath)
          if (remoteFile.exists()) {
            ++iter
          } else {
            LOG.debug { "Uploading file $name to $presentableName : $remotePath" }
            remoteFile.outputStream(false).use {
              it.write(content)
            }
            break
          }
        }
        remotePath
      }
    }
  }

  private fun uploadTmpFile(file: File): String {
    if (FileUtil.isAncestor(RPluginUtil.helpersPath, file.path, false)) {
      return uploadRHelper(file)
    }
    return uploadTmpFile(file.name, file.readBytes())
  }

  fun uploadFileIfNeeded(file: VirtualFile): String {
    RRemoteVFS.getHostAndPath(file)?.let { (host, path) ->
      if (host == this) {
        return path
      }
    }
    if (file.isInLocalFileSystem) {
      return uploadTmpFile(File(file.path))
    }
    return uploadTmpFile(file.name, file.contentsToByteArray())
  }

  fun createProcess(command: GeneralCommandLine, timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS, workingDir: String? = null): SshExecProcess {
    workingDir?.let { mkDirs(it) }
    val commandLine = buildCommand(command, workingDir)
    return ExecBuilder(sessionConfig, commandLine).execute(timeoutMillis.toInt())
  }

  fun runCommand(command: GeneralCommandLine, timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS): ProcessOutput {
    val process = createProcess(command, timeoutMillis)
    val result = ProcessOutput()
    if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
      process.destroy()
      result.exitCode = -1
      result.setTimeout()
    } else {
      result.exitCode = process.exitValue()
    }
    result.appendStdout(process.inputStream.bufferedReader().readText())
    result.appendStderr(process.errorStream.bufferedReader().readText())
    return result
  }

  private fun mkDirs(path: String) = useSftpChannel { it.mkDirs(path) }

  private fun buildCommand(commandLine: GeneralCommandLine, workingDir: String? = null): String {
    if (operatingSystem == OperatingSystem.WINDOWS) {
      val command = StringBuilder()
      if (workingDir != null) {
        command.append("cd ${CommandLineUtil.escapeParameterOnWindows(workingDir, true)}&&")
      }
      commandLine.environment.forEach { (key, value) ->
        command.append("set $key=$value&&")
      }
      command.append(CommandLineUtil.escapeParameterOnWindows(commandLine.exePath, true))
      commandLine.parametersList.parameters.forEach {
        command.append(' ').append(CommandLineUtil.escapeParameterOnWindows(it, true))
      }
      return command.toString()
    }

    val command = StringBuilder()
    if (workingDir != null) {
      command.append("cd ${CommandLineUtil.posixQuote(workingDir)}\n")
    }
    command.append("chmod +x ${CommandLineUtil.posixQuote(commandLine.exePath)} >/dev/null 2>/dev/null\n")
    commandLine.environment.forEach { (key, value) ->
      command.append("export ${CommandLineUtil.posixQuote(key)}=${CommandLineUtil.posixQuote(value)}\n")
    }
    command.append(CommandLineUtil.posixQuote(commandLine.exePath))
    commandLine.parametersList.parameters.forEach {
      command.append(' ').append(CommandLineUtil.posixQuote(it))
    }
    return command.toString()
  }

  companion object {
    private const val DEFAULT_TIMEOUT_MILLIS = RRemoteUtil.DEFAULT_TIMEOUT_MILLIS
    private const val REMOTE_HELPERS_HASH_SUFFIX = ".file_hash"
    private val LOG = Logger.getInstance(RRemoteHost::class.java)
  }
}

fun SftpChannel.mkDirs(path: String) {
  if (path == "") return
  mkDirs(PathUtil.getParentPath(path))
  val file = file(path)
  if (!file.exists()) {
    file.mkdir()
  }
}
