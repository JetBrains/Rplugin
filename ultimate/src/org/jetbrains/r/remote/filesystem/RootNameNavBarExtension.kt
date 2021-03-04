package org.jetbrains.r.remote.filesystem

import com.intellij.ide.navigationToolbar.AbstractNavBarModelExtension
import com.intellij.psi.PsiDirectory
import org.jetbrains.plugins.notebooks.jupyter.remote.vfs.JupyterRemoteVirtualFile
import org.jetbrains.r.remote.host.RRemoteHostManager

class RootNameNavBarExtension : AbstractNavBarModelExtension() {
  override fun getPresentableText(obj: Any?): String? {
    if (obj !is PsiDirectory) return null
    val file = obj.virtualFile
    if (file.fileSystem is RRemoteVFS) {
      val pathParts = (file as JupyterRemoteVirtualFile).remotePath.fullPathParts
      if (pathParts.size == 1) {
        val authority = pathParts[0]
        return RRemoteHostManager.getInstance().getRemoteHostByConfigId(authority)?.presentableName
      }
    }
    return null
  }
}