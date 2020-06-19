/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.interpreter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.common.ExpiringList
import org.jetbrains.r.packages.RInstalledPackage
import org.jetbrains.r.rinterop.RInterop
import java.io.File

interface RInterpreter : RInterpreterInfo {
  val project: Project

  val isUpdating: Boolean

  val installedPackages: ExpiringList<RInstalledPackage>

  data class LibraryPath(val path: String, val isWritable: Boolean)
  val libraryPaths: List<LibraryPath>

  val userLibraryPath: String

  val interop: RInterop

  fun getPackageByName(name: String): RInstalledPackage?

  fun getLibraryPathByName(name: String): LibraryPath?

  fun getProcessOutput(scriptText: String): ProcessOutput?

  /**
   * @return a system-dependant paths to the skeleton roots
   */
  val skeletonPaths: List<String>

  val skeletonRoots: Set<VirtualFile>

  val basePath: String

  val hostOS: OperatingSystem

  /** A place where all skeleton-related data will be stored */
  val skeletonsDirectory: String

  fun runCommand(cmd: String): String? {
    return getProcessOutput(cmd)?.stdout
  }
  
  fun suggestConsoleName(workingDirectory: String): String {
    return "[ ${FileUtil.getLocationRelativeToUserHome(LocalFileSystem.getInstance().extractPresentableUrl(workingDirectory))} ]"
  }

  fun getFilePathAtHost(file: VirtualFile): String? {
    return if (file.isInLocalFileSystem) file.canonicalPath else null
  }
  
  fun findFileByPathAtHost(path: String): VirtualFile? {
    return LocalFileSystem.getInstance().findFileByPath(path)
  }

  fun uploadHelperToHost(helper: File): String

  fun runProcessOnHost(command: GeneralCommandLine): ProcessHandler

  fun runHelperProcess(script: String, args: List<String>, workingDirectory: String?): ProcessHandler

  /**
   * @param errorHandler if errorHelper is not null, it could be called instead of throwing the exception.
   * @throws RuntimeException if errorHandler is null and the helper exited with non-zero code or produced zero length output.
   */
  fun runHelper(helper: File, workingDirectory: String?, args: List<String>, errorHandler: ((ProcessOutput) -> Unit)? = null): String

  fun runMultiOutputHelper(helper: File,
                           workingDirectory: String?,
                           args: List<String>,
                           processor: RMultiOutputProcessor)

  fun getSkeletonFileByPackageName(name: String): PsiFile?

  fun updateState(): Promise<Unit>

  fun findLibraryPathBySkeletonPath(skeletonPath: String): String?

  fun createRInteropForProcess(process: ProcessHandler, port: Int): RInterop

  fun uploadFileToHostIfNeeded(file: VirtualFile): String

  fun createFileChooserForHost(value: String = "", selectFolder: Boolean = false): TextFieldWithBrowseButton

  fun createTempFileOnHost(name: String = "a", content: ByteArray? = null): String

  // Note: returns pair of writable path and indicator whether new library path was created
  fun getGuaranteedWritableLibraryPath(libraryPaths: List<LibraryPath> = this.libraryPaths,
                                       userPath: String = userLibraryPath): Pair<String, Boolean>

  fun registersRootsToWatch()
}

fun RInterpreter.isLocal(): Boolean = interpreterLocation is RLocalInterpreterLocation
