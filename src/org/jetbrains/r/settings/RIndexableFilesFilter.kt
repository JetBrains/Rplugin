package org.jetbrains.r.settings

import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.IndexableFilesFilter
import org.jetbrains.r.packages.RSkeletonUtil

class RIndexableFilesFilter : IndexableFilesFilter {
  override fun shouldIndex(file: VirtualFile): Boolean = !Registry.`is`("pycharm.lazy.indexing", false) ||
  file.path.contains(RSkeletonUtil.SKELETON_DIR_NAME)
}