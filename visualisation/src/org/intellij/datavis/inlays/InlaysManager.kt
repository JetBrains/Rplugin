/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import icons.org.intellij.datavis.inlays.EditorInlaysManager

/**
 * Manages inlays.
 *
 * On project load subscribes
 *    on editor opening/closing.
 *    on adding/removing notebook cells
 *    on any document changes
 *    on folding actions
 *
 * On editor open checks the PSI structure and restores saved inlays.
 *
 * ToDo should be split into InlaysManager with all basics and NotebookInlaysManager with all specific.
 */

val RMARKDOWN_CHUNK = TextAttributesKey.createTextAttributesKey("RMARKDOWN_CHUNK")

private val logger = Logger.getInstance(InlaysManager::class.java)

class InlaysManager(val project: Project) : ProjectComponent {

  companion object {
    private val KEY = Key.create<EditorInlaysManager>("org.intellij.datavis.inlays.editorInlaysManager")

    @JvmStatic
    fun getInstance(project: Project): InlaysManager = project.getComponent(InlaysManager::class.java)

    fun getEditorManager(editor: Editor): EditorInlaysManager? = editor.getUserData(KEY)

    private fun getDescriptor(editor: Editor): InlayElementDescriptor? {
      return InlayDescriptorProvider.EP.extensionList
        .asSequence()
        .mapNotNull { it.getInlayDescriptor(editor) }
        .firstOrNull()
    }
  }

  override fun projectOpened() {
    EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
      override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val descriptor = getDescriptor(editor) ?: return
        InlayDimensions.init(editor as EditorImpl)
        editor.putUserData(KEY, EditorInlaysManager(project, editor, descriptor))
      }

      override fun editorReleased(event: EditorFactoryEvent) {
        if (event.editor.project != project) return
        event.editor.getUserData(KEY)?.let { manager ->
          manager.dispose()
          event.editor.putUserData(KEY, null)
        }
      }
    }, project)
  }
}