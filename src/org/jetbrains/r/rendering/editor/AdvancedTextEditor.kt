/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.editor

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JPanel

abstract class AdvancedTextEditor(val project: Project,
                                  val textEditor: TextEditor,
                                  private val virtualFile: VirtualFile) : UserDataHolderBase(), TextEditor {
  protected val mainComponent = JPanel(BorderLayout())

  init {
    mainComponent.add(textEditor.component, BorderLayout.CENTER)
  }

  override fun dispose() {
    TextEditorProvider.getInstance().disposeEditor(textEditor)
  }

  override fun getComponent() = mainComponent

  override fun getState(level: FileEditorStateLevel): FileEditorState = textEditor.getState(level)
  override fun setState(state: FileEditorState) = textEditor.setState(state)
  override fun isModified(): Boolean = textEditor.isModified
  override fun isValid(): Boolean = textEditor.isValid
  override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = textEditor.backgroundHighlighter
  override fun getCurrentLocation(): FileEditorLocation? = textEditor.currentLocation
  override fun getPreferredFocusedComponent() = textEditor.preferredFocusedComponent
  override fun getStructureViewBuilder(): StructureViewBuilder? = textEditor.structureViewBuilder
  override fun getEditor(): Editor = textEditor.editor
  override fun navigateTo(navigatable: Navigatable) = textEditor.navigateTo(navigatable)
  override fun canNavigateTo(navigatable: Navigatable): Boolean = textEditor.canNavigateTo(navigatable)
  override fun getFile(): VirtualFile = virtualFile

  override fun addPropertyChangeListener(listener: PropertyChangeListener) {
    textEditor.addPropertyChangeListener(listener)
  }

  override fun removePropertyChangeListener(listener: PropertyChangeListener) {
    textEditor.removePropertyChangeListener(listener)
  }
}