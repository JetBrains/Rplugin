/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.r.RLanguage

class RLightVirtualFileManager(private val project: Project) {
  private val cache = ContainerUtil.createConcurrentSoftValueMap<String, LightVirtualFile>()

  fun openLightFileWithContent(fqn: String, name: String, methodContent: CharSequence) {
    val virtualFile = getOrCreateLightFile(fqn, name, methodContent)
    FileEditorManager.getInstance(project).openFile(virtualFile, true, true)
  }

  fun getOrCreateLightFile(fqn: String, name: String, methodContent: CharSequence): LightVirtualFile {
    val virtualFile = cache.computeIfAbsent(fqn) { LightVirtualFile(name, RLanguage.INSTANCE, "") }
    virtualFile.isWritable = true
    virtualFile.setContent(this, methodContent, false)
    virtualFile.isWritable = false
    return virtualFile
  }

  companion object {
    fun getInstance(project: Project) : RLightVirtualFileManager = project.service<RLightVirtualFileManager>()
  }
}