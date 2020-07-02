package org.jetbrains.r.settings

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.IndexableFilesFilter
import org.jetbrains.r.skeleton.RSkeletonFileType

class RIndexableFilesFilter : IndexableFilesFilter {
  override fun shouldIndex(file: VirtualFile): Boolean = file.fileType == RSkeletonFileType
}