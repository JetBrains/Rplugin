package com.intellij.r.psi.packages

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.r.psi.packages.LibrarySummary.RLibrarySymbol
import com.intellij.r.psi.skeleton.RSkeletonFileType

object RSkeletonUtilPsi {
  fun skeletonFileToRPackage(skeletonFile: PsiFile): RPackage? = RPackage.getOrCreateRPackageBySkeletonFile(skeletonFile)

  fun skeletonFileToRPackage(skeletonFile: VirtualFile): RPackage? {
    val (name, version) = skeletonFile.parent.name.split('-', limit = 2)
                            .takeIf { it.size == 2 }
                            ?.let { Pair(it[0], it[1]) } ?: return null
    return RPackage(name, version)
  }
}

data class RPackage(val name: String, val version: String) {
  companion object {
    /**
     * if [file] type is Skeleton File Type, returns package and version which was used for its generation or null otherwise
     */
    fun getOrCreateRPackageBySkeletonFile(file: PsiFile): RPackage? {
      if (!FileTypeRegistry.getInstance().isFileOfType(file.virtualFile, RSkeletonFileType)) return null
      return CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result(RSkeletonUtilPsi.skeletonFileToRPackage(file.virtualFile), file)
      }
    }
  }
}

val RLibrarySymbol.Type.isFunctionDeclaration: Boolean
  get() = this == RLibrarySymbol.Type.FUNCTION || this == RLibrarySymbol.Type.S4GENERIC || this == RLibrarySymbol.Type.S4METHOD
