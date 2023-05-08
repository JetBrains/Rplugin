package org.jetbrains.r.documentation

import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorCustomizer
import org.jetbrains.r.rinterop.RSourceFileManager

private class RTextEditorCustomizer : TextEditorCustomizer {
  override fun customize(textEditor: TextEditor) {
    val file = textEditor.editor.virtualFile ?: FileDocumentManager.getInstance().getFile(textEditor.editor.document)
    if (file != null && RSourceFileManager.isTemporary(file)) {
      DocRenderManager.setDocRenderingEnabled(textEditor.editor, true)
    }
  }
}