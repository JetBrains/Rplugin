/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RPluginCoroutineScope
import com.intellij.r.psi.interpreter.OperatingSystem
import com.intellij.r.psi.interpreter.RInterpreter
import com.intellij.r.psi.interpreter.RInterpreterBase
import com.intellij.r.psi.interpreter.RInterpreterState
import com.intellij.r.psi.interpreter.RInterpreterUtil
import com.intellij.r.psi.interpreter.RLocalInterpreterLocation
import com.intellij.r.psi.interpreter.RLocalInterpreterProvider
import com.intellij.r.psi.rinterop.RInterop
import com.intellij.util.system.CpuArch
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.asPromise
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.RInteropImpl
import org.jetbrains.r.rinterop.RInteropUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class RLocalInterpreterProviderImpl : RLocalInterpreterProvider {
  override fun instantiate(location: RLocalInterpreterLocation, project: Project): RInterpreterBase {
    return RLocalInterpreterImpl(location, project)
  }
}

class RLocalInterpreterImpl(location: RLocalInterpreterLocation, project: Project) : RInterpreterBase(location, project) {
  private val interpreterPath = location.path

  override val version: Version = RInterpreterUtil.getVersionByLocation(location) ?: throw RuntimeException("Invalid R interpreter")
  private val versionInfo = RInterpreterUtil.loadInterpreterVersionInfo(location)
  override val interpreterName: String get() = versionInfo["version.string"]?.replace(' ', '_')  ?: "unnamed"

  override val basePath = project.basePath!!
  override val hostOS get() = OperatingSystem.current()
  override val hostArch get() = CpuArch.CURRENT
  override val interpreterPathOnHost get() = interpreterPath

  override fun createRInteropForProcess(process: ProcessHandler, port: Int): RInteropImpl {
    return RInteropUtil.createRInteropForLocalProcess(this, process, port)
  }

  override fun deleteFileOnHost(path: String) {
    File(path).delete()
  }

  override fun uploadFileToHost(file: File, remoteDir: String) {
    val target = Paths.get(remoteDir, file.name).toFile()
    // Note: `copyTo()` calls `mkdirs()`
    file.copyTo(target)
  }

  override fun uploadFileToHostIfNeeded(file: VirtualFile, preserveName: Boolean): String {
    return file.path
  }

  override fun createFileChooserForHost(value: String, selectFolder: Boolean): TextFieldWithBrowseButton {
    return TextFieldWithBrowseButton().also { component ->
      component.text = value
      val descriptor = if (selectFolder) {
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
      } else {
        FileChooserDescriptorFactory.createSingleFileDescriptor()
      }
      component.addActionListener(ComponentWithBrowseButton.BrowseFolderActionListener(
        component, project, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT)
      )
      FileChooserFactory.getInstance().installFileCompletion(component.textField, descriptor, true, null)
    }
  }

  override fun showFileChooserDialogForHost(selectFolder: Boolean): String? {
    val descriptor = FileChooserDescriptor(!selectFolder, selectFolder, false, false, false, false)
    val dialog = FileChooserDialogImpl(descriptor, project)
    val choice = dialog.choose(project)
    return choice.firstOrNull()?.path
  }

  override fun createTempFileOnHost(name: String, content: ByteArray?): String {
    val i = name.indexOfLast { it == '.' }
    val file = if (i == -1) {
      createTempFileWithTimestamp(name, null)
    } else {
      createTempFileWithTimestamp(name.substring(0, i), name.substring(i))
    }
    content?.let { file.writeBytes(it) }
    return file.path
  }

  private fun createTempFileWithTimestamp(name: String, extension: String?): File {
    // Note: timestamps ensure the global uniqueness of file name in order to
    // avoid any issues with VFS caching when outdated file contents might be read
    return FileUtilRt.createTempFile("$name${System.currentTimeMillis()}", extension, true)
  }

  override fun createFileOnHost(name: String, content: ByteArray?, directory: String): String {
    val path = Paths.get(directory, name)
    path.toFile().parentFile.mkdirs()
    Files.write(path, content ?: "".toByteArray())
    LocalFileSystem.getInstance().refreshNioFiles(listOf(path))
    return path.toString()
  }

  override fun createTempDirOnHost(name: String): String = FileUtilRt.createTempDirectory(name, null, true).path

  override fun getGuaranteedWritableLibraryPath(libraryPaths: List<RInterpreterState.LibraryPath>, userPath: String): RInterpreter.PathWithInfo {
    val writable = libraryPaths.find { it.isWritable }
    return if (writable != null) {
      RInterpreter.PathWithInfo(writable.path, isUserDirectoryCreated = false)
    } else {
      RInterpreter.PathWithInfo(userPath, isUserDirectoryCreated = File(userPath).mkdirs())
    }
  }

  override fun showFileInViewer(rInterop: RInterop, pathOnHost: String): Promise<Unit> {
    return RPluginCoroutineScope.getScope(project).async(ModalityState.defaultModalityState().asContextElement()) {
      RToolWindowFactory.showFile(project, pathOnHost)
    }.asCompletableFuture().asPromise()
  }

  override fun showUrlInViewer(rInterop: RInterop, url: String) {
    RPluginCoroutineScope.getScope(project).launch(ModalityState.defaultModalityState().asContextElement()) {
      RToolWindowFactory.showUrl(project, url)
    }
  }
}
