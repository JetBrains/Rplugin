/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.interpreter.*
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import java.io.File

class MockInterpreter(override val project: Project) : RInterpreter {
  private val interpreterPath = RInterpreterUtil.suggestHomePath()

  override val interpreterLocation = RLocalInterpreterLocation(interpreterPath)

  override val interpreterName = "test"

  override val version: Version = interpreterLocation.getVersion()!!

  override val basePath = project.basePath!!

  override val hostOS: OperatingSystem
    get() = OperatingSystem.current()

  private val fsNotifier by lazy { RFsNotifier(this) }

  override fun createRInteropForProcess(process: ProcessHandler, port: Int): RInterop {
    return RInteropUtil.createRInteropForLocalProcess(this, process, port)
  }

  override fun uploadFileToHostIfNeeded(file: VirtualFile, preserveName: Boolean): String {
    return file.path
  }

  override fun createFileChooserForHost(value: String, selectFolder: Boolean): TextFieldWithBrowseButton {
    throw NotImplementedError()
  }

  override fun showFileChooserDialogForHost(selectFolder: Boolean): String? {
    throw NotImplementedError()
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

  override fun addFsNotifierListenerForHost(roots: List<String>, parentDisposable: Disposable, listener: (String) -> Unit) {
    fsNotifier.addListener(roots, parentDisposable, listener)
  }
}
