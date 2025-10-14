package org.jetbrains.r.rmarkdown

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.rmarkdown.RMarkdownFileType
import org.jetbrains.r.quarto.QuartoFileType

object RMarkdownVirtualFile {
  fun isRMarkdownOrQuarto(virtualFile: VirtualFile): Boolean =
    isRMarkdownOrQuarto(FileTypeRegistry.getInstance(), virtualFile)

  fun isRMarkdownOrQuarto(fileTypeRegistryInstance: FileTypeRegistry, virtualFile: VirtualFile): Boolean {
    return fileTypeRegistryInstance.isFileOfType(virtualFile, RMarkdownFileType) ||
           fileTypeRegistryInstance.isFileOfType(virtualFile, QuartoFileType)
  }

  fun isRMarkdownOrQuarto(document: Document): Boolean {
    val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return false
    return isRMarkdownOrQuarto(virtualFile)
  }

  fun hasVirtualFile(editor: Editor): Boolean {
    val virtualFile = getAnyVirtualFile(editor) ?: return false
    return isRMarkdownOrQuarto(virtualFile)
  }

  private fun getAnyVirtualFile(editor: Editor): VirtualFile? {
    return FileDocumentManager.getInstance().getFile(editor.document)
  }
}