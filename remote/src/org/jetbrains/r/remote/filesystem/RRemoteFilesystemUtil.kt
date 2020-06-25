/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.remote.filesystem

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.PathUtilRt
import org.jetbrains.r.remote.RRemoteBundle
import org.jetbrains.r.remote.host.RRemoteHost

object RRemoteFilesystemUtil {
  fun editRemoteFile(project: Project, remoteHost: RRemoteHost, path: String) {
    val name = PathUtilRt.getFileName(path)
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Opening '$name'") {
      override fun run(indicator: ProgressIndicator) {
        val virtualFile = RRemoteVFS.instance.findFileByPath(remoteHost, path) ?: return
        invokeAndWaitIfNeeded {
          FileTypeChooser.getKnownFileTypeOrAssociate(name)
          val opened = OpenFileDescriptor(project, virtualFile).navigateInEditor(project, true)
          if (!opened) {
            Messages.showInfoMessage(project, RRemoteBundle.message("remote.host.view.cannot.open.file", name), this.title)
          }
        }
      }
    })
  }
}