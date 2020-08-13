/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.compute
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import java.io.File

class RLocalInterpreterImpl(
  override val interpreterLocation: RLocalInterpreterLocation,
  versionInfo: Map<String, String>, project: Project) : RInterpreterBase(versionInfo, project) {
  val interpreterPath get() = interpreterLocation.path
  override val basePath = project.basePath!!
  override val hostOS get() = OperatingSystem.current()

  override fun createRInteropForProcess(process: ProcessHandler, port: Int): RInterop {
    return RInteropUtil.createRInteropForLocalProcess(this, process, port)
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
      component.addActionListener(
        ComponentWithBrowseButton.BrowseFolderActionListener(
          null, null, component, project, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT))
      FileChooserFactory.getInstance().installFileCompletion(component.textField, descriptor, true, null)
    }
  }

  override fun showFileChooserDialogForHost(selectFolder: Boolean): String? {
    val descriptor = FileChooserDescriptor(
      !selectFolder, selectFolder, false, false, false, false)
    val dialog = FileChooserDialogImpl(descriptor, project)
    val choice = dialog.choose(project)
    return choice.firstOrNull()?.path
  }

  override fun createTempFileOnHost(name: String, content: ByteArray?): String {
    val i = name.indexOfLast { it == '.' }
    val file = if (i == -1) {
      FileUtilRt.createTempFile(name, null, true)
    } else {
      FileUtilRt.createTempFile(name.substring(0, i), name.substring(i), true)
    }
    content?.let { file.writeBytes(it) }
    return file.path
  }

  override fun createTempDirOnHost(name: String): String = FileUtilRt.createTempDirectory(name, null, true).path

  override fun getGuaranteedWritableLibraryPath(libraryPaths: List<RInterpreterState.LibraryPath>, userPath: String): Pair<String, Boolean> {
    val writable = libraryPaths.find { it.isWritable }
    return if (writable != null) {
      Pair(writable.path, false)
    } else {
      Pair(userPath, File(userPath).mkdirs())
    }
  }

  override fun showFileInViewer(rInterop: RInterop, pathOnHost: String): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    invokeLater {
      promise.compute {
        RToolWindowFactory.showFile(project, pathOnHost)
      }
    }
    return promise
  }

  override fun showUrlInViewer(rInterop: RInterop, url: String) {
    invokeLater {
      RToolWindowFactory.showUrl(project, url)
    }
  }

  companion object {
    val LOG = Logger.getInstance(RLocalInterpreterImpl::class.java)
  }
}
