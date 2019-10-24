/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import com.intellij.util.indexing.*
import org.jetbrains.r.RFileType
import org.jetbrains.r.psi.api.RAssignmentStatement

class RPackageInfoIndex : SingleEntryFileBasedIndexExtension<RPackageInfo>(), PsiDependentIndex {
  override fun getName(): ID<Int, RPackageInfo> = KEY

  override fun getInputFilter(): FileBasedIndex.InputFilter = RFileType.INPUT_FILTER

  override fun getIndexer() = INDEXER

  override fun getValueExternalizer() = RPackageInfo.DATA_EXTERNALIZER

  override fun getVersion() = 1

  companion object {
    val KEY = ID.create<Int, RPackageInfo>("rplugin.packageInfo.index")

    private const val SKELETON_PACKAGE_TITLE = ".skeleton_package_title"
    private const val SKELETON_VERSION = ".skeleton_version"
    private const val SKELETON_PACKAGE_DEPENDS = ".skeleton_package_depends"
    private const val SKELETON_PACKAGE_IMPORTS = ".skeleton_package_imports"
    private const val SKELETON_LOADED_LIBRARIES = ".skeleton_loaded_libraries"

    private val INDEXER = object: SingleEntryIndexer<RPackageInfo>(false) {
      override fun computeValue(inputData: FileContent): RPackageInfo? {
        var packageTitle: String? = null
        var skeletonVersion: Int? = null
        var packageDepends: List<String>? = null
        var packageImports: List<String>? = null
        var packageLoadedLibraries: List<String>? = null

        for (assignment in inputData.psiFile.children.filterIsInstance<RAssignmentStatement>()) {
          val lhsText = assignment.assignee?.text ?: continue
          when {
            lhsText == SKELETON_PACKAGE_TITLE    -> packageTitle           = getAssignedString(assignment)
            lhsText == SKELETON_VERSION          -> skeletonVersion        = assignment.assignedValue?.text?.toInt()
            lhsText == SKELETON_PACKAGE_DEPENDS  -> packageDepends         = toList(assignment)
            lhsText == SKELETON_PACKAGE_IMPORTS  -> packageImports         = toList(assignment)
            lhsText == SKELETON_LOADED_LIBRARIES -> packageLoadedLibraries = toList(assignment)
          }
        }
        if (packageTitle == null || skeletonVersion == null || packageDepends == null ||
            packageImports == null || packageLoadedLibraries == null) {
          return null
        }
        return RPackageInfo(packageTitle, packageDepends, packageImports, packageLoadedLibraries, skeletonVersion)
      }

      private fun getAssignedString(it: RAssignmentStatement) = it.assignedValue?.text?.removeSurrounding("\"")

      private fun toList(it: RAssignmentStatement) = getAssignedString(it)?.split(",")
    }
  }

}