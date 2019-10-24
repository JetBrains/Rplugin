// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProfiler
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import org.jetbrains.r.bin.RBinFileType
import org.jetbrains.r.interpreter.RInterpreterManager
import java.io.DataInput
import java.io.DataOutput
import java.io.File

private const val INTERPRETER = "interpreter"

enum class RPackagePriority {
  BASE,
  RECOMMENDED,
  NA
}

data class RPackage(val packageName: String, val packageVersion: String, val priority: RPackagePriority?, val libraryPath: String) {
  val isStandard: Boolean
    get() = priority == RPackagePriority.BASE || priority == RPackagePriority.RECOMMENDED

  val isUser: Boolean
    get() = priority == null || priority == RPackagePriority.NA

  fun getLibraryBinFileName() = "$packageName-${packageVersion}.${RBinFileType.DOT_R_BIN_EXTENSION}"

  companion object {
    private val SKELETON_FILE_REGEX = "([^-]*)-(.*)\\.${RBinFileType.DOT_R_BIN_EXTENSION}".toRegex()

    fun getOrCreate(file: PsiFile): RPackage? {
      return CachedValuesManager.getCachedValue(file) { CachedValueProvider.Result<RPackage>(create(file), file) }
    }

    private fun create(file: PsiFile): RPackage? {
      val interpreter = RInterpreterManager.getInterpreter(file.project) ?: return null
      val skeletonRoots = interpreter.skeletonRoots
      val skeletonPath = skeletonRoots.firstOrNull { VfsUtil.isAncestor(it, file.virtualFile, true) } ?: return null
      val virtualFile = file.virtualFile
      val name = virtualFile?.name ?: return null
      val priority = RSkeletonUtil.getPriorityFromSkeletonFile(File(virtualFile.path))
      val libraryPath = interpreter.findLibraryPathBySkeletonPath(skeletonPath.path) ?: return null
      return SKELETON_FILE_REGEX.matchEntire(name)?.let { RPackage(it.groupValues[1], it.groupValues[2], priority, libraryPath) }
    }
  }
}

data class RPackageInfo(
  val packageTitle: String,
  val packageDependencies: List<String>,
  val packageImports: List<String>,
  val loadedPackages: List<String>,
  val skeletonizerVersion: Int
) {
  companion object {
    val DATA_EXTERNALIZER = object:DataExternalizer<RPackageInfo> {
      override fun save(out: DataOutput, value: RPackageInfo) {
        IOUtil.writeUTF(out, value.packageTitle)
        IOUtil.writeStringList(out, value.packageDependencies)
        IOUtil.writeStringList(out, value.packageImports)
        IOUtil.writeStringList(out, value.loadedPackages)
        out.writeInt(value.skeletonizerVersion)
      }

      override fun read(input: DataInput): RPackageInfo {
        return RPackageInfo(IOUtil.readUTF(input), IOUtil.readStringList(input), IOUtil.readStringList(input),
                            IOUtil.readStringList(input), input.readInt())
      }
    }
  }
}