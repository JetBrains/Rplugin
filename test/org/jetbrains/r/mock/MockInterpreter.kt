/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.mock

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.interpreter.OperatingSystem
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.RLocalInterpreterLocation
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.packages.RPackage
import org.jetbrains.r.packages.RSkeletonUtil
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import java.io.File
import java.nio.file.Paths

class MockInterpreter(override val project: Project, var provider: MockInterpreterProvider) : RInterpreter {
  override val interpreterLocation = RLocalInterpreterLocation(RInterpreterUtil.suggestHomePath())

  override val interpreterName = "test"

  override val version: Version = interpreterLocation.getVersion()!!

  override val skeletonPaths = listOf(RUsefulTestCase.SKELETON_LIBRARY_PATH)

  private val skeletonFiles
    get() = RSkeletonUtil.getSkeletonFiles(skeletonPaths.first())

  override val installedPackages: ExpiringList<RInstalledPackage>
    get() = provider.installedPackages.takeIf { it.isNotEmpty() } ?: ExpiringList(
      skeletonFiles.keys.map { RInstalledPackage(it.name, it.version, null, RUsefulTestCase.SKELETON_LIBRARY_PATH, emptyMap()) }) { false }

  override val skeletonRoots
    get() = VfsUtil.findFile(Paths.get(RUsefulTestCase.SKELETON_LIBRARY_PATH), false) ?.let { setOf(it) }.orEmpty()

  override val isUpdating: Boolean
    get() = provider.isUpdating ?: false

  override val userLibraryPath: String
    get() = provider.userLibraryPath

  override val interop: RInterop
    get() = provider.interop

  override val basePath = project.basePath!!

  override fun getPackageByName(name: String): RInstalledPackage? = installedPackages.firstOrNull { it.packageName == name }

  override fun getLibraryPathByName(name: String): VirtualFile? {
    throw NotImplementedError()
  }

  override fun getProcessOutput(scriptText: String): ProcessOutput? = throw NotImplementedError()

  override val libraryPaths: List<VirtualFile>
    get() = provider.libraryPaths

  override val skeletonsDirectory: String
    get() = RUsefulTestCase.SKELETON_LIBRARY_PATH

  override fun runHelper(helper: File, workingDirectory: String?, args: List<String>, errorHandler: ((ProcessOutput) -> Unit)?): String {
    return RInterpreterUtil.runHelper(interpreterLocation.path, helper, workingDirectory, args, errorHandler)
  }

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

  override fun runProcessOnHost(command: GeneralCommandLine): ProcessHandler {
    return ColoredProcessHandler(command.withWorkDirectory(basePath)).apply {
      setShouldDestroyProcessRecursively(true)
    }
  }

  override fun uploadHelperToHost(helper: File): String {
    return helper.absolutePath
  }
}
