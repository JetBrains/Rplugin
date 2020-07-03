package org.jetbrains.r.remote.filesystem

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ssh.SftpChannelConfig
import com.intellij.ssh.SftpChannelException
import com.intellij.ssh.SshConnectionService
import kotlinx.coroutines.runBlocking
import org.jetbrains.concurrency.await
import org.jetbrains.concurrency.runAsync
import org.jetbrains.plugins.notebooks.jupyter.remote.JupyterRemotePath
import org.jetbrains.plugins.notebooks.jupyter.remote.vfs.JupyterRemoteFileStrategy
import org.jetbrains.plugins.notebooks.jupyter.remote.vfs.JupyterRemoteFileSystem
import org.jetbrains.plugins.notebooks.jupyter.remote.vfs.JupyterRemoteVirtualFile
import org.jetbrains.r.interpreter.OperatingSystem
import org.jetbrains.r.remote.host.RRemoteHost
import org.jetbrains.r.remote.host.RRemoteHostManager
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths

class RRemoteVFS : JupyterRemoteFileSystem(RRemoteFileStrategy) {
  override val localFileRoot: Path
    get() = Paths.get(PathManager.getSystemPath(), "rplugin-remote", "localRoot").toAbsolutePath()

  override val temporaryDirectory: Path
    get() = Paths.get(PathManager.getSystemPath(), "rplugin-remote", "tmp").toAbsolutePath()

  override fun getProtocol() = PROTOCOL

  fun findFileByPath(host: RRemoteHost, path: String): VirtualFile? {
    val convertedPath = if (host.operatingSystem == OperatingSystem.WINDOWS) {
      path.replace('\\', '/')
    } else {
      path
    }
    val jupyterPath = JupyterRemotePath.parse(host.configId, convertedPath)
    return runBlocking { openAsVirtualFile(jupyterPath) }
  }

  companion object {
    private const val PROTOCOL = "rplugin-remote"
    val instance = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as RRemoteVFS

    internal fun getHostAndPath(remotePath: JupyterRemotePath): Pair<RRemoteHost, String>? {
      val authority = remotePath.authority
      val host = RRemoteHostManager.getInstance().getRemoteHostByConfigId(authority) ?: return null
      val path = (if (host.operatingSystem == OperatingSystem.WINDOWS) "" else "/") +
                 remotePath.jupyterPathParts.joinToString("/")
      return host to path
    }

    internal fun getHostAndPath(remotePath: String): Pair<RRemoteHost, String>? {
      return getHostAndPath(JupyterRemotePath.parse(remotePath) ?: return null)
    }

    fun getHostAndPath(file: VirtualFile): Pair<RRemoteHost, String>? {
      if (file.fileSystem != instance) return null
      return getHostAndPath((file as JupyterRemoteVirtualFile).remotePath)
    }
  }
}

private object RRemoteFileStrategy : JupyterRemoteFileStrategy {
  override suspend fun fetchIntoLocalFile(remotePath: JupyterRemotePath, localPath: Path) {
    val (host, path) = RRemoteVFS.getHostAndPath(remotePath) ?: return
    runAsync {
      try {
        host.useSftpChannel { it.downloadFileOrDir(path, localPath.toString()) }
      } catch (_: SftpChannelException) {
      }
    }.await()
  }

  override suspend fun createDirectory(remotePath: JupyterRemotePath) {
    val (host, path) = RRemoteVFS.getHostAndPath(remotePath) ?: return
    runAsync {
      host.useSftpChannel { it.file(path).mkdir() }
    }.await()
  }

  override suspend fun delete(remotePath: JupyterRemotePath) {
    val (host, path) = RRemoteVFS.getHostAndPath(remotePath) ?: return
    runAsync {
      host.useSftpChannel { it.file(path).rm() }
    }.await()
  }

  override fun fileWriter(remotePath: JupyterRemotePath): OutputStream {
    val (host, path) = RRemoteVFS.getHostAndPath(remotePath) ?: return OutputStream.nullOutputStream()
    val channel = SshConnectionService.instance.sftp(host.sessionConfig, SftpChannelConfig())
    return channel.file(path).outputStream(false)
  }
}