/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import org.jetbrains.r.RLanguage

object REditorUtil {
  fun createReadOnlyLightRFileAndOpen(project: Project, name: String, methodContent: CharSequence): ReadOnlyLightVirtualFile {
    val destination = ReadOnlyLightVirtualFile(name, RLanguage.INSTANCE, methodContent)
    FileEditorManager.getInstance(project).openFile(destination, true, true)
    return destination
  }
}