/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.SplitAction
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.ex.FakeFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.r.RBundle
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.Icon
import javax.swing.JComponent

private const val EDITOR_TYPE_ID = "r-tables"

private object TableFileType : FakeFileType() {
  override fun getName() = "R Table"

  override fun getDescription() = RBundle.message("table.file.label")

  override fun isMyFileType(file: VirtualFile) = file is RTableVirtualFile

  override fun getIcon(): Icon = AllIcons.Nodes.DataTables
}

class RTableVirtualFile(val table: RDataFrameTablePage, name: String) : LightVirtualFile(name) {
  init {
    putUserData(SplitAction.FORBID_TAB_SPLIT, true)
  }

  override fun getFileType(): FileType = TableFileType
}

class RTableEditorProvider : FileEditorProvider, DumbAware {
  override fun getEditorTypeId(): String = EDITOR_TYPE_ID

  override fun accept(project: Project, file: VirtualFile): Boolean = file is RTableVirtualFile

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val rTableVirtualFile = file as RTableVirtualFile
    val tableFileEditor = TableFileEditor(rTableVirtualFile)
    rTableVirtualFile.table.viewer.registerDisposable(tableFileEditor, rTableVirtualFile)
    return tableFileEditor
  }

  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

private class TableFileEditor(private val tableFile: RTableVirtualFile) : UserDataHolderBase(), FileEditor {
  override fun getComponent(): JComponent = tableFile.table
  override fun getName(): String = tableFile.name
  override fun getFile(): VirtualFile = tableFile
  override fun getPreferredFocusedComponent(): JComponent? = null

  private val propertyChangeSupport = PropertyChangeSupport(this)

  override fun dispose() {}
  override fun isValid(): Boolean = true

  override fun selectNotify() {}
  override fun deselectNotify() {}

  override fun addPropertyChangeListener(listener: PropertyChangeListener) {
    propertyChangeSupport.addPropertyChangeListener(listener)
  }

  override fun removePropertyChangeListener(listener: PropertyChangeListener) {
    propertyChangeSupport.removePropertyChangeListener(listener)
  }

  //
  // Unused
  //

  override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE
  override fun setState(state: FileEditorState) {}
  override fun isModified(): Boolean = false

  override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null
}
