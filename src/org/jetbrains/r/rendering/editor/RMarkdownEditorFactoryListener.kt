package org.jetbrains.r.rendering.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.plugins.notebooks.visualization.NotebookCellInlayManager
import org.jetbrains.plugins.notebooks.visualization.NotebookCellLinesProvider
import org.jetbrains.plugins.notebooks.visualization.NotebookEditorAppearanceProvider
import org.jetbrains.r.quarto.QuartoFileType
import org.jetbrains.r.rmarkdown.RMarkdownFileType

class RMarkdownEditorFactoryListener : EditorFactoryListener {

  override fun editorCreated(event: EditorFactoryEvent) {
    val virtualFile = getVirtualFile(event.editor)
    if (virtualFile != null && isRMarkdownOrQuarto(virtualFile)) {
      onRMarkdownFileEditorCreated(event.editor as EditorImpl)
    }
  }

  companion object {
    fun onRMarkdownFileEditorCreated(editor: EditorImpl) {
      NotebookCellLinesProvider.install(editor)
      NotebookEditorAppearanceProvider.install(editor)
      NotebookCellInlayManager.install(editor, shouldCheckInlayOffsets = false)
    }

    fun isRMarkdownOrQuarto(virtualFile: VirtualFile): Boolean {
      val fileTypeRegistryInstance = FileTypeRegistry.getInstance()
      return fileTypeRegistryInstance.isFileOfType(virtualFile, RMarkdownFileType) ||
             fileTypeRegistryInstance.isFileOfType(virtualFile, QuartoFileType)
    }

    fun getVirtualFile(editor: Editor): VirtualFile? {
      val project = editor.project ?: return null
      return PsiDocumentManager.getInstance(project).getPsiFile(editor.document)?.virtualFile
    }
  }
}