/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.interpreter.*
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import java.io.File
import java.nio.file.Paths

class MockInterpreter(override val project: Project, var provider: MockInterpreterProvider) : RInterpreter {
  private val interpreterPath = RInterpreterUtil.suggestHomePath()

  override val interpreterLocation = RLocalInterpreterLocation(interpreterPath)

  override val interpreterName = "test"

  override val version: Version = interpreterLocation.getVersion()!!

  override val skeletonPaths = listOf(RUsefulTestCase.SKELETON_LIBRARY_PATH)

  private val skeletonFiles
    get() = RSkeletonUtil.getSkeletonFiles(skeletonPaths.first())

  override val installedPackages: ExpiringList<RInstalledPackage>
    get() = provider.installedPackages.takeIf { it.isNotEmpty() } ?: ExpiringList(
      skeletonFiles.keys.map { RInstalledPackage(it.name, it.version, null, RUsefulTestCase.SKELETON_LIBRARY_PATH, emptyMap()) }) { false }

  override val skeletonRoots
    get() = VfsUtil.findFile(Paths.get(RUsefulTestCase.SKELETON_LIBRARY_PATH), true)?.let { setOf(it) }.orEmpty()

  override val isUpdating: Boolean
    get() = provider.isUpdating ?: false

  override val userLibraryPath: String
    get() = provider.userLibraryPath

  override val interop: RInterop
    get() = provider.interop

  override val basePath = project.basePath!!

  override fun getPackageByName(name: String): RInstalledPackage? = installedPackages.firstOrNull { it.packageName == name }

  override fun getLibraryPathByName(name: String): RInterpreter.LibraryPath? {
    throw NotImplementedError()
  }

  override val libraryPaths: List<RInterpreter.LibraryPath>
    get() = provider.libraryPaths

  override val skeletonsDirectory: String
    get() = RUsefulTestCase.SKELETON_LIBRARY_PATH

  override fun findLibraryPathBySkeletonPath(skeletonPath: String): String? = ""

  override fun getSkeletonFileByPackageName(name: String): PsiFile? {
    val installedPackage = getPackageByName(name) ?: return null
    val ioFile = File(skeletonPaths.first(), RPackage(installedPackage.name, installedPackage.version).skeletonFileName)
    val virtualFile = runAsync { VfsUtil.findFileByIoFile(ioFile, true) }
                        .onError { throw it }
                        .blockingGet(RInterpreterUtil.DEFAULT_TIMEOUT) ?: return null
    return PsiManager.getInstance(project).findFile(virtualFile)
  }

  override fun updateState() = resolvedPromise<Unit>()

  override val hostOS: OperatingSystem
    get() = OperatingSystem.current()

  override fun createRInteropForProcess(process: ProcessHandler, port: Int): RInterop {
    return RInteropUtil.createRInteropForLocalProcess(this, process, port)
  }

  override fun uploadFileToHostIfNeeded(file: VirtualFile, preserveName: Boolean): String {
    return file.path
  }

  override fun createFileChooserForHost(value: String, selectFolder: Boolean): TextFieldWithBrowseButton {
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

  override fun getGuaranteedWritableLibraryPath(libraryPaths: List<RInterpreter.LibraryPath>, userPath: String): Pair<String, Boolean> {
    val writable = libraryPaths.find { it.isWritable }
    return if (writable != null) {
      Pair(writable.path, false)
    } else {
      Pair(userPath, File(userPath).mkdirs())
    }
  }
}
