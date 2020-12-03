/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote.host

import com.intellij.execution.CommandLineUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.remote.RemoteCredentials
import com.intellij.ssh.*
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.ssh.interaction.ConnectionOwnerFactory
import com.intellij.ssh.process.SshExecProcess
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.util.PathUtil
import com.intellij.util.io.Compressor
import com.intellij.util.messages.Topic
import com.jetbrains.plugins.webDeployment.WebDeploymentTopics
import com.jetbrains.plugins.webDeployment.config.ServerPasswordSafeDeployable
import com.jetbrains.plugins.webDeployment.config.WebServerConfig
import com.jetbrains.plugins.webDeployment.ui.auth.AuthHelper
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.compute
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.interpreter.OperatingSystem
import org.jetbrains.r.interpreter.RFsNotifier
import org.jetbrains.r.remote.RRemoteBundle
import org.jetbrains.r.remote.filesystem.RRemoteVFS
import org.jetbrains.r.remote.filesystem.RVirtualFileUploadingListener
import org.jetbrains.r.rinterop.RInteropUtil
import org.jetbrains.r.util.RPathUtil
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RRemoteHost internal constructor(var sshConfig: SshConfig) {
  lateinit var configId: String
    private set
  lateinit var credentials: RemoteCredentials
    private set
  lateinit var sessionConfig: SessionConfig
    private set
  lateinit var config: ServerPasswordSafeDeployable
    private set
  private var prevSshConfig: SshConfig? = null

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

  private val remoteHomePathValue = object : AtomicClearableLazyValue<String>() {
    override fun compute(): String {
      return useSftpChannel { channel ->
        channel.home.let { if (operatingSystem == OperatingSystem.WINDOWS && it.startsWith('/')) it.substring(1) else it }
      }
    }
  }
  private val remoteHomePath get() = remoteHomePathValue.value

  val remoteHelpersRoot: String
    get() {
      return RPathUtil.join(remoteHomePath, HELPERS_DIRECTORY_NAME, RPluginUtil.getPlugin().version.makeFileName())
    }

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
  private val uploadHelpersLock = ReentrantLock()
  private val uploadRWrapperLock = ReentrantLock()
  private var helpersUploaded = false
  private var rwrapperUploaded = false

  val virtualFileUploadingListener = RVirtualFileUploadingListener(this).also {
    ApplicationManager.getApplication().messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, it)
  }

  init {
    refresh()
  }

  internal fun refresh() {
    sshConfig = SshConfigManager.getInstance(null).findConfigById(sshConfig.id) ?: sshConfig
    configId = sshConfig.id
    credentials = sshConfig.copyToCredentials()
    sessionConfig = credentials.connectionBuilder().buildSessionConfig()
    config = object : ServerPasswordSafeDeployable(WebServerConfig(), SshUiData(sshConfig, false)) {
      init {
        sshUiData!!.loadFromCredentials(this@RRemoteHost.credentials)
        myServer.fileTransferConfig.setSshConfig(sshConfig)
      }

      override fun getRootNodePresentableDescription(showRootFolder: Boolean): String {
        return presentableName + server.fileTransferConfig.rootFolder.takeIf { it.isNotBlank() }?.let { " : $it" }.orEmpty()
      }

      override fun refreshCredentials() {
        super.refreshCredentials()
        sshUiData?.let {
          it.loadFromCredentials(this@RRemoteHost.credentials)
          server.fileTransferConfig.setSshConfig(it.config)
        }
      }
    }.also {
      it.server.setName(presentableName)
    }

    val hostChanged = prevSshConfig?.let {
      sshConfig.host != it.host || sshConfig.literalPort != it.literalPort || sshConfig.username != it.username ||
      sshConfig.literalLocalPort != it.literalLocalPort
    } ?: true
    if (hostChanged) {
      operatingSystemValue.drop()
      remoteHomePathValue.drop()
      remoteTempDirValue.drop()
      helpersUploaded = false
      rwrapperUploaded = false
      ApplicationManager.getApplication().messageBus.syncPublisher(REMOTE_HOST_CHANGED).onChanged(this)
    }

    prevSshConfig = sshConfig.clone()
  }

  fun ensureHasCredentials() {
    try {
      val channel = SshConnectionService.instance.sftp(sessionConfig, SftpChannelConfig(null, CHECK_AUTH_TIMEOUT))
      channel.close()
    } catch (_: AuthFailSshTransportException) {
      AuthHelper.setDeployableToReAuth(config)
      invokeAndWaitIfNeeded {
        val auth = AuthHelper.ensureAuthSpecified(
          null, config, ConnectionOwnerFactory.createConnectionOwnerWithDialogMessages(null))
        if (auth) {
          ApplicationManager.getApplication().messageBus.syncPublisher(WebDeploymentTopics.DEPLOYMENT_CONFIG).deploymentConfigChanged()
          refresh()
        }
      }
    }
  }

  inline fun <T> useSftpChannel(f: (SftpChannel) -> T): T {
    val channel = SshConnectionService.instance.sftp(sessionConfig, SftpChannelConfig())
    channel.use { return f(it) }
  }

  fun uploadTmpFile(name: String, content: ByteArray, preserveName: Boolean = false): String {
    return uploadFile(name, content, preserveName, remoteTempDir)
  }

  fun uploadFile(name: String, content: ByteArray, preserveName: Boolean = false, remoteDir: String): String {
    return useSftpChannel { channel ->
      val nameNoExt = FileUtil.getNameWithoutExtension(name)
      val extension = FileUtilRt.getExtension(name).takeIf { it.isNotEmpty() }?.let { ".$it" } ?: ""
      var iter = 0
      var remotePath: String
      while (true) {
        val remoteName = if (iter == 0) "$nameNoExt$extension" else "$nameNoExt-$iter$extension"
        remotePath = RPathUtil.join(remoteDir, remoteName)
        var remoteFile = channel.file(remotePath)
        if (remoteFile.exists()) {
          ++iter
        } else {
          if (preserveName) {
            try {
              remoteFile.mkdir()
            } catch (e: SftpChannelException) {
              if (remoteFile.exists()) {
                ++iter
                continue
              }
              throw e
            }
            remotePath = RPathUtil.join(remotePath, name)
            remoteFile = channel.file(remotePath)
          }
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

  fun createTmpDir(name: String): String {
    return useSftpChannel { channel -> createTmpDir(name, channel) }
  }

  private fun createTmpDir(name: String, channel: SftpChannel): String {
    var iter = 0
    val parentDir = remoteTempDir
    var remotePath: String
    while (true) {
      val remoteName = if (iter == 0) name else "$name-$iter"
      remotePath = RPathUtil.join(parentDir, remoteName)
      val remoteFile = channel.file(remotePath)
      if (remoteFile.exists()) {
        ++iter
      } else {
        try {
          remoteFile.mkdir()
        } catch (e: SftpChannelException) {
          if (remoteFile.exists()) {
            ++iter
            continue
          }
          throw e
        }
        break
      }
    }
    return remotePath
  }

  private fun uploadTmpDir(dir: File): String {
    return useSftpChannel { channel ->
      val tmpDir = createTmpDir(dir.name, channel)
      channel.uploadFileOrDir(dir, PathUtil.getParentPath(tmpDir), PathUtil.getFileName(tmpDir))
      tmpDir
    }
  }

  fun uploadFileIfNeeded(file: File, preserveName: Boolean = false): String {
    if (FileUtil.isAncestor(RPluginUtil.helpersPath, file.path, false)) {
      if (file == File(RInteropUtil.getWrapperPath(operatingSystem))) {
        return ensureRWrapperUploaded()
      } else {
        ensureHelpersUploaded()
        val relativePath = FileUtil.getRelativePath(File(RPluginUtil.helpersPath), file)!!
        return RPathUtil.join(remoteHelpersRoot, relativePath)
      }
    }
    if (file.isDirectory) {
      return uploadTmpDir(file)
    } else {
      return uploadTmpFile(file.name, file.readBytes(), preserveName)
    }
  }

  fun uploadFileIfNeeded(file: VirtualFile, preserveName: Boolean = false): String {
    RRemoteVFS.getHostAndPath(file)?.let { (host, path) ->
      if (host == this) {
        return path
      }
    }
    if (file.isInLocalFileSystem) {
      return uploadFileIfNeeded(File(file.path), preserveName)
    }
    return uploadTmpFile(file.name, file.contentsToByteArray(), preserveName)
  }

  fun createProcess(command: GeneralCommandLine, timeoutMillis: Long? = DEFAULT_TIMEOUT_MILLIS, workingDir: String? = null): SshExecProcess {
    var dir = workingDir
    try { dir?.let { mkDirs(it) } } catch (e: SftpChannelException) { dir = null }
    val commandLine = buildCommand(command, dir ?: remoteHomePath)
    return ExecBuilder(sessionConfig, commandLine).execute(timeoutInSeconds = ((timeoutMillis?.toInt() ?: 0) + 999) / 1000)
  }

  fun runCommand(command: GeneralCommandLine, timeoutMillis: Long? = null): ProcessOutput {
    val process = createProcess(command, timeoutMillis)
    val result = ProcessOutput()
    if (timeoutMillis != null && !process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
      process.destroy()
      result.exitCode = -1
      result.setTimeout()
    } else {
      if (timeoutMillis == null) process.waitFor()
      result.exitCode = process.exitValue()
    }
    result.appendStdout(process.inputStream.bufferedReader().readText())
    result.appendStderr(process.errorStream.bufferedReader().readText())
    return result
  }

  fun checkPermissions(path: String, permissions: String): Boolean {
    val commandLine = permissions
      .filter { it in "rwx" }
      .map { "[ -$it ${CommandLineUtil.posixQuote(path)} ]" }
      .also { if (it.isEmpty()) return true }
      .joinToString(" && ")
    val process = ExecBuilder(sessionConfig, commandLine).execute(timeoutInSeconds = DEFAULT_TIMEOUT_MILLIS.toInt() / 1000)
    if (!process.waitFor(DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) return false
    return process.exitValue() == 0
  }

  fun ensureHelpersUploaded() = uploadHelpersLock.withLock {
    useSftpChannel { channel ->
      val remoteDir = channel.file(remoteHelpersRoot)
      fun checkValidationFile(): Boolean {
        return try {
          remoteDir.exists() && remoteDir.child(VALIDATION_FILE_NAME).exists()
        } catch (_: SftpChannelException) {
          false
        }
      }
      if (!(IS_RPLUGIN_SNAPSHOT && !helpersUploaded) && checkValidationFile()) {
        return
      }
      runWithProgress(RRemoteBundle.message("remote.host.uploading.helpers.title", presentableName)) { indicator ->
        LOG.debug("Uploading helpers to $presentableName")
        val localTar = FileUtil.createTempFile("r-helpers", ".tar.gz", true)
        Compressor.Tar(localTar, Compressor.Tar.Compression.GZIP).use { tar ->
          val files = listOf("R", RFsNotifier.getFsNotifierExecutableName(operatingSystem))
          files.forEach { name ->
            val file = File(RPluginUtil.helpersPath, name)
            if (!file.exists()) return@forEach
            if (file.isDirectory) {
              tar.addDirectory(file.name, file)
            } else {
              tar.addFile(file.name, file)
            }
          }
        }
        val tmpDir = createTmpDir("helpers", channel)
        val remoteTar = RPathUtil.join(tmpDir, "r-helpers.tar.gz")
        channel.uploadWithProgress(localTar, remoteTar, indicator)
        localTar.delete()
        runCommand(GeneralCommandLine("tar", "-xf", remoteTar, "-C", tmpDir)).ensureSuccess()
        channel.file(remoteTar).rm()
        if (!IS_RPLUGIN_SNAPSHOT && checkValidationFile()) {
          runCommand(GeneralCommandLine("rm", "-rf", "--", tmpDir))
          return@runWithProgress
        }
        if (remoteDir.exists()) {
          runCommand(GeneralCommandLine("rm", "-rf", "--", remoteDir.path()))
        }
        try {
          channel.mkDirs(PathUtil.getParentPath(remoteDir.path()))
          runCommand(GeneralCommandLine("mv", "-T", "--", tmpDir, remoteDir.path())).ensureSuccess()
          remoteDir.child(VALIDATION_FILE_NAME).outputStream(false).use { it.write(1) }
          helpersUploaded = true
        } catch (e: Exception) {
          if (!checkValidationFile()) throw e
        }
      }
    }
  }

  private fun ensureRWrapperUploaded(): String = uploadRWrapperLock.withLock {
    useSftpChannel { channel ->
      val dirName = rkernelVersion?.let { "rkernel_$rkernelVersion" }
                    ?: "rplugin_${PathUtil.suggestFileName(RPluginUtil.getPlugin().version).makeFileName()}"
      val localFile = File(RInteropUtil.getWrapperPath(operatingSystem))
      val fileName = localFile.name
      val remotePath = RPathUtil.join(remoteHomePath, HELPERS_DIRECTORY_NAME, HELPERS_RWRAPPER_DIRECTORY_NAME, dirName, fileName)
      val validationFile = "$remotePath$VALIDATION_FILE_NAME"
      fun checkValidationFile(): Boolean {
        return try {
          channel.file(validationFile).exists()
        } catch (_: SftpChannelException) {
          false
        }
      }
      if ((!(IS_RPLUGIN_SNAPSHOT && !rwrapperUploaded) || rkernelVersion != null) && checkValidationFile()) return remotePath
      runWithProgress(RRemoteBundle.message("remote.host.uploading.file.title", fileName, presentableName)) { indicator ->
        val tmpDir = createTmpDir("rwrapper", channel)
        try {
          val tmpFile = RPathUtil.join(tmpDir, fileName)
          var downloaded = false
          if (rkernelVersion != null) {
            val url = "https://bintray.com/jetbrains/rplugin/download_file?file_path=$rkernelVersion-$fileName"
            LOG.debug("Trying to download rwrapper from $url to $presentableName")
            indicator.text2 = RRemoteBundle.message("remote.host.downloading.text", url)
            indicator.isIndeterminate = true
            val downloadCommands = listOf(
              GeneralCommandLine("curl", "-o", tmpFile, "--connect-timeout", "5", "-L", "--", url),
              GeneralCommandLine("wget", "-O", tmpFile, "--timeout=5", "--", url))
            for (command in downloadCommands) {
              if (runCommand(GeneralCommandLine("command", "-v", command.exePath)).exitCode != 0) {
                LOG.debug("'${command.exePath}' does not exist on $presentableName")
                continue
              }
              LOG.debug("Running $command")
              val result = runCommand(command, DOWNLOAD_RWRAPPER_TIMEOUT)
              if (result.exitCode == 0 && channel.file(tmpFile).let { it.exists() && it.size() > 0 }) {
                downloaded = true
                LOG.debug("Download successful")
              } else {
                LOG.warn(
                  "Download failed: exitcode = ${result.exitCode}; stdout = ${result.stdout.trim()}; stderr = ${result.stderr.trim()}")
              }
              break
            }
          }
          if (!downloaded) {
            if (!localFile.exists()) {
              throw RuntimeException("Cannot find suitable RWrapper version in " + localFile.path)
            }
            LOG.debug("Uploading $fileName to $presentableName")
            channel.uploadWithProgress(localFile, tmpFile, indicator)
          }
          try {
            channel.mkDirs(PathUtil.getParentPath(remotePath))
            channel.file(remotePath).rm()
            runCommand(GeneralCommandLine("mv", "-T", "--", tmpFile, remotePath)).ensureSuccess()
            channel.file(validationFile).outputStream(false).use { it.write(1) }
            rwrapperUploaded = true
          } catch (e: Exception) {
            if (!checkValidationFile()) throw e
          }
        } finally {
          runCommand(GeneralCommandLine("rm", "-rf", "--", tmpDir))
        }
      }
      remotePath
    }
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
    private const val DEFAULT_TIMEOUT_MILLIS = 60000L
    private const val CHECK_AUTH_TIMEOUT = 5000L
    private const val DOWNLOAD_RWRAPPER_TIMEOUT = 180000L
    private val LOG = Logger.getInstance(RRemoteHost::class.java)
    private val IS_RPLUGIN_SNAPSHOT get() = RPluginUtil.getPlugin().version.contains("SNAPSHOT")
    private const val HELPERS_DIRECTORY_NAME = ".jetbrains_rplugin_helpers"
    private const val HELPERS_RWRAPPER_DIRECTORY_NAME = "rwrapper"
    private val RKERNEL_VERSION_HELPER = RPluginUtil.findFileInRHelpers("rkernelVersion.txt")
    private val rkernelVersion by lazy { RKERNEL_VERSION_HELPER.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() } }
    private const val VALIDATION_FILE_NAME = ".is_valid"

    val REMOTE_HOST_CHANGED = Topic("R Remote Host Changed", RRemoteHostChangedListener::class.java)

    private fun <R> runWithProgress(title: String, f: (ProgressIndicator) -> R): R {
      val promise = AsyncPromise<R>()
      runBackgroundableTask(title, null, false) {
        promise.compute { f(it) }
      }
      try {
        return promise.get()!!
      } catch (e: ExecutionException) {
        throw e.cause ?: e
      }
    }
  }
}

interface RRemoteHostChangedListener {
  fun onChanged(remoteHost: RRemoteHost)
}

fun SftpChannel.mkDirs(path: String) {
  if (path == "") return
  mkDirs(PathUtil.getParentPath(path))
  val file = file(path)
  if (!file.exists()) {
    try {
      file.mkdir()
    } catch (e: SftpChannelException) {
      if (!file.exists()) throw e
    }
  }
}

private fun String.makeFileName(): String {
  return replace(' ', '_').replace('/', '_')
}

private fun SftpChannel.uploadWithProgress(localFile: File, remotePath: String, indicator: ProgressIndicator) {
  val size = localFile.length()
  indicator.isIndeterminate = false
  indicator.fraction = 0.0
  indicator.text2 = "${StringUtil.formatFileSize(0)} / ${StringUtil.formatFileSize(size)}"
  var transferred = 0L
  uploadFileOrDir(localFile, PathUtil.getParentPath(remotePath), PathUtil.getFileName(remotePath), object : SftpProgressTracker {
    override val isCanceled = indicator.isCanceled

    override fun onFileCopied(file: File) {
    }

    override fun onBytesTransferred(count: Long) {
      transferred += count
      indicator.fraction = transferred.toDouble() / size
      indicator.text2 = "${StringUtil.formatFileSize(transferred)} / ${StringUtil.formatFileSize(size)}"
    }
  }, null)
}

private fun ProcessOutput.ensureSuccess() {
  if (isTimeout) throw TimeoutException()
  if (exitCode != 0) {
    throw com.intellij.execution.ExecutionException(
      RRemoteBundle.message("remote.interpreter.exception.with.exit.code", stderr.trim(), exitCode))
  }
}