/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import org.jetbrains.r.skeleton.psi.RSkeletonFileImpl

class RSkeletonViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(file: VirtualFile,
                                      language: Language?,
                                      manager: PsiManager,
                                      eventSystemEnabled: Boolean): FileViewProvider {
    return RSkeletonFileViewProvider(manager, file, eventSystemEnabled)
  }
}


private class RSkeletonFileViewProvider(manager: PsiManager,
                                        file: VirtualFile,
                                        eventSystemEnabled: Boolean
) : SingleRootFileViewProvider(manager, file, eventSystemEnabled) {

  override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile {
    return RSkeletonFileImpl(this)
  }
}