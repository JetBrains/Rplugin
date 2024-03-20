package org.jetbrains.r.documentation

import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorCustomizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.r.rinterop.RSourceFileManager

private class RTextEditorCustomizer : TextEditorCustomizer {
  override suspend fun execute(textEditor: TextEditor) {
    val file = textEditor.editor.virtualFile ?: serviceAsync<FileDocumentManager>().getFile(textEditor.editor.document)
    if (file != null && RSourceFileManager.isTemporary(file)) {
      withContext(Dispatchers.EDT) {
        DocRenderManager.setDocRenderingEnabled(textEditor.editor, true)
      }
    }
  }
}