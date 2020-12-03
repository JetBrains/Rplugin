package org.jetbrains.r.remote.filesystem

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class RRemoteFileEditorTabTitleProvider : EditorTabTitleProvider {
  override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
    val (host, _) = RRemoteVFS.getHostAndPath(file) ?: return null
    return "<${host.presentableName}> ${file.name}"
  }

  override fun getEditorTabTooltipText(project: Project, virtualFile: VirtualFile): String? {
    val (host, path) = RRemoteVFS.getHostAndPath(virtualFile) ?: return null
    return "${host.presentableName}:$path"
  }
}