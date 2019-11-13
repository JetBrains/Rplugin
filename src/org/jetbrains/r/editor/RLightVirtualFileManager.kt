/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.r.RLanguage

class RLightVirtualFileManager(private val project: Project) {
  private val cache = ContainerUtil.createConcurrentSoftValueMap<String, LightVirtualFile>()

  fun openLightFileWithContent(fqn: String, name: String, methodContent: CharSequence): VirtualFile {
    val virtualFile = getOrCreateLightFile(fqn, name)
    val openFile = FileEditorManager.getInstance(project).openFile(virtualFile, true, true)
    invokeAndWaitIfNeeded { runWriteAction {
      try {
        virtualFile.isWritable = true
        val first = openFile.first()
        if (first is TextEditor) {
          val document = first.editor.document
          document.setText(methodContent)
          PsiDocumentManager.getInstance(project).commitDocument(document)
        }
      } finally {
        virtualFile.isWritable = false
      }
    } }
    return virtualFile
  }

  private fun getOrCreateLightFile(fqn: String, name: String): LightVirtualFile =
    cache.computeIfAbsent(fqn) { LightVirtualFile(name, RLanguage.INSTANCE, "") }

  companion object {
    fun getInstance(project: Project) : RLightVirtualFileManager = project.service<RLightVirtualFileManager>()
  }
}