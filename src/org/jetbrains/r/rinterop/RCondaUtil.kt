/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rinterop

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileSystemUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SystemProperties
import java.io.File
import java.nio.file.Paths

object RCondaUtil {

  private val CONDA_DEFAULT_ROOTS = arrayOf("anaconda", "anaconda2", "anaconda3", "miniconda",
                                            "miniconda2", "miniconda3", "Anaconda", "Anaconda2",
                                            "Anaconda3", "Miniconda", "Miniconda2", "Miniconda3")

  fun getCondaRoot(systemConda: String): File? {
    var condaRoot: File? = Paths.get(systemConda).toFile()
    while (condaRoot != null && !CONDA_DEFAULT_ROOTS.contains(condaRoot.name)) {
      condaRoot = condaRoot.parentFile
    }
    return condaRoot
  }

  fun getEnvironmentName(file: File): String? {
    var environmentDir: File? = file
    while (environmentDir != null) {
      if (environmentDir.parentFile?.name == "envs" && CONDA_DEFAULT_ROOTS.contains(environmentDir.parentFile?.parentFile?.name)) break
      environmentDir = environmentDir.parentFile
    }
    return environmentDir?.name
  }

  fun getSystemCondaExecutable(): String? {
    val condaName = if (SystemInfo.isWindows) "conda.exe" else "conda"
    val condaInPath = PathEnvironmentVariableUtil.findInPath(condaName)
    return if (condaInPath != null) condaInPath.path else getCondaExecutableByName(condaName)
  }

  fun findCondaByRInterpreter(file: File): File? {
    var current: File? = file
    while (current != null) {
      current = current.parentFile
      if (CONDA_DEFAULT_ROOTS.contains(current?.name)) {
        findCondaByCondaRoot(current)?.let { return it }
      }
    }
    return null
  }

  private fun findCondaByCondaRoot(file: File): File? =
    Paths.get(file.absolutePath,
              if (SystemInfo.isWindows) "Scripts" else "bin",
              if (SystemInfo.isWindows) "conda.exe" else "conda").toFile().takeIf { it.exists() }


  private fun getCondaExecutableByName(condaName: String): String? {
    val userHome = LocalFileSystem.getInstance().findFileByPath(
      SystemProperties.getUserHome().replace('\\', '/'))
    if (userHome != null) {
      for (root in CONDA_DEFAULT_ROOTS) {
        var condaFolder = userHome.findChild(root)
        var executableFile = findExecutable(condaName, condaFolder)
        if (executableFile != null) return executableFile
        if (SystemInfo.isWindows) {
          val appData = userHome.findFileByRelativePath("AppData\\Local\\Continuum\\$root")
          executableFile = findExecutable(condaName, appData)
          if (executableFile != null) return executableFile
          condaFolder = LocalFileSystem.getInstance().findFileByPath("C:\\ProgramData\\$root")
          executableFile = findExecutable(condaName, condaFolder)
          if (executableFile != null) return executableFile
          condaFolder = LocalFileSystem.getInstance().findFileByPath("C:\\$root")
          executableFile = findExecutable(condaName, condaFolder)
          if (executableFile != null) return executableFile
        }
      }
    }
    if (!SystemInfo.isWindows) {
      val systemCondaFolder = LocalFileSystem.getInstance().findFileByPath("/opt/anaconda")
      val executableFile = findExecutable(condaName, systemCondaFolder)
      if (executableFile != null) return executableFile
    }
    return null
  }

  private fun findExecutable(condaName: String, condaFolder: VirtualFile?): String? {
    if (condaFolder != null) {
      val binFolder = condaFolder.findChild(if (SystemInfo.isWindows) "Scripts" else "bin")
      if (binFolder != null) {
        val bin = binFolder.findChild(condaName)
        if (bin != null) {
          val directoryPath = bin.path
          val executableFile = getExecutablePath(directoryPath, condaName)
          if (executableFile != null) {
            return executableFile
          }
        }
      }
    }
    return null
  }

  private fun getExecutablePath(homeDirectory: String, name: String): String? {
    val binPath = File(homeDirectory)
    val binDir = binPath.parentFile ?: return null
    var runner = File(binDir, name)
    if (runner.exists()) return LocalFileSystem.getInstance().extractPresentableUrl(runner.path)
    runner = File(File(binDir, "Scripts"), name)
    if (runner.exists()) return LocalFileSystem.getInstance().extractPresentableUrl(runner.path)
    runner = File(File(binDir.parentFile, "Scripts"), name)
    if (runner.exists()) return LocalFileSystem.getInstance().extractPresentableUrl(runner.path)
    runner = File(File(binDir.parentFile, "local"), name)
    if (runner.exists()) return LocalFileSystem.getInstance().extractPresentableUrl(runner.path)
    runner = File(File(File(binDir.parentFile, "local"), "bin"), name)
    if (runner.exists()) return LocalFileSystem.getInstance().extractPresentableUrl(runner.path)
    // if interpreter is a symlink
    if (FileSystemUtil.isSymLink(homeDirectory)) {
      val resolvedPath = FileSystemUtil.resolveSymLink(homeDirectory)
      if (resolvedPath != null) {
        return getExecutablePath(resolvedPath, name)
      }
    }
    // Search in standard unix path
    runner = File(File("/usr", "bin"), name)
    if (runner.exists()) return LocalFileSystem.getInstance().extractPresentableUrl(runner.path)
    runner = File(File(File("/usr", "local"), "bin"), name)
    return if (runner.exists()) LocalFileSystem.getInstance().extractPresentableUrl(runner.path) else null
  }
}