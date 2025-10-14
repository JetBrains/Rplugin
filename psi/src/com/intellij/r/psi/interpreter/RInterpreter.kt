/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RPluginUtil
import com.intellij.r.psi.rinterop.RInterop
import com.intellij.r.psi.rinterop.RInteropCoroutineScope
import com.intellij.util.system.CpuArch
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

interface RInterpreter : RInterpreterInfo {
  val project: Project

  val basePath: String

  val hostOS: OperatingSystem

  val hostArch: CpuArch

  val interpreterPathOnHost: String

  fun suggestConsoleName(workingDirectory: String): String {
    val path = Path.of(project.basePath).parent.relativize(Path.of(workingDirectory)).toString()
    return LocalFileSystem.getInstance().extractPresentableUrl(path)
  }

  fun getFilePathAtHost(file: VirtualFile): String? {
    return if (file.isInLocalFileSystem) file.canonicalPath else null
  }

  fun findFileByPathAtHost(path: String): VirtualFile? {
    return VfsUtil.findFile(Paths.get(path), true)
  }

  fun downloadFileFromHost(path: String, localPath: String) {
    File(path).takeIf { it.exists() }?.copyTo(File(localPath), overwrite = true)
  }

  fun getHelpersRootOnHost(): String = RPluginUtil.helpersPath

  fun createRInteropForProcess(process: ProcessHandler, port: Int): RInterop

  fun deleteFileOnHost(path: String)

  fun uploadFileToHost(file: File, remoteDir: String)

  fun uploadFileToHostIfNeeded(file: VirtualFile, preserveName: Boolean = false): String

  fun createFileChooserForHost(value: String = "", selectFolder: Boolean = false): TextFieldWithBrowseButton

  fun showFileChooserDialogForHost(selectFolder: Boolean = false): String?

  fun createTempFileOnHost(name: String = "a", content: ByteArray? = null): String

  fun createFileOnHost(name: String = "a", content: ByteArray? = null, directory: String): String

  fun createTempDirOnHost(name: String = "a"): String

  // Note: returns writable path and indicator whether new library path was created
  fun getGuaranteedWritableLibraryPath(libraryPaths: List<RInterpreterState.LibraryPath>, userPath: String): PathWithInfo

  data class PathWithInfo(val path: String, val isUserDirectoryCreated: Boolean)

  @Deprecated("use prepareForExecution() instead")
  fun prepareForExecutionAsync(): Promise<Unit> {
    return RInteropCoroutineScope.wrapIntoPromise(project, this::prepareForExecution)
  }

  suspend fun prepareForExecution() {
    edtWriteAction {
      FileDocumentManager.getInstance().saveAllDocuments()
    }
  }

  fun showFileInViewer(rInterop: RInterop, pathOnHost: String): Promise<Unit> = resolvedPromise()

  fun showUrlInViewer(rInterop: RInterop, url: String) {}

  fun translateLocalUrl(rInterop: RInterop, url: String, absolute: Boolean): Promise<String> = AsyncPromise<String>().also { it.setResult(url) }

  fun addFsNotifierListenerForHost(roots: List<String>, parentDisposable: Disposable, listener: (Path) -> Unit)
}

data class LocalOrRemotePath(val path: String, val isRemote: Boolean)

fun RInterpreter.uploadFileToHost(file: File, preserveName: Boolean = false): String = interpreterLocation.uploadFileToHost(file, preserveName)

fun RInterpreter.runProcessOnHost(command: GeneralCommandLine, workingDirectory: String = basePath, isSilent: Boolean = false): BaseProcessHandler<*> =
  interpreterLocation.runProcessOnHost(command, workingDirectory, isSilent)

fun VirtualFile.getLocalOrRemotePath(interpreter: RInterpreter): LocalOrRemotePath? {
  if (isInLocalFileSystem) return LocalOrRemotePath(path, false)
  return interpreter.getFilePathAtHost(this)?.let { LocalOrRemotePath(it, true) }
}

fun LocalOrRemotePath.findFile(interpreter: RInterpreter, refreshIfNeeded: Boolean = false): VirtualFile? {
  if (isRemote) return interpreter.findFileByPathAtHost(path)
  return VfsUtil.findFile(Paths.get(path), refreshIfNeeded)
}
