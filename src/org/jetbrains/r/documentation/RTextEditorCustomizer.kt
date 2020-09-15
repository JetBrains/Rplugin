package org.jetbrains.r.documentation

import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorCustomizer
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.r.rinterop.RSourceFileManager

class RTextEditorCustomizer : TextEditorCustomizer {
  override fun customize(textEditor: TextEditor) {
    val manager = FileDocumentManager.getInstance()
    val file = manager.getFile(textEditor.editor.document)
    if (file is LightVirtualFile && RSourceFileManager.isTemporary(file)) {
      DocRenderManager.setDocRenderingEnabled(textEditor.editor, true)
    }
  }
}