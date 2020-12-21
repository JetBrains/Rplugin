package org.jetbrains.r.rendering.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.plugins.notebooks.editor.NotebookCellInlayManager
import org.jetbrains.plugins.notebooks.editor.NotebookEditorAppearanceProvider
import org.jetbrains.plugins.notebooks.editor.NotebookGutterRenderer
import org.jetbrains.r.rmarkdown.RMarkdownFileType

class RMarkdownEditorFactoryListener : EditorFactoryListener {

  override fun editorCreated(event: EditorFactoryEvent) {
    if (isRMarkdown(event.editor)) {
      onRMarkdownFileEditorCreated(event.editor as EditorImpl)
    }
  }

  companion object {
    fun onRMarkdownFileEditorCreated(editor: EditorImpl) {
      NotebookEditorAppearanceProvider.install(editor)
      NotebookGutterRenderer.install(editor)
      NotebookCellInlayManager.install(editor)
    }

    fun isRMarkdown(editor: Editor): Boolean {
      val project = editor.project ?: return false
      val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return false
      return psiFile.virtualFile?.fileType is RMarkdownFileType
    }
  }
}