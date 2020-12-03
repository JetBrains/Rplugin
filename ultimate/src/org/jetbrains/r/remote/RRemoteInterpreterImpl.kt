/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.remote.BaseRemoteProcessHandler
import com.intellij.ssh.SftpChannelException
import com.intellij.ssh.SftpChannelNoSuchFileException
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ssh.process.SshExecProcess
import com.intellij.util.PathUtil
import com.jetbrains.plugins.remotesdk.ui.RemoteBrowseActionListener
import com.jetbrains.plugins.webDeployment.config.FileTransferConfig
import com.jetbrains.plugins.webDeployment.ui.ServerBrowserDialog
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.compute
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.interpreter.*
import org.jetbrains.r.remote.filesystem.RRemoteHostViewManager
import org.jetbrains.r.remote.filesystem.RRemoteVFS
import org.jetbrains.r.remote.host.RRemoteHost
import org.jetbrains.r.remote.host.RRemoteHostChangedListener
import org.jetbrains.r.remote.host.mkDirs
import org.jetbrains.r.remote.settings.RRemoteSettings
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import org.jetbrains.r.util.RPathUtil
import org.jetbrains.r.util.tryRegisterDisposable
import java.io.File

class RRemoteInterpreterImpl(
  location: RRemoteInterpreterLocation,
  versionInfo: Map<String, String>,
  project: Project) : RInterpreterBase(versionInfo, project) {
  override var interpreterLocation: RRemoteInterpreterLocation = location
    private set
  val remoteHost = interpreterLocation.remoteHost
  override val basePath get() = interpreterLocation.basePath
  override val hostOS get() = remoteHost.operatingSystem
  override val interpreterPathOnHost get() = interpreterLocation.remotePath

  private var currentDisposable: Disposable? = null

  override val interpreterName: String
    get() {
      val remoteHostName = PathUtil.suggestFileName(remoteHost.presentableName, true, false)
      return "Remote_${remoteHostName}_${super.interpreterName}"
    }

  init {
    remoteHost.useSftpChannel { channel ->
      channel.mkDirs(basePath)
      removeRDataTmpFiles(channel)
    }
  }

  override fun suggestConsoleName(workingDirectory: String): String {
    val info = RInterpreterUtil.suggestAllInterpreters(true).firstOrNull { it.interpreterLocation == interpreterLocation }
    val name = info?.interpreterName ?: remoteHost.presentableName
    return "$name ($workingDirectory)"
  }

  override fun onSetAsProjectInterpreter() {
    super.onSetAsProjectInterpreter()
    RRemoteHostViewManager.getInstance(project).addInterpreter(this)
    currentDisposable = Disposer.newDisposable().also {
      ApplicationManager.getApplication().messageBus.connect(it)
        .subscribe(RRemoteHost.REMOTE_HOST_CHANGED, object : RRemoteHostChangedListener {
          override fun onChanged(remoteHost: RRemoteHost) {
            if (remoteHost != this@RRemoteInterpreterImpl.remoteHost) return
            RInterpreterManager.restartInterpreter(project)
          }
      })
      project.tryRegisterDisposable(it)
    }
  }

  override fun onUnsetAsProjectInterpreter() {
    super.onUnsetAsProjectInterpreter()
    currentDisposable?.let {
      Disposer.dispose(it)
      currentDisposable = null
    }
    if (RRemoteSettings.getInstance(project).closeRemoteHostView) {
      RRemoteHostViewManager.getInstance(project).removeInterpreter(this)
    }
  }

  override fun getFilePathAtHost(file: VirtualFile): String? {
    return RRemoteVFS.getHostAndPath(file)?.let { (host, path) ->
      path.takeIf { host == remoteHost }
    }
  }

  override fun findFileByPathAtHost(path: String): VirtualFile? {
    return RRemoteVFS.instance.findFileByPath(remoteHost, path)
  }

  override fun downloadFileFromHost(path: String, localPath: String) = remoteHost.useSftpChannel { channel ->
    try {
      channel.downloadFileOrDir(path, localPath)
    } catch (e: SftpChannelNoSuchFileException) {
    }
  }

  override fun getHelpersRootOnHost() = remoteHost.remoteHelpersRoot

  override fun createRInteropForProcess(process: ProcessHandler, port: Int): RInterop {
    val session = ((process as? BaseRemoteProcessHandler<*>)?.process as? SshExecProcess)?.session
                  ?: throw RuntimeException("Process is not remote")
    val localPort = session.addLocalTunnelWithRandomLocalPort("127.0.0.1", port)
    process.addProcessListener(object : ProcessAdapter() {
      override fun processTerminated(event: ProcessEvent) {
        session.removeLocalTunnel(localPort)
      }
    })
    val rInterop = RInterop(this, process, "127.0.0.1", localPort, project)
    val workspaceFile = if (ApplicationManager.getApplication().isUnitTestMode) {
      project.getUserData(RInteropUtil.WORKSPACE_FILE_FOR_TESTS)
    } else {
      interpreterLocation.remotePath.hashCode().toString().let {
        RPathUtil.join(basePath, ".RDataFiles", "$it.RData")
      }
    }
    val remoteScriptsPath = RPathUtil.join(remoteHost.remoteHelpersRoot, "R")
    remoteHost.ensureHelpersUploaded()
    rInterop.init(remoteScriptsPath, basePath, workspaceFile)
    rInterop.putUserData(RInteropUtil.TERMINATE_WITH_REPORT_HANDLER) {
      runProcessOnHost(GeneralCommandLine("kill", "-SIGABRT", rInterop.processPid.toString()))
    }
    return rInterop
  }

  override fun deleteFileOnHost(path: String) {
    remoteHost.useSftpChannel { channel ->
      channel.file(path).rm()
    }
  }

  override fun uploadFileToHost(file: File, remoteDir: String) {
    remoteHost.useSftpChannel { channel ->
      channel.uploadFileOrDir(file, remoteDir, file.name)
    }
  }

  override fun uploadFileToHostIfNeeded(file: VirtualFile, preserveName: Boolean): String {
    return remoteHost.uploadFileIfNeeded(file, preserveName)
  }

  override fun createFileChooserForHost(value: String, selectFolder: Boolean): TextFieldWithBrowseButton {
    return TextFieldWithBrowseButton().also { component ->
      component.text = value
      component.addActionListener(RemoteBrowseActionListener(component.textField,
                                                             RRemoteBundle.message("remote.file.chooser.title")) { consumer ->
        runAsync { consumer.consume(remoteHost.credentials) }
      }.withFoldersOnly(selectFolder))
    }
  }

  override fun showFileChooserDialogForHost(selectFolder: Boolean): String? {
    val dialog = ServerBrowserDialog(project, remoteHost.config,
                                     RRemoteBundle.message("remote.file.chooser.title"), selectFolder,
                                     FileTransferConfig.Origin.Default, null)
    dialog.show()
    if (!dialog.isOK) return null
    return dialog.path?.path
  }

  override fun createTempFileOnHost(name: String, content: ByteArray?): String {
    return remoteHost.uploadTmpFile(name, content ?: ByteArray(0))
  }

  override fun createFileOnHost(name: String, content: ByteArray?, directory: String): String {
    return remoteHost.uploadFile(name, content ?: "".toByteArray(), remoteDir = directory)
  }

  override fun createTempDirOnHost(name: String): String = remoteHost.createTmpDir(name)

  override fun getGuaranteedWritableLibraryPath(libraryPaths: List<RInterpreterState.LibraryPath>, userPath: String): Pair<String, Boolean> {
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

  override fun prepareForExecution(): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    invokeLater {
      FileDocumentManager.getInstance().saveAllDocuments()
      remoteHost.virtualFileUploadingListener.ensureCurrentUploadsFinished().onProcessed {
        promise.setResult(Unit)
      }
    }
    return promise
  }

  private fun removeRDataTmpFiles(channel: SftpChannel) {
    try {
      val dir = channel.file(RPathUtil.join(basePath, ".RDataFiles"))
      if (dir.exists() && dir.isDir()) {
        dir.list().filter { ".RDataTmp" in it.name() }.forEach { it.rm() }
      }
    } catch (e: SftpChannelException) {
      LOG.warn(e)
    }
  }

  private fun getHttpdLocalAndRemotePortAsync(rInterop: RInterop): Promise<Pair<Int, Int>> {
    return rInterop.getUserData(INTEROP_HTTPD_LOCAL_PORT_PROMISE) ?: run {
      val promise = rInterop.startHttpd().thenAsync { remotePort ->
        runAsync {
          val process = (rInterop.processHandler as BaseProcessHandler<*>).process as SshExecProcess
          val localPort = process.session.addLocalTunnelWithRandomLocalPort("127.0.0.1", remotePort)
            .also { rInterop.tryRegisterDisposable(Disposable { process.session.removeLocalTunnel(it) }) }
          localPort to remotePort
        }
      }
      // CAUTION: an experimentation has shown that `putCopyableUserData()`
      // doesn't pair with `getUserData()` and thus a brand new instance of
      // HTTP server will be created each time
      rInterop.putUserData(INTEROP_HTTPD_LOCAL_PORT_PROMISE, promise)
      promise.onError { rInterop.putUserData(INTEROP_HTTPD_LOCAL_PORT_PROMISE, null) }
      promise
    }
  }

  override fun showFileInViewer(rInterop: RInterop, pathOnHost: String): Promise<Unit> {
    return getHttpdLocalAndRemotePortAsync(rInterop).thenAsync { (port, _) ->
      val url = "http://127.0.0.1:$port/$HTTPD_GET_FILE_PREFIX$pathOnHost"
      val promise = AsyncPromise<Unit>()
      invokeLater {
        promise.compute {
          RToolWindowFactory.showUrl(project, url)
        }
      }
      promise
    }
  }

  override fun showUrlInViewer(rInterop: RInterop, url: String) {
    val match = LOCALHOST_REGEX.find(url)
    if (match != null) {
      getHttpdLocalAndRemotePortAsync(rInterop).onProcessed { ports ->
        val localPort = ports?.first ?: -1
        val remotePort = ports?.second ?: -1
        val newUrl = if (match.groupValues[3] == remotePort.toString()) {
          url.replace(":$remotePort/", ":$localPort/")
        } else {
          url
        }
        invokeLater { RToolWindowFactory.showUrl(project, newUrl) }
      }
    } else {
      invokeLater { RToolWindowFactory.showUrl(project, url) }
    }
  }

  override fun translateLocalUrl(rInterop: RInterop, url: String, absolute: Boolean): Promise<String> {
    val match = LOCALHOST_REGEX.find(url)
    val promise = AsyncPromise<String>()
    rInterop.workingDir
    if (match != null) {
      getHttpdLocalAndRemotePortAsync(rInterop).onProcessed { ports ->
        val localPort = ports?.first ?: -1
        val remotePort = ports?.second ?: -1
        val result = if (absolute) {
          if (match.groupValues[3] == remotePort.toString()) {
            url.replace(":$remotePort/", ":$localPort/")
          } else {
            url
          }
        } else {
          "p/" + if (match.groupValues[3] == remotePort.toString()) {
            localPort
          }
          else {
            match.groupValues[3]
          } + match.groupValues[4]
        }
        promise.setResult(result)
      }
    } else {
      promise.setResult(url)
    }
    return promise
  }

  companion object {
    val LOG = Logger.getInstance(RRemoteInterpreterImpl::class.java)
    private val INTEROP_HTTPD_LOCAL_PORT_PROMISE = Key<Promise<Pair<Int, Int>>>("org.jetbrains.r.remote.InteropHttpdLocalPortPromise")
    private const val HTTPD_GET_FILE_PREFIX = "custom/jb_get_file/"
    private val LOCALHOST_REGEX = Regex("^([A-Za-z]+://)?(127\\.0\\.0\\.1|localhost):([0-9]+)(/.*)?")

    fun checkBasePath(basePath: String, remoteHost: RRemoteHost): String? {
      if (!basePath.startsWith("/")) {
        return RRemoteBundle.message("add.remote.dialog.working.directory.invalid.path", basePath)
      }
      remoteHost.useSftpChannel { channel ->
        var path = basePath
        while (true) {
          val dir = channel.file(path)
          if (dir.exists()) {
            if (!dir.isDir()) {
              return RRemoteBundle.message("add.remote.dialog.working.directory.is.not.a.directory", path)
            }
            if (!remoteHost.checkPermissions(path, "rwx")) {
              return RRemoteBundle.message("add.remote.dialog.working.directory.not.writeable", path)
            }
            return null
          }
          if (path.length <= 1) break
          path = PathUtil.getParentPath(path).takeIf { it.isNotEmpty() } ?: "/"
        }
        return RRemoteBundle.message("add.remote.dialog.working.directory.invalid.path", basePath)
      }
    }
  }
}