package org.jetbrains.r.documentation

import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorCustomizer
import com.intellij.r.psi.rinterop.RSourceFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class RTextEditorCustomizer : TextEditorCustomizer {
  override fun customize(textEditor: TextEditor, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
      val file = textEditor.editor.virtualFile ?: serviceAsync<FileDocumentManager>().getFile(textEditor.editor.document)
      if (file != null && RSourceFileManager.isTemporary(file)) {
        withContext(Dispatchers.EDT) {
          DocRenderManager.setDocRenderingEnabled(textEditor.editor, true)
        }
      }
    }
  }
}