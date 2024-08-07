package org.jetbrains.r.rmarkdown

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.quarto.QuartoFileType

object RMarkdownVirtualFile {
  fun isRMarkdownOrQuarto(virtualFile: VirtualFile): Boolean {
    val fileTypeRegistryInstance = FileTypeRegistry.getInstance()
    return fileTypeRegistryInstance.isFileOfType(virtualFile, RMarkdownFileType) ||
           fileTypeRegistryInstance.isFileOfType(virtualFile, QuartoFileType)
  }

  fun hasVirtualFile(editor: Editor): Boolean {
    val virtualFile = getAnyVirtualFile(editor) ?: return false
    return isRMarkdownOrQuarto(virtualFile)
  }

  private fun getAnyVirtualFile(editor: Editor): VirtualFile? {
    return FileDocumentManager.getInstance().getFile(editor.document)
  }
}