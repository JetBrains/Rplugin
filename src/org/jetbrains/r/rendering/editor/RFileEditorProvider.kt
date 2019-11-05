/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.RFileType

class RFileEditorProvider : FileEditorProvider, DumbAware {

  override fun getEditorTypeId() = "r-editor"

  override fun accept(project: Project, file: VirtualFile) = file.fileType == RFileType

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val editor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditorImpl
    return RFileEditor(project, editor, file)
  }

  override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}