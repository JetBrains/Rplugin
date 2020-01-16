/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.diff.util.FileEditorBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.ex.FakeFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import icons.org.jetbrains.r.run.visualize.RDataFrameTablePage
import javax.swing.Icon
import javax.swing.JComponent

private const val EDITOR_TYPE_ID = "r-tables"

private object TableFileType : FakeFileType() {
  override fun getName() = "R Table"

  override fun getDescription() = "R table"

  override fun isMyFileType(file: VirtualFile) = file is RTableVirtualFile

  override fun getIcon(): Icon = AllIcons.Nodes.DataTables
}

class RTableVirtualFile(val table: RDataFrameTablePage, name: String) : LightVirtualFile(name) {
  override fun getFileType(): FileType = TableFileType
}

class RTableEditorProvider : FileEditorProvider, DumbAware {
  override fun getEditorTypeId(): String = EDITOR_TYPE_ID

  override fun accept(project: Project, file: VirtualFile): Boolean = file is RTableVirtualFile

  override fun createEditor(project: Project, file: VirtualFile): FileEditor = TableFileEditor(file as RTableVirtualFile)

  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

private class TableFileEditor(private val tableFile: RTableVirtualFile) : FileEditorBase() {
  override fun getComponent(): JComponent = tableFile.table

  override fun getName(): String = tableFile.name

  override fun getFile(): VirtualFile? = tableFile

  override fun getPreferredFocusedComponent(): JComponent? = null
}